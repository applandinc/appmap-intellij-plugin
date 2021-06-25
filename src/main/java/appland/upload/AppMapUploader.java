package appland.upload;

import appland.AppMapBundle;
import appland.Icons;
import appland.settings.AppMapProjectSettingsService;
import com.google.gson.GsonBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.HttpRequests;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Manages the uploading of AppMap files to the AppLand cloud servers.
 */
public class AppMapUploader {
    public static final String DEFAULT_SERVER_URL = "https://app.land";

    private static final Logger LOG = Logger.getInstance("#appmap.upload");

    @NotNull
    private static String uploadURL(@NotNull Project project) {
        var url = StringUtil.defaultIfEmpty(AppMapProjectSettingsService.getState(project).getCloudServerUrl(), DEFAULT_SERVER_URL);
        return StringUtil.trimEnd(url, "/") + "/api/appmaps/create_upload";
    }

    @NotNull
    private static String confirmationURL(@NotNull Project project) {
        var url = StringUtil.defaultIfEmpty(AppMapProjectSettingsService.getState(project).getCloudServerUrl(), DEFAULT_SERVER_URL);
        return StringUtil.trimEnd(url, "/") + "/scenario_uploads";
    }

    /**
     * Upload the file. Asks the user for confirmation if the user did not confirm yet.
     * <p>
     * Must be called on the EDT.
     *
     * @param project The current project
     * @param file    The file to upload
     */
    @SuppressWarnings("DialogTitleCapitalization")
    public static void uploadAppMap(@NotNull Project project, @NotNull VirtualFile file) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        var confirmUpload = AppMapProjectSettingsService.getState(project).getConfirmAppMapUpload();
        if (confirmUpload == null || confirmUpload) {
            var reply = Messages.showYesNoDialog(AppMapBundle.get("upload.confirmation.message"),
                    AppMapBundle.get("upload.confirmation.title"),
                    Icons.APPMAP_FILE);
            if (reply == Messages.NO) {
                return;
            }

            if (confirmUpload == null) {
                AppMapProjectSettingsService.getState(project).setConfirmAppMapUpload(true);
            }
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, AppMapBundle.get("upload.progress.title")) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                var gson = new GsonBuilder().create();

                try {
                    var document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () -> {
                        return FileDocumentManager.getInstance().getDocument(file);
                    });

                    if (document == null) {
                        LOG.warn("unable to load content of VirtualFile, file: " + file.getPath() + ", size: " + file.getLength());
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showErrorDialog(project,
                                    AppMapBundle.get("upload.docUnavailable.message"),
                                    AppMapBundle.get("upload.docUnavaialble.title"));
                        });
                        return;
                    }

                    var content = ApplicationManager
                            .getApplication()
                            .runReadAction((Computable<CharSequence>) document::getImmutableCharSequence);

                    var request = HttpRequests.post(uploadURL(project), "application/json")
                            .gzip(true)
                            .forceHttps(true)
                            .tuner(connection -> connection.setRequestProperty("X-Requested-With", "IntelliJUploader"));
                    request.write(content.toString());

                    var replyContent = request.readString(ProgressManager.getGlobalProgressIndicator());
                    var reply = gson.fromJson(replyContent, UploadResponse.class);

                    var confirmationURL = confirmationURL(project) + "/" + reply.id + "?token=" + reply.token;
                    ApplicationManager.getApplication().invokeLater(() -> BrowserUtil.browse(confirmationURL));
                } catch (IOException e) {
                    LOG.warn("Uploading AppMap failed", e);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project,
                                AppMapBundle.get("upload.uploadFailed.message", file.getName(), StringEscapeUtils.escapeHtml(e.getMessage())),
                                AppMapBundle.get("upload.uploadFailed.title"));
                    });
                }
            }
        });
    }
}