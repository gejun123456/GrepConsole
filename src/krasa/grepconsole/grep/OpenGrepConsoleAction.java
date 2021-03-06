package krasa.grepconsole.grep;

import java.awt.*;
import java.io.OutputStream;

import javax.swing.*;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.*;
import com.intellij.execution.ui.layout.impl.RunnerLayoutUiImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;

import krasa.grepconsole.plugin.ServiceManager;
import krasa.grepconsole.utils.Utils;

public class OpenGrepConsoleAction extends DumbAwareAction {

	public OpenGrepConsoleAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
		super(text, description, icon);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project eventProject = getEventProject(e);
		ConsoleViewImpl originalConsoleView = (ConsoleViewImpl) getConsoleView(e);
		final GrepCopyingFilter copyingFilter = ServiceManager.getInstance().getCopyingFilter(originalConsoleView);
		if (copyingFilter == null) {
			throw new IllegalStateException("Console not supported: " + originalConsoleView);
		}
		String expression = getExpression(e);
		CopyListenerModel copyListenerModel = new CopyListenerModel(false, false, false, expression, null);
		RunnerLayoutUi runnerLayoutUi = getRunnerLayoutUi(eventProject, originalConsoleView, e.getDataContext());

		final LightProcessHandler myProcessHandler = new LightProcessHandler();
		final ConsoleViewImpl newConsole = (ConsoleViewImpl) createConsole(eventProject, myProcessHandler);
		DefaultActionGroup actions = new DefaultActionGroup();

		final GrepCopyingListener copyingListener = new GrepCopyingListener(copyListenerModel) {

			@Override
			protected void onMatch(String s, Key key) {
				myProcessHandler.notifyTextAvailable(s, key);
			}

		};

		final GrepPanel quickFilterPanel = new GrepPanel(originalConsoleView, newConsole, copyingListener, expression,
				runnerLayoutUi);
		final MyJPanel consolePanel = createConsolePanel(runnerLayoutUi, newConsole, actions, quickFilterPanel);
		for (AnAction action : newConsole.createConsoleActions()) {
			actions.add(action);
		}

		final Content tab = runnerLayoutUi.createContent(getContentType(runnerLayoutUi), consolePanel, title(expression),
				getTemplatePresentation().getSelectedIcon(), consolePanel);
		runnerLayoutUi.addContent(tab);
		runnerLayoutUi.selectAndFocus(tab, true, true);

		for (String s : originalConsoleView.getEditor().getDocument().getText().split("\n")) {
			copyingListener.process(s + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
		}
		copyingFilter.addListener(copyingListener);

		Disposer.register(tab, consolePanel);
		Disposer.register(tab, newConsole);
		Disposer.register(consolePanel, quickFilterPanel);
		Disposer.register(newConsole, new Disposable() {
			@Override
			public void dispose() {
				copyingFilter.removeListener(copyingListener);
			}
		});


		Disposable inactiveTitleDisposer;
		Container parent = originalConsoleView.getParent();
		if (parent instanceof MyJPanel && !Disposer.isDisposed((MyJPanel) parent)) {
			inactiveTitleDisposer = (MyJPanel) parent;
		} else {
			inactiveTitleDisposer = originalConsoleView;
		}

		Disposer.register(inactiveTitleDisposer, new Disposable() {
			@Override
			public void dispose() {
				// dispose chained grep consoles
				Disposer.dispose(consolePanel);
				updateTitle(tab, consolePanel.disposed, tab.getDisplayName());
			}
		});

		quickFilterPanel.setApplyCallback(new ApplyCallback() {
			@Override
			public void apply(CopyListenerModel copyListenerModel) {
				copyingListener.set(copyListenerModel);
				updateTitle(tab, consolePanel.disposed, copyListenerModel.getExpression());
			}

		});
	}

	protected String title(String expression) {
		return StringUtils.substring(expression, 0, 20);
	}

