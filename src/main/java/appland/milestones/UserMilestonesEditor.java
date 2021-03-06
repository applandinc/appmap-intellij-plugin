package appland.milestones;

import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataIndex;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ClipboardSynchronizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
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
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static appland.AppMapPlugin.getProjectDirPath;

public class UserMilestonesEditor extends UserDataHolderBase implements FileEditor {
    private static final Logger LOG = Logger.getInstance("#appmap.milestones");
    private static final String READY_MESSAGE_ID = "intellij-plugin-ready";

    private final Project project;
    @NotNull
    private final VirtualFile file;
    private final MilestonesViewType type;

    private final JBCefClient jcefClient = JBCefApp.getInstance().createClient();
    private final JCEFHtmlPanel contentPanel = new JCEFHtmlPanel(jcefClient, null);
    private final JBCefJSQuery jcefBridge = JBCefJSQuery.create((JBCefBrowserBase) contentPanel);

    private final AtomicBoolean navigating = new AtomicBoolean(false);
    private final Gson gson = new GsonBuilder().create();

    public UserMilestonesEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull MilestonesViewType type) {
        this.project = project;
        this.file = file;
        this.type = type;

        Disposer.register(this, jcefClient);
        Disposer.register(this, contentPanel);
        Disposer.register(this, jcefBridge);

        setupJCEF();
        loadApplication();
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
            contentPanel.loadURL(type.getHTMLPath().toUri().toURL().toString());
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private void onJavaScriptApplicationReady() {
        jcefBridge.addHandler(request -> {
            LOG.warn("postMessage received message: " + request);

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
                        case "openFile":
                            ApplicationManager.getApplication().invokeLater(() -> {
                                var file = LocalFileSystem.getInstance().findFileByNioFile(Paths.get(json.getAsJsonPrimitive("file").getAsString()));
                                if (file != null) {
                                    FileEditorManager.getInstance(project).openFile(file, true);
                                }
                            });
                            break;
                        case "transition": {
                            var target = json.getAsJsonPrimitive("target").getAsString();
                            var targetViewType = MilestonesViewType.findByTransitionTarget(target);
                            if (targetViewType != null) {
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    UserMilestonesEditorProvider.open(project, targetViewType);
                                });
                            }
                            break;
                        }
                        case "clipboard": {
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

        JsonObject json;
        switch (type) {
            case InstallAgent:
                json = createInstallAgentInitJSON();
                break;
            case RecordAppMaps:
                json = createRecordAppMapsInitJSON();
                break;
            case AppMapsTable:
                json = createAppMapsInitJSON();
                break;
            default:
                throw new IllegalStateException("Unsupported view type: " + type);
        }

        var jsonString = gson.toJson(json);
        contentPanel.getCefBrowser().executeJavaScript("window.postMessage(" + jsonString + ")", "", 0);
    }

    @NotNull
    private JsonObject createInstallAgentInitJSON() {
        var json = new JsonObject();
        String projectDirPath = getProjectDirPath(project);
        json.addProperty("type", "init");
        json.addProperty("codeSnippet", "npx @appland/appmap install " + projectDirPath);
        return json;
    }

    @NotNull
    private JsonObject createRecordAppMapsInitJSON() {
        var json = new JsonObject();
        json.addProperty("type", "init");
        json.addProperty("editor", "jetbrains");
        return json;
    }

    @NotNull
    private JsonObject createAppMapsInitJSON() {
        var json = new JsonObject();
        json.addProperty("type", "init");
        json.add("appmaps", createAppMapsArray(project));
        return json;
    }

    private JsonArray createAppMapsArray(@NotNull Project project) {
        var appmaps = ReadAction.compute(() -> AppMapMetadataIndex.findAppMaps(project, null, 10))
                .stream()
                .filter(AppMapMetadata::hasAnyCount)
                .sorted(Comparator.comparingInt(AppMapMetadata::getSortCount).reversed())
                .collect(Collectors.toList());

        var json = new JsonArray(appmaps.size());
        for (var appmap : appmaps) {
            var jsonAppMap = new JsonObject();
            jsonAppMap.addProperty("path", appmap.getSystemIndependentFilepath());
            jsonAppMap.addProperty("name", appmap.getName());
            jsonAppMap.addProperty("requests", appmap.getRequestCount());
            jsonAppMap.addProperty("sqlQueries", appmap.getQueryCount());
            jsonAppMap.addProperty("functions", appmap.getFunctionsCount());
            json.add(jsonAppMap);
        }
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
        return "User Milestones";
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
        LOG.debug("Disposing user milestones editor");
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }
}
