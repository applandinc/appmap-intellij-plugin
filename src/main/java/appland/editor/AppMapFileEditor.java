package appland.editor;

import appland.AppMapPlugin;
import appland.Messages;
import appland.files.FileLocation;
import appland.files.FileLookup;
import com.intellij.CommonBundle;
import com.intellij.ide.actions.OpenInRightSplitAction;
import com.intellij.ide.plugins.MultiPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.jcef.*;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.openapi.ui.Messages.showErrorDialog;

/**
 * This is similar to JetBrains' com.intellij.openapi.fileEditor.impl.HTMLFileEditor,
 * but adds additional functionality to handle the Appmap JS application.
 * <p>
 * Extending JetBrains' class isn't possible because it's internal to its module.
 */
public class AppMapFileEditor extends UserDataHolderBase implements FileEditor {
    private static final Logger LOG = Logger.getInstance("#appmap.editor");
    private static final int LOADING_KEY = 0;
    private static final int CONTENT_KEY = 1;
    private static final String READY_MESSAGE_ID = "intellij-plugin-ready";

    private final Project project;
    private final String baseURL;
    private final VirtualFile file;
    private final JBCefClient jcefClient = JBCefApp.getInstance().createClient();
    private final JCEFHtmlPanel contentPanel = new JCEFHtmlPanel(jcefClient, null);
    private final JBCefJSQuery jcefBridge = JBCefJSQuery.create((JBCefBrowserBase) contentPanel);

    private final JBLoadingPanel loadingPanel = new JBLoadingPanel(new BorderLayout(), this);
    private final AtomicBoolean initial = new AtomicBoolean(true);
    private final AtomicBoolean navigating = new AtomicBoolean(false);
    private final MultiPanel multiPanel = new MultiPanel() {
        @Override
        protected JComponent create(@NotNull Integer key) {
            switch (key) {
                case LOADING_KEY:
                    return loadingPanel;
                case CONTENT_KEY:
                    return contentPanel.getComponent();
                default:
                    throw new IllegalStateException("unsupported key: " + key);
            }
        }

        @Override
        public ActionCallback select(Integer key, boolean now) {
            var callback = super.select(key, now);
            if (key == CONTENT_KEY) {
                UIUtil.invokeLaterIfNeeded(() -> contentPanel.getComponent().requestFocusInWindow());
            }
            return callback;
        }
    };

