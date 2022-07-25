package appland.toolwindow.installGuide;

import appland.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Status of a single milestone step.
 */
enum InstallGuideStatus {
    Incomplete, Completed, Error;

    @NotNull
    public Icon getIcon() {
        switch (this) {
            case Completed:
                return Icons.MILESTONE_COMPLETED;
            case Error:
                return Icons.MILESTONE_ERROR;
            case Incomplete:
                return Icons.MILESTONE_INCOMPLETE;
            default:
                throw new IllegalStateException("unknown status: " + this);
        }
    }
}
