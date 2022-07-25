package appland.toolwindow;

import appland.AppMapBundle;
import appland.actions.StartAppMapRecordingAction;
import appland.actions.StopAppMapRecordingAction;
import appland.files.AppMapFiles;
import appland.installGuide.InstallGuideViewPage;
import appland.installGuide.InstallGuideEditorProvider;
import appland.toolwindow.installGuide.InstallGuidePanel;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.content.Content;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.Alarm;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;

import static com.intellij.psi.NavigatablePsiElement.EMPTY_NAVIGATABLE_ELEMENT_ARRAY;

public class AppMapWindowPanel extends SimpleToolWindowPanel implements DataProvider, Disposable {
    private static final Logger LOG = Logger.getInstance("#appmap.toolwindow");
    private static final int TREE_REFRESH_DELAY_MILLIS = 250;
    private static final long INPUT_FILTER_DELAY_MILLIS = 250L; // + TREE_REFRESH_DELAY after typing stopped

    @NotNull
    private final SimpleTree tree;
    private final StructureTreeModel<AppMapTreeModel> treeModel;
    @NotNull
    private final Project project;
    // debounce requests for AppMap tree refresh
    private final Alarm treeRefreshAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    // debounce filter requests when search text changes
    private volatile Alarm filterInputAlarm;

    private volatile boolean isToolWindowVisible;
    private volatile boolean hasPendingTreeRefresh;

