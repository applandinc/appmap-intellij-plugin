package appland;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class Icons {
    // 16x16
    public static final Icon APPMAP_FILE = IconLoader.getIcon("/icons/appmap.svg", Icons.class);
    public static final Icon APPMAP_DOCS = APPMAP_FILE;
    public static final Icon START_RECORDING_ACTION = IconLoader.getIcon("/icons/record_start.svg", Icons.class);
    public static final Icon STOP_RECORDING_ACTION = IconLoader.getIcon("/icons/record_stop.svg", Icons.class);
    public static final Icon APPMAP_TOOLS_MENU = APPMAP_FILE;

    public static final Icon MILESTONE_COMPLETED = IconLoader.getIcon("/icons/milestone_completed.svg", Icons.class);
    public static final Icon MILESTONE_ERROR = IconLoader.getIcon("/icons/milestone_error.svg", Icons.class);
    public static final Icon MILESTONE_INCOMPLETE = IconLoader.getIcon("/icons/milestone_incomplete.svg", Icons.class);

    // 13x13
    public static final Icon APPMAP_FILE_SMALL = IconLoader.getIcon("/icons/appmap_small.svg", Icons.class);
    public static final Icon TOOL_WINDOW = APPMAP_FILE_SMALL;
}
