package krasa.grepconsole.filter;

import java.lang.ref.WeakReference;
import java.util.*;

import krasa.grepconsole.ansi.AnsiConsoleStyleProcessor;
import krasa.grepconsole.filter.support.ConsoleListener;
import krasa.grepconsole.model.Profile;

import org.apache.commons.net.util.Base64;

import com.intellij.execution.filters.InputFilter;
import com.intellij.execution.ui.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;

public class AnsiInputFilter extends AbstractFilter implements InputFilter, ConsoleListener {
	protected AnsiConsoleStyleProcessor ansiConsoleStyleProcessor;
	private WeakReference<ConsoleView> console;

	public AnsiInputFilter(Project project) {
		super(project);
		ansiConsoleStyleProcessor = new AnsiConsoleStyleProcessor(profile);
		ansiConsoleStyleProcessor.addListener(this);
	}

	public AnsiInputFilter(Profile profile) {
		super(profile);
		ansiConsoleStyleProcessor = new AnsiConsoleStyleProcessor(profile);
		ansiConsoleStyleProcessor.addListener(this);
	}

	@Override
	public List<Pair<String, ConsoleViewContentType>> applyFilter(String s,
			ConsoleViewContentType consoleViewContentType) {
		List<Pair<String, ConsoleViewContentType>> list = null;

		if (profile.isEnableAnsiColoring() || profile.isHideAnsiCommands()) {
			list = ansiConsoleStyleProcessor.process(s, consoleViewContentType);
		}
		if (profile.isEncodeText()) {
			if (list == null) {
				list = new ArrayList<Pair<String, ConsoleViewContentType>>(2);
			}
			if (list.isEmpty()) {
				list.add(new Pair<String, ConsoleViewContentType>(s, consoleViewContentType));
			}
			StringBuilder stringBuilder = new StringBuilder();
			for (Pair<String, ConsoleViewContentType> stringConsoleViewContentTypePair : list) {
				stringBuilder.append(stringConsoleViewContentTypePair.first);
			}
			list.add(Pair.create(">>>input:" + Base64.encodeBase64URLSafeString(s.getBytes()), consoleViewContentType));
			list.add(Pair.create("\n>>>result:" + Base64.encodeBase64URLSafeString(stringBuilder.toString().getBytes())
					+ "\n\n", consoleViewContentType));
		}

		if (list == null || list.isEmpty()) {
			return null;
		}
		return list;
	}

	public void setProfile(Profile profile) {
		ansiConsoleStyleProcessor.setProfile(profile);
	}

	@Override
	public void onChange() {
		super.onChange();

		ansiConsoleStyleProcessor.setProfile(profile);
	}

	public void setConsole(ConsoleView console) {
		this.console = new WeakReference<ConsoleView>(console);
	}

	@Override
	public void clearConsole() {
		final ConsoleView consoleView = console.get();
		if (consoleView != null) {
			console.clear();
		}
	}

	public boolean isRegistered() {
		return console != null;
	}
}