	@NotNull
	protected String getExpression(AnActionEvent e) {
		String s = Utils.getString(e);
		if (s == null)
			s = "";
		if (s.endsWith("\n")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	protected String getContentType(RunnerLayoutUi runnerLayoutUi) {
		ContentManager contentManager = runnerLayoutUi.getContentManager();
		Content selectedContent = contentManager.getSelectedContent();
		return RunnerLayoutUiImpl.CONTENT_TYPE.get(selectedContent);
	}

	protected void updateTitle(Content logContent, boolean disposed, String s) {
		logContent.setDisplayName(title(s) + (disposed ? " (Inactive)" : ""));
	}

	interface ApplyCallback {

		void apply(CopyListenerModel copyListenerModel);
	}

	@Nullable
	private RunnerLayoutUi getRunnerLayoutUi(Project eventProject, ConsoleViewImpl originalConsoleView, DataContext dataContext) {
		RunnerLayoutUi runnerLayoutUi = null;

		RunContentManager contentManager = ExecutionManager.getInstance(eventProject).getContentManager();
		final RunContentDescriptor selectedContent = contentManager.getSelectedContent();
		if (selectedContent != null) {
			runnerLayoutUi = selectedContent.getRunnerLayoutUi();
		}

		if (runnerLayoutUi == null) {
			XDebugSession debugSession = XDebuggerManager.getInstance(eventProject).getDebugSession(
					originalConsoleView);
			if (debugSession != null) {
				runnerLayoutUi = debugSession.getUI();
			}
			if (debugSession == null) {
				XDebugSession currentSession = XDebuggerManager.getInstance(eventProject).getCurrentSession();
				if (currentSession != null) {
					runnerLayoutUi = currentSession.getUI();
				}
			}
		}

		if (runnerLayoutUi == null) {
			Container parent = originalConsoleView.getParent();
			if (parent instanceof MyJPanel) {
				runnerLayoutUi = ((MyJPanel) parent).runnerLayoutUi;
			}
		}
		return runnerLayoutUi;
	}

	protected static class LightProcessHandler extends ProcessHandler {
		@Override
		protected void destroyProcessImpl() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void detachProcessImpl() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean detachIsDefault() {
			return false;
		}

		@Override
		@Nullable
		public OutputStream getProcessInput() {
			return null;
		}
	}

	private static MyJPanel createConsolePanel(RunnerLayoutUi runnerLayoutUi, ConsoleView view, ActionGroup actions,
											   GrepPanel comp) {
		MyJPanel panel = new MyJPanel(runnerLayoutUi);
		panel.setLayout(new BorderLayout());
		panel.add(comp.getRootComponent(), BorderLayout.NORTH);
		panel.add(view.getComponent(), BorderLayout.CENTER);
		panel.add(createToolbar(actions), BorderLayout.WEST);
		return panel;
	}

	private static JComponent createToolbar(ActionGroup actions) {
		ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actions,
				false);
		return actionToolbar.getComponent();
	}

	private ConsoleView createConsole(@NotNull Project project, @NotNull ProcessHandler processHandler) {
		TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
		ConsoleView console = consoleBuilder.getConsole();
		console.attachToProcess(processHandler);
		return console;
	}

	private ConsoleView getConsoleView(AnActionEvent e) {
		return e.getData(LangDataKeys.CONSOLE_VIEW);
	}

	@Override
	public void update(AnActionEvent e) {
		Presentation presentation = e.getPresentation();
		boolean enabled = false;

		Project eventProject = getEventProject(e);
		ConsoleViewImpl originalConsoleView = (ConsoleViewImpl) getConsoleView(e);
		GrepCopyingFilter copyingFilter = ServiceManager.getInstance().getCopyingFilter(originalConsoleView);
		if (eventProject != null && copyingFilter != null) {
			RunnerLayoutUi runnerLayoutUi = getRunnerLayoutUi(eventProject, originalConsoleView, e.getDataContext());
			enabled = runnerLayoutUi != null;
		}

		presentation.setEnabled(enabled);

	}

	static class MyJPanel extends JPanel implements Disposable {
		private final RunnerLayoutUi runnerLayoutUi;
		private boolean disposed;

		public MyJPanel(RunnerLayoutUi runnerLayoutUi) {
			this.runnerLayoutUi = runnerLayoutUi;
		}

		@Override
		public void dispose() {
			disposed = true;
		}
	}
}
