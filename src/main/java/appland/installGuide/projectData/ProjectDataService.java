package appland.installGuide.projectData;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ProjectDataService {
    static @NotNull ProjectDataService getInstance(@NotNull Project project) {
        return project.getService(ProjectDataService.class);
    }

    @NotNull List<ProjectMetadata> getAppMapProjects();
}
