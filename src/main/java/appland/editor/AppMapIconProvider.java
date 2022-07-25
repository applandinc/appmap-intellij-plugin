package appland.editor;

import appland.Icons;
import appland.files.AppMapFiles;
import appland.installGuide.InstallGuideEditorProvider;
import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AppMapIconProvider implements FileIconProvider {
    @Override
    @Nullable
    public Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
        boolean isAppLandFile = AppMapFiles.isAppMap(file)
                | InstallGuideEditorProvider.isInstallGuideFile(file)
                | InstallGuideEditorProvider.isInstallGuideFile(file);

        return isAppLandFile ? Icons.APPMAP_FILE : null;
    }
}