    public AppMapWindowPanel(@NotNull Project project, @NotNull Content parent) {
        super(true);
        Disposer.register(parent, this);

        var appMapModel = new AppMapTreeModel(project);

        this.project = project;
        this.treeModel = createModel(appMapModel, this);
        this.tree = createTree(this, treeModel);

        setToolbar(createToolBar(appMapModel));
        setContent(ScrollPaneFactory.createScrollPane(tree));
        add(createUserMilestonesPanel(), BorderLayout.SOUTH);

        // refresh when dumb mode changes
        var busConnection = project.getMessageBus().connect(this);
        busConnection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void exitDumbMode() {
                rebuild(false);
            }
        });

        // refresh when VirtualFiles change
        VirtualFileManager.getInstance().addAsyncFileListener(events -> {
            var appMapChanged = false;
            for (var event : events) {
                var file = event.getFile();
                if (file != null && AppMapFiles.isAppMap(file)) {
                    LOG.debug("appmap VirtualFile changes, rebuilding tree");
                    appMapChanged = true;
                    break;
                }
            }

            return !appMapChanged ? null : new AsyncFileListener.ChangeApplier() {
                @Override
                public void afterVfsChange() {
                    LOG.debug("afterVfsChange, invalidating tree");
                    rebuild(false);
                }
            };
        }, this);
    }

    @Override
    public void dispose() {
        LOG.debug("disposing AppMap tool window");
    }

    @NotNull
    private StructureTreeModel<AppMapTreeModel> createModel(@NotNull AppMapTreeModel model, @NotNull Disposable disposable) {
        var treeModel = new StructureTreeModel<>(model, disposable);
        // sort alphabetically, case-insensitive
        treeModel.setComparator(Comparator.comparing(NodeDescriptor::toString, String.CASE_INSENSITIVE_ORDER));
        return treeModel;
    }

    /**
     * Creates a panel with the search text field and the start and stop actions.
     */
    @NotNull
    private JComponent createToolBar(AppMapTreeModel appMapModel) {
        var actions = new DefaultActionGroup();
        actions.add(new StartAppMapRecordingAction());
        actions.add(new StopAppMapRecordingAction());

        var bar = ActionManager.getInstance().createActionToolbar("appmapToolWindow", actions, true);
        bar.setTargetComponent(this);
        bar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);

        var panel = new JPanel(new BorderLayout());
        panel.add(createNameFilter(appMapModel), BorderLayout.CENTER);
        panel.add(bar.getComponent(), BorderLayout.EAST);
        return panel;
    }

    @NotNull
    private SimpleTree createTree(@NotNull Disposable disposable, StructureTreeModel<AppMapTreeModel> treeModel) {
        var tree = new SimpleTree(new AsyncTreeModel(treeModel, true, disposable));
        tree.getEmptyText().setText(AppMapBundle.get("toolwindow.appmap.emptyText"));
        tree.getEmptyText().appendSecondaryText(
                AppMapBundle.get("toolwindow.appmap.installAgentEmptyText"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> InstallGuideEditorProvider.open(project, InstallGuideViewPage.InstallAgent));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        TreeUtil.installActions(tree);
        new EditSourceOnDoubleClickHandler.TreeMouseListener(tree, null).installOn(tree);
        EditSourceOnEnterKeyHandler.install(tree);
        return tree;
    }

    @NotNull
    private JPanel createUserMilestonesPanel() {
        return new InstallGuidePanel(project);
    }

    public void rebuild(boolean force) {
        if (force || isToolWindowVisible) {
            hasPendingTreeRefresh = false;

            treeRefreshAlarm.cancelAllRequests();
            treeRefreshAlarm.addRequest(treeModel::invalidate, TREE_REFRESH_DELAY_MILLIS, false);
        } else {
            LOG.debug("rebuild with hidden AppMap tool window");
            hasPendingTreeRefresh = true;
        }
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
            var file = getSelectedFile();
            return file == null ? null : PsiManager.getInstance(project).findFile(file);
        } else if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) {
            var file = getSelectedFile();
            if (file == null) {
                return null;
            }
            var psiFile = PsiManager.getInstance(project).findFile(file);
            return psiFile == null ? EMPTY_NAVIGATABLE_ELEMENT_ARRAY : new Navigatable[]{psiFile};
        } else if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
            return getSelectedFile();
        } else if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
            var file = getSelectedFile();
            return file == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{file};
        }

        return super.getData(dataId);
    }

    private @Nullable VirtualFile getSelectedFile() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (!(userObject instanceof AppMapTreeModel.SingleAppMapDescriptor)) {
            return null;
        }
        var data = ((AppMapTreeModel.SingleAppMapDescriptor) userObject).getAppMapData();
        var filepath = data.getSystemIndependentFilepath();
        return LocalFileSystem.getInstance().findFileByPath(filepath);
    }

    @NotNull
    private SearchTextField createNameFilter(@NotNull AppMapTreeModel appMapModel) {
        var textFilter = new SearchTextField();
        filterInputAlarm = createAlarm(textFilter);

        textFilter.getTextEditor().getEmptyText().setText(AppMapBundle.get("toolwindow.appmap.filterEmptyText"));
        textFilter.getTextEditor().addActionListener(e -> {
            LOG.debug("applying appmap filter: " + textFilter.getText());
            appMapModel.setNameFilter(textFilter.getText());
            rebuild(false);
        });
        textFilter.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                LOG.debug("applying delayed appmap filter: " + textFilter.getText());
                filterInputAlarm.cancelAllRequests();
                filterInputAlarm.addComponentRequest(() -> {
                    appMapModel.setNameFilter(textFilter.getText());
                    rebuild(false);
                }, INPUT_FILTER_DELAY_MILLIS);
            }
        });
        return textFilter;
    }

    public void onToolWindowShown() {
        LOG.debug("onToolWindowShown");
        this.isToolWindowVisible = true;

        if (hasPendingTreeRefresh) {
            LOG.debug("Triggering pending refresh of AppMap tool window");
            rebuild(true);
        }
    }

    public void onToolWindowHidden() {
        LOG.debug("onToolWindowHidden");
        this.isToolWindowVisible = false;
    }

    @NotNull
    private Alarm createAlarm(@NotNull SearchTextField textFilter) {
        // 2021.3 is showing a notification (at least in EAPs) that the constructor used above is deprecated
        // it's too visible and distracting, so we try to work around it
        if (ApplicationInfo.getInstance().getBuild().getBaselineVersion() >= 213) {
            try {
                var constructor = Alarm.class.getConstructor(JComponent.class, Disposable.class);
                return constructor.newInstance(textFilter, this);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                // ignore, use fallback below
            }
        }

        // fallback to deprecated method
        var alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
        alarm.setActivationComponent(textFilter);
        return alarm;
    }
}
