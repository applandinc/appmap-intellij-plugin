package appland.files;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * This class locates files by relative paths.
 * <p>
 * AppMaps contain relative paths. Although these paths are usually based on the "project root path" the root path isn't
 * clearly defined.
 * <p>
 * This class tries to locate the best matching file in the project.
 */
public class FileLookup {
    /**
     * @param project      Current project
     * @param base         The base file or directory. This usually is the currently opened appmap file.
     * @param relativePath A relative path, it has to use / as delimiter.
     * @return The target file, if it was found
     */
    @Nullable
    public static VirtualFile findRelativeFile(@NotNull Project project, @NotNull VirtualFile base, @NotNull String relativePath) {
        // support the rare case of absolute paths
        if (relativePath.startsWith("/")) {
            return LocalFileSystem.getInstance().findFileByPath(relativePath);
        }

        var baseDir = base.isDirectory() ? base : base.getParent();

        // 1st, try to locate relative to the base path
        var file = baseDir.findFileByRelativePath(relativePath);
        if (file != null) {
            return file;
        }

        if (DumbService.isDumb(project)) {
            // no further lookup possible without an index
            return null;
        }

        for (var candidate : FilenameIndex.getVirtualFilesByName(project, filename(relativePath), true, ProjectScope.getAllScope(project))) {
            var parent = candidate.getParent();
            for (String expectedParentName : parentsReversed(relativePath)) {
                if (parent == null || !FileUtil.namesEqual(expectedParentName, parent.getName())) {
                    parent = null;
                    break;
                }
                parent = parent.getParent();
            }

            if (parent != null) {
                // the candidate is matching all parent directories in the hierarchy
                return candidate;
            }
        }

        // no match :(
        return null;
    }

    static String filename(@NotNull String path) {
        int index = path.lastIndexOf('/');
        if (index == -1) {
            return path;
        }
        return path.substring(index + 1);
    }

    static List<String> parentsReversed(@NotNull String path) {
        int index = path.lastIndexOf('/');
        if (index == -1) {
            return Collections.emptyList();
        }

        return Lists.reverse(StringUtil.split(path.substring(0, index), "/"));
    }
}
