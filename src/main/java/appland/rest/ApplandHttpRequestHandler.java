package appland.rest;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Provides a custom REST handler at http://localhost:63343/api/appland
 * If 63343 is already taken, then try the next few ports, e.g. 63344.
 */
public class ApplandHttpRequestHandler extends RestService {
    @NotNull
    @Override
    protected String getServiceName() {
        return "appland";
    }

    @Nullable
    @Override
    public String execute(@NotNull QueryStringDecoder urlDecoder,
                          @NotNull FullHttpRequest request,
                          @NotNull ChannelHandlerContext channelHandlerContext) throws IOException {

        var uriParam = urlDecoder.parameters().get("uri");
        if (uriParam == null || uriParam.size() != 1) {
            sendStatus(HttpResponseStatus.BAD_REQUEST, false, channelHandlerContext.channel());
            return Companion.parameterMissedErrorMessage("uri");
        }

        var uri = uriParam.get(0);
        if (StringUtil.isEmptyOrSpaces(uri)) {
            sendStatus(HttpResponseStatus.BAD_REQUEST, false, channelHandlerContext.channel());
            return Companion.parameterMissedErrorMessage("uri");
        }

/*        var stateParam = urlDecoder.parameters().get("state");
        if (stateParam == null || stateParam.size() != 1) {
            sendStatus(HttpResponseStatus.BAD_REQUEST, false, channelHandlerContext.channel());
            return Companion.parameterMissedErrorMessage("state");
        }*/

        var systemIndependentPath = FileUtil.toSystemIndependentName(FileUtil.expandUserHome(uri));
        var file = Paths.get(FileUtil.toSystemDependentName(systemIndependentPath));
        var vFile = LocalFileSystem.getInstance().findFileByNioFile(file);
        if (vFile == null) {
            sendStatus(HttpResponseStatus.NOT_FOUND, false, channelHandlerContext.channel());
            return null;
        }

        var project = getLastFocusedOrOpenedProject();
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }

        var openFileRequest = new OpenFileDescriptor(project, vFile);
        ApplicationManager.getApplication().invokeLater(() -> {
            openFileRequest.navigate(true);
        });

        sendOk(request, channelHandlerContext);
        return null;
    }
}
