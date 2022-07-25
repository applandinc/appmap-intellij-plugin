package appland.installGuide;

import appland.AppMapBundle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public enum InstallGuideViewPage {
    InstallAgent("project-picker"),
    RecordAppMaps("record-appmaps"),
    OpenAppMaps("open-appmaps");

    // value used by the JS application as "page" values
    @Getter
    private final String pageId;

    public static @Nullable InstallGuideViewPage findByPageId(@NotNull String pageId) {
        for (var type : InstallGuideViewPage.values()) {
            if (type.pageId.equals(pageId)) {
                return type;
            }
        }
        return null;
    }

    public String getPageTitle() {
        switch (this) {
            case InstallAgent:
                return AppMapBundle.get("installGuide.pageInstallAgent.title");
            case RecordAppMaps:
                return AppMapBundle.get("installGuide.pageRecordAppMaps.title");
            case OpenAppMaps:
                return AppMapBundle.get("installGuide.pageOpenAppMaps.title");
            default:
                throw new IllegalStateException("Unsupported type: " + this);
        }
    }
}
