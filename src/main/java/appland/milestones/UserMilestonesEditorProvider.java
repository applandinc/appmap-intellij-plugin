package appland.milestones;

import appland.AppMapPlugin;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class UserMilestonesEditorProvider implements FileEditorProvider, DumbAware {
    private static final Key<Boolean> MILESTONES_KEY = KeyWithDefaultValue.create("appland.milestonesFile", false);

    public static void openUserMilestones(@NotNull Project project) {
        var dummyFile = new LightVirtualFile("User Milestones");
        MILESTONES_KEY.set(dummyFile, true);

        FileEditorManager.getInstance(project).openFile(dummyFile, true);
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return JBCefApp.isSupported() && MILESTONES_KEY.getRequired(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new UserMilestonesEditor(project, file);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "appland.milestones";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}