package appland.installGuide;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.installGuide.analyzer.GsonUtils;
import appland.installGuide.projectData.ProjectDataService;
import com.google.gson.*;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ClipboardSynchronizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.*;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public class InstallGuideEditor extends UserDataHolderBase implements FileEditor {
    private static final Logger LOG = Logger.getInstance("#appmap.installGuide");
    private static final String READY_MESSAGE_ID = "intellij-plugin-ready";

    private final Project project;
    private final @NotNull VirtualFile file;
    private @NotNull InstallGuideViewPage type;

    private final JBCefClient jcefClient = JBCefApp.getInstance().createClient();
    private final JCEFHtmlPanel contentPanel = new JCEFHtmlPanel(jcefClient, null);
    private final JBCefJSQuery jcefBridge = JBCefJSQuery.create((JBCefBrowserBase) contentPanel);

    private final AtomicBoolean navigating = new AtomicBoolean(false);
    private final Gson gson = new GsonBuilder().create();

    public InstallGuideEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull InstallGuideViewPage type) {
        this.project = project;
        this.file = file;
        this.type = type;

        Disposer.register(this, jcefClient);
        Disposer.register(this, contentPanel);
        Disposer.register(this, jcefBridge);

        setupJCEF();
        loadApplication();
    }

    public void navigateTo(@NotNull InstallGuideViewPage page) {
        this.type = page;
        postMessage(createPageNavigationJSON(page));
    }

    private void setupJCEF() {
        // open links to https://appland.com in the external browser
        contentPanel.getJBCefClient().addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                var url = request.getURL();
                if (url != null && url.startsWith("https://appland.com")) {
                    navigating.set(true);
                    BrowserUtil.browse(url);
                    return true;
                }
                return false;
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
                    onJavaScriptApplicationReady();
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

    private void loadApplication() {
        try {
            contentPanel.loadURL(AppMapPlugin.getInstallGuideHTMLPath().toUri().toURL().toString());
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private void onJavaScriptApplicationReady() {
        jcefBridge.addHandler(request -> {
            LOG.debug("postMessage received message: " + request);

            try {
                var json = gson.fromJson(request, JsonObject.class);
                var type = json.has("type") ? json.getAsJsonPrimitive("type").getAsString() : null;
                if (type != null) {
                    switch (type) {
                        case "postInitialize":
                            // ignored
                            break;

                        case "clickLink":
                            // ignored, handled by JCEF listener
                            break;

                        case "open-file":
                            ApplicationManager.getApplication().invokeLater(() -> {
                                var path = Paths.get(json.getAsJsonPrimitive("file").getAsString());
                                var file = LocalFileSystem.getInstance().findFileByNioFile(path);
                                if (file != null) {
                                    FileEditorManager.getInstance(project).openFile(file, true);
                                }
                            });
                            break;

                        case "open-page":
                            // fixme send telemetry, as in VSCode?
                            break;

                        case "view-problems":
                            //fixme
                            break;

                        case "clipboard": {
                            // fixme send telemetry, as in VSCode?
                            var content = json.getAsJsonPrimitive("target").getAsString();
                            LOG.debug("Copying text to clipboard: " + content);

                            var target = new StringSelection(content);
                            ClipboardSynchronizer.getInstance().setContent(target, target);
                            break;
                        }

                        default:
                            LOG.warn("Unhandled message type: " + type);
                    }
                }
            } catch (Exception e) {
                LOG.warn("error handling command: " + request, e);
            }

            return new JBCefJSQuery.Response("Received " + request);
        });

        contentPanel.getCefBrowser().executeJavaScript(createCallbackJS(jcefBridge, "postMessage"), "", 0);

        postMessage(createInitMessageJSON());
    }

    private void postMessage(@NotNull JsonElement json) {
        contentPanel.getCefBrowser().executeJavaScript("window.postMessage(" + gson.toJson(json) + ")", "", 0);
    }

    private @NotNull JsonObject createInitMessageJSON() {
        var projects = ProjectDataService.getInstance(project).getAppMapProjects();

        var json = new JsonObject();
        json.addProperty("type", "init");
        json.add("projects", GsonUtils.GSON.toJsonTree(projects));
        json.add("disabled", new JsonArray()); // fixme
        json.addProperty("page", type.getPageId());
        return json;
    }

    private @NotNull JsonObject createPageNavigationJSON(@NotNull InstallGuideViewPage page) {
        var json = new JsonObject();
        json.addProperty("type", "page");
        json.addProperty("page", page.getPageId());
        return json;
    }

    @NotNull
    private String createCallbackJS(JBCefJSQuery query, @NotNull String functionName) {
        return "if (!window.AppLand) window.AppLand={}; window.AppLand." + functionName + "=function(name) {" +
                query.inject("name") + "};";
    }

    @Override
    public @NotNull JComponent getComponent() {
        return contentPanel.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("installGuide.editor.title");
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
        return true;
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
        LOG.debug("Disposing AppLand install guide editor");
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }
}
