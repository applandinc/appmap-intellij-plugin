package appland.installGuide;

import appland.AppMapBundle;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class InstallGuideEditorProvider implements FileEditorProvider, DumbAware {
    // defines the install-guide view type, i.e. the install-guide page to show in a particular editor
    private static final Key<InstallGuideViewPage> INSTALL_GUIDE_PAGE_KEY = Key.create("appland.installGuideFile");
    private static final String EDITOR_ID = "appland.installGuide";

    public static boolean isSupported() {
        return JBCefApp.isSupported();
    }

    public static void open(@NotNull Project project, @NotNull InstallGuideViewPage page) {
        var editorManager = FileEditorManager.getInstance(project);
        // try to re-use an already open editor for "Install Guide"
        for (var editor : editorManager.getAllEditors()) {
            var file = editor.getFile();
            if (file != null && isInstallGuideFile(file)) {
                assert editor instanceof InstallGuideEditor;

                FileEditorManagerEx.getInstanceEx(project).openFile(file, true, true);
                ((InstallGuideEditor) editor).navigateTo(page);
                return;
            }
        }

        var file = new LightVirtualFile(AppMapBundle.get("installGuide.editor.title"));
        INSTALL_GUIDE_PAGE_KEY.set(file, page);
        editorManager.openFile(file, true);
    }

    @NotNull
    public static Boolean isInstallGuideFile(@NotNull VirtualFile file) {
        return INSTALL_GUIDE_PAGE_KEY.isIn(file);
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return JBCefApp.isSupported() && isInstallGuideFile(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new InstallGuideEditor(project, file, INSTALL_GUIDE_PAGE_KEY.getRequired(file));
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return EDITOR_ID;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