    // keeps track if the current editor is focused
    private final AtomicBoolean isSelected = new AtomicBoolean(true);
    // keeps track if the file was modified and not yet loaded into the AppMap application
    private final AtomicBoolean isModified = new AtomicBoolean(false);
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);

    public AppMapFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        this.file = file;
        this.baseURL = getBaseURL();

        Disposer.register(this, jcefClient);
        Disposer.register(this, contentPanel);
        Disposer.register(this, jcefBridge);

        setupVfsListener(file);

        loadingPanel.setLoadingText(CommonBundle.getLoadingTreeNodeText());
        setupJCEF();

        multiPanel.select(CONTENT_KEY, true);
        loadAppmapApplication();
    }

    private void setupVfsListener(@NotNull VirtualFile file) {
        file.getFileSystem().addVirtualFileListener(new VirtualFileListener() {
            /** This method is called on the EDT */
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event) {
                assert ApplicationManager.getApplication().isDispatchThread();
                if (!file.equals(event.getFile())) {
                    return;
                }

                LOG.info("AppMap file was modified, fromSave: " + event.isFromSave() + ", fromRefresh: " + event.isFromRefresh());
                if (isSelected.get()) {
                    // load immediately if focused
                    LOG.info("contentsChanged: refreshing immediately");
                    ApplicationManager.getApplication().invokeLater(AppMapFileEditor.this::loadAppmapData, ModalityState.defaultModalityState());
                } else {
                    // delay if not focused
                    LOG.info("contentsChanged: delaying refresh");
                    isModified.set(true);
                }
            }
        }, this);
    }

    /**
     * This method is called on the EDT
     */
    @Override
    public void selectNotify() {
        LOG.info("selectNotify");
        assert ApplicationManager.getApplication().isDispatchThread();

        isSelected.set(true);
        if (isModified.compareAndSet(true, false)) {
            ApplicationManager.getApplication().invokeLater(AppMapFileEditor.this::loadAppmapData, ModalityState.defaultModalityState());
        }
    }

    /**
     * This method is called on the EDT
     */
    @Override
    public void deselectNotify() {
        LOG.info("deselectNotify");
        isSelected.set(false);
    }

    /**
     * Load the AppLand JavaScript application into the HTML panel.
     * The app will notify this editor when it's ready.
     */
    private void loadAppmapApplication() {
        try {
            var filePath = AppMapPlugin.getAppMapHTMLPath();
            String htmlFileURL = filePath.toUri().toURL().toString();
            contentPanel.loadURL(htmlFileURL);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    /**
     * Load the current file's data into the AppLand JS application.
     */
    private void loadAppmapData() {
        LOG.info("loadAppmapData");

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            LOG.error("unable to retrieve document for file: " + file.getPath());
            return;
        }

        var text = document.getText();
        String javascript = "window.loadAppMap(\"" + StringEscapeUtils.escapeJavaScript(text) + "\")";
        contentPanel.getCefBrowser().executeJavaScript(javascript, baseURL, 0);
    }

    private void setupJCEF() {
        contentPanel.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                if (!initial.get()) {
                    return;
                }

                if (isLoading) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        loadingPanel.startLoading();
                        multiPanel.select(LOADING_KEY, true);
                    }, ModalityState.defaultModalityState());
                } else {
                    initial.set(false);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        loadingPanel.stopLoading();
                        multiPanel.select(CONTENT_KEY, true);
                    }, ModalityState.defaultModalityState());
                }
            }
        }, contentPanel.getCefBrowser());

        contentPanel.setErrorPage((errorCode, errorText, failedUrl) -> {
            if (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED && navigating.getAndSet(false)) {
                return null;
            }
            return JBCefBrowserBase.ErrorPage.DEFAULT.create(errorCode, errorText, failedUrl);
        });

        contentPanel.getJBCefClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                if (READY_MESSAGE_ID.equals(message)) {
                    onJavaScriptAppMapReady();
                    return true;
                }

                var output = String.format("AppMap JS, %s:%d, %s", source, line, message);
                switch (level) {
                    case LOGSEVERITY_FATAL:
                        LOG.error(output);
                        return true;
                    // logging ERROR as warning because the AppMap app always logs errors about svg image dimensions at start
                    case LOGSEVERITY_ERROR:
                    case LOGSEVERITY_WARNING:
                        LOG.warn(output);
                        return true;
                    case LOGSEVERITY_INFO:
                        LOG.info(output);
                        return true;
                    default:
                        LOG.debug(output);
                        return true;
                }
            }
        }, contentPanel.getCefBrowser());
    }

    private void onJavaScriptAppMapReady() {
        setupJCEFBridge();

        var app = ApplicationManager.getApplication();
        app.invokeLater(AppMapFileEditor.this::loadAppmapData, ModalityState.defaultModalityState());
    }

    private void setupJCEFBridge() {
        // viewSource callback handler
        jcefBridge.addHandler(relativePath -> {
            LOG.debug("viewSource callback handler, file: " + relativePath);
            ApplicationManager.getApplication().invokeLater(() -> {
                var location = FileLocation.parse(relativePath);
                if (location != null) {
                    var referencedFile = FileLookup.findRelativeFile(project, file, FileUtil.toSystemIndependentName(location.filePath));
                    if (referencedFile != null) {
                        // IntelliJ's lines are 0-based, AppMap lines seem to be 0-based
                        var line = location.line == null ? -1 : location.line - 1;
                        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, referencedFile, line, -1);

                        OpenInRightSplitAction.Companion.openInRightSplit(project, referencedFile, descriptor, true);
                        return;
                    }
                }

                // fallback message if the could not be found
                showErrorDialog("File " + relativePath + " could not be found.", "AppMap");
            }, ModalityState.defaultModalityState());
            return null;
        });
        contentPanel.getCefBrowser().executeJavaScript(createCallbackJS(jcefBridge, "viewSource"), baseURL, 0);
    }

    @NotNull
    private String createCallbackJS(JBCefJSQuery query, @NotNull String functionName) {
        return "if (!window.AppLand) window.AppLand={}; window.AppLand." + functionName + "=function(link) {" +
                query.inject("link") + "};";
    }

    /**
     * @return The base URL of the HTML panel.
     */
    @NotNull
    private String getBaseURL() {
        String baseURL;
        try {
            baseURL = AppMapPlugin.getAppMapHTMLPath().toUri().toURL().toString();
        } catch (MalformedURLException e) {
            baseURL = "";
        }
        return baseURL;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return multiPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return multiPanel;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return Messages.get("appmap.editor.name");
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return !isDisposed.get();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void dispose() {
        LOG.debug("disposing AppMap file editor");
        this.isDisposed.set(true);
    }

    @Override
    @Nullable
    public VirtualFile getFile() {
        return file;
    }
}
