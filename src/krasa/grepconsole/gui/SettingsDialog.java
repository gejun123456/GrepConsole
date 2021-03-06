package krasa.grepconsole.gui;

import static krasa.grepconsole.Cloner.deepClone;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.NumberFormatter;
import javax.swing.tree.*;

import krasa.grepconsole.gui.table.*;
import krasa.grepconsole.gui.table.CheckboxTreeTable;
import krasa.grepconsole.model.*;
import krasa.grepconsole.plugin.*;
import krasa.grepconsole.tail.TailIntegrationForm;

import com.centerkey.utils.BareBonesBrowserLaunch;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.*;
import com.intellij.ui.*;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.tree.TreeUtil;

public class SettingsDialog {
	private static final Logger log = Logger.getInstance(SettingsDialog.class);
	private final SettingsContext settingsContext;
	private JPanel rootComponent;
	private CheckboxTreeTable table;
	private JButton addNewButton;
	private JButton resetToDefaultButton;
	private JCheckBox enableHighlightingCheckBox;
	private JFormattedTextField maxLengthToMatch;
	private JCheckBox enableMaxLength;
	private JButton duplicateButton;
	private JButton deleteButton;
	private JCheckBox enableFiltering;
	private JCheckBox ansi;
	private JCheckBox hideAnsiCharacters;
	private JCheckBox encodeText;
	private JCheckBox multilineOutput;
	private JButton DONATEButton;
	private JCheckBox showStatsInConsole;
	private JCheckBox showStatsInStatusBar;
	private JButton fileTailSettings;
	private JButton reddit;
	private JButton addNewGroup;
	private JLabel contextSpecificText;
	private JCheckBox enableFoldings;
	private PluginState settings;

	public SettingsDialog(PluginState settings) {
		this(settings, SettingsContext.NONE);
	}

	public SettingsDialog(PluginState settings, SettingsContext settingsContext) {
		this.settingsContext = settingsContext;
		this.settings = settings;
		DONATEButton.setBorder(null);
		DONATEButton.setContentAreaFilled(false);
		DONATEButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BareBonesBrowserLaunch.openURL("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=75YN7U7H7D7XU&lc=CZ&item_name=Grep%20Console%20%2d%20IntelliJ%20plugin&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest");
			}
		});
		reddit.setBorder(null);
		reddit.setContentAreaFilled(false);
		reddit.setBorderPainted(false);
		reddit.setFocusPainted(false);
		reddit.setOpaque(false);
		reddit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BareBonesBrowserLaunch.openURL("http://www.reddit.com/r/IntelliJIDEA/");
			}
		});
		addNewButton.addActionListener(new AddNewItemAction());
		addNewGroup.addActionListener(new AddNewGroupAction());
		resetToDefaultButton.addActionListener(new ResetToDefaultAction());
		duplicateButton.addActionListener(new DuplicateAction());
		deleteButton.addActionListener(new DeleteAction());
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					Object source = e.getSource();
					if (source instanceof DefaultListSelectionModel) {
						setSelectedRow(((DefaultListSelectionModel) source).getLeadSelectionIndex());
					}

				}
			}
		});
		table.addMouseListener(rightClickMenu());
		table.addKeyListener(new DeleteListener());
		disableCopyDeleteButton();
		ansi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (ansi.getModel().isSelected()) {
					hideAnsiCharacters.getModel().setSelected(true);
				}
			}
		});

		fileTailSettings.addActionListener(new FileTailSettings());

		if (settingsContext == SettingsContext.CONSOLE) {
			contextSpecificText.setText("Select items for which statistics should be displayed ('"
					+ SettingsTableBuilder.CONSOLE_COUNT + "' column)");
		} else if (settingsContext == SettingsContext.STATUS_BAR) {
			contextSpecificText.setText("Select items for which statistics should be displayed ('"
					+ SettingsTableBuilder.STATUS_BAR_COUNT + "' column)");
		} else {
			contextSpecificText.setVisible(false);
		}
	}

	public MouseAdapter rightClickMenu() {
		return new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
				} else if (SwingUtilities.isRightMouseButton(e)) {
					if (getSelectedNode() == null) {
						return;
					}
					JPopupMenu popup = new JBPopupMenu();
					GrepExpressionItem selectedGrepExpressionItem = getSelectedGrepExpressionItem();
					if (selectedGrepExpressionItem != null) {
						popup.add(getConvertAction(selectedGrepExpressionItem));
					}
					popup.add(newMenuItem("Add New Item", new AddNewItemAction()));
					popup.add(newMenuItem("Duplicate", new DuplicateAction()));
					popup.add(newMenuItem("Delete", new DeleteAction()));
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}

			private JMenuItem newMenuItem(String name, ActionListener l) {
				final JMenuItem item = new JMenuItem(name);
				item.addActionListener(l);
				return item;
			}

			private JMenuItem getConvertAction(final GrepExpressionItem item) {
				final boolean highlightOnlyMatchingText = item.isHighlightOnlyMatchingText();
				final JMenuItem convert = new JMenuItem(highlightOnlyMatchingText ? "Convert to whole line"
						: "Convert to words only");

				try {
					convert.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if (!highlightOnlyMatchingText) {
								getSelectedGrepExpressionItem().setHighlightOnlyMatchingText(true);
								getSelectedGrepExpressionItem().setContinueMatching(true);
								if (item.getGrepExpression().startsWith(".*")) {
									item.grepExpression(item.getGrepExpression().substring(2));
								}
								if (item.getGrepExpression().endsWith(".*")) {
									item.grepExpression(item.getGrepExpression().substring(0,
											item.getGrepExpression().length() - 2));
								}
							} else {
								getSelectedGrepExpressionItem().setHighlightOnlyMatchingText(false);
								getSelectedGrepExpressionItem().setContinueMatching(false);
								if (!item.getGrepExpression().startsWith(".*")) {
									item.grepExpression(".*" + item.getGrepExpression());
								}
								if (!item.getGrepExpression().endsWith(".*")) {
									item.grepExpression(item.getGrepExpression() + ".*");
								}
							}
							reloadNode(SettingsDialog.this.getSelectedNode());
						}

					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				return convert;
			}
		};
	}

	private void reloadNode(final DefaultMutableTreeNode selectedNode) {
		DefaultTreeModel model = (DefaultTreeModel) table.getTree().getModel();
		model.nodeChanged(selectedNode);
	}

	private GrepExpressionItem getSelectedGrepExpressionItem() {
		DefaultMutableTreeNode selectedNode = getSelectedNode();
		GrepExpressionItem item = null;
		if (selectedNode instanceof GrepExpressionItemTreeNode) {
			item = (GrepExpressionItem) selectedNode.getUserObject();
		}
		return item;
	}

	public DefaultMutableTreeNode getSelectedNode() {
		return (DefaultMutableTreeNode) table.getTree().getLastSelectedPathComponent();
	}

	private void disableCopyDeleteButton() {
		deleteButton.setEnabled(false);
		duplicateButton.setEnabled(false);
	}

	private void setSelectedRow(Integer selectedRow) {
		deleteButton.setEnabled(selectedRow != null && selectedRow >= 0);
		duplicateButton.setEnabled(selectedRow != null && selectedRow >= 0);
	}

	public JPanel getRootComponent() {
		return rootComponent;
	}

	public PluginState getSettings() {
		getData(getProfile());
		return settings;
	}

	public Profile getProfile() {
		return settings.getDefaultProfile();
	}

	public void importFrom(PluginState settings) {
		this.settings = settings;
		setData(settings.getDefaultProfile());
		resetTreeModel();
	}

	private void resetTreeModel() {
		CheckedTreeNode root = (CheckedTreeNode) table.getTree().getModel().getRoot();
		root.removeAllChildren();
		for (GrepExpressionGroup group : getProfile().getGrepExpressionGroups()) {
			GrepExpressionGroupTreeNode newChild = new GrepExpressionGroupTreeNode(group);
			for (GrepExpressionItem grepExpressionItem : group.getGrepExpressionItems()) {
				newChild.add(new GrepExpressionItemTreeNode(grepExpressionItem));
			}
			root.add(newChild);
		}
		TableUtils.reloadTree(table);
		TreeUtil.expandAll(table.getTree());
	}

	public boolean isSettingsModified(PluginState data) {
		getData(getProfile());
		return !this.settings.equals(data);
	}

	private void createUIComponents() {
		fileTailSettings = new JButton();
		NumberFormatter numberFormatter = new NumberFormatter();
		numberFormatter.setMinimum(0);
		maxLengthToMatch = new JFormattedTextField(numberFormatter);
		table = new SettingsTableBuilder(this).getTable();
	}

	public void setData(Profile data) {
		enableMaxLength.setSelected(data.isEnableMaxLengthLimit());
		ansi.setSelected(data.isEnableAnsiColoring());
		enableHighlightingCheckBox.setSelected(data.isEnabledHighlighting());
		showStatsInStatusBar.setSelected(data.isShowStatsInStatusBarByDefault());
		showStatsInConsole.setSelected(data.isShowStatsInConsoleByDefault());
		maxLengthToMatch.setText(data.getMaxLengthToMatch());
		hideAnsiCharacters.setSelected(data.isHideAnsiCommands());
		enableFiltering.setSelected(data.isEnabledInputFiltering());
		multilineOutput.setSelected(data.isMultiLineOutput());
		encodeText.setSelected(data.isEncodeText());
		enableFoldings.setSelected(data.isEnableFoldings());
	}

	public void getData(Profile data) {
		data.setEnableMaxLengthLimit(enableMaxLength.isSelected());
		data.setEnableAnsiColoring(ansi.isSelected());
		data.setEnabledHighlighting(enableHighlightingCheckBox.isSelected());
		data.setShowStatsInStatusBarByDefault(showStatsInStatusBar.isSelected());
		data.setShowStatsInConsoleByDefault(showStatsInConsole.isSelected());
		data.setMaxLengthToMatch(maxLengthToMatch.getText());
		data.setHideAnsiCommands(hideAnsiCharacters.isSelected());
		data.setEnabledInputFiltering(enableFiltering.isSelected());
		data.setMultiLineOutput(multilineOutput.isSelected());
		data.setEncodeText(encodeText.isSelected());
		data.setEnableFoldings(enableFoldings.isSelected());
	}

	public boolean isModified(Profile data) {
		if (enableMaxLength.isSelected() != data.isEnableMaxLengthLimit())
			return true;
		if (ansi.isSelected() != data.isEnableAnsiColoring())
			return true;
		if (enableHighlightingCheckBox.isSelected() != data.isEnabledHighlighting())
			return true;
		if (showStatsInStatusBar.isSelected() != data.isShowStatsInStatusBarByDefault())
			return true;
		if (showStatsInConsole.isSelected() != data.isShowStatsInConsoleByDefault())
			return true;
		if (maxLengthToMatch.getText() != null ? !maxLengthToMatch.getText().equals(data.getMaxLengthToMatch())
				: data.getMaxLengthToMatch() != null)
			return true;
		if (hideAnsiCharacters.isSelected() != data.isHideAnsiCommands())
			return true;
		if (enableFiltering.isSelected() != data.isEnabledInputFiltering())
			return true;
		if (multilineOutput.isSelected() != data.isMultiLineOutput())
			return true;
		if (encodeText.isSelected() != data.isEncodeText())
			return true;
		if (enableFoldings.isSelected() != data.isEnableFoldings())
			return true;
		return false;
	}

	private GrepExpressionItem newItem() {
		GrepExpressionItem item = new GrepExpressionItem();
		item.setGrepExpression("foo");
		item.setEnabled(true);
		item.setContinueMatching(true);
		item.setHighlightOnlyMatchingText(true);
		item.getStyle().setBackgroundColor(new GrepColor(true, JBColor.CYAN));
		return item;
	}

	private GrepExpressionGroup getGrepExpressionGroup(DefaultMutableTreeNode selectedNode) {
		return (GrepExpressionGroup) selectedNode.getUserObject();
	}

	private GrepExpressionItem getSelectedGrepExpressionItem(DefaultMutableTreeNode selectedNode) {
		return (GrepExpressionItem) selectedNode.getUserObject();
	}

	public void rebuildProfile() {
		List<GrepExpressionGroup> grepExpressionGroups = getProfile().getGrepExpressionGroups();
		grepExpressionGroups.clear();

		DefaultMutableTreeNode model = (DefaultMutableTreeNode) table.getTree().getModel().getRoot();
		Enumeration children = model.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode o = (DefaultMutableTreeNode) children.nextElement();
			if (o instanceof GrepExpressionGroupTreeNode) {
				GrepExpressionGroup grepExpressionGroup = ((GrepExpressionGroupTreeNode) o).getGrepExpressionGroup();
				grepExpressionGroup.getGrepExpressionItems().clear();
				Enumeration children1 = o.children();
				while (children1.hasMoreElements()) {
					Object o1 = children1.nextElement();
					if (o1 instanceof GrepExpressionItemTreeNode) {
						GrepExpressionItem grepExpressionItem = ((GrepExpressionItemTreeNode) o1).getGrepExpressionItem();
						grepExpressionGroup.add(grepExpressionItem);
					} else {
						throw new IllegalStateException("unexpected tree node" + o1);
					}
				}
				grepExpressionGroups.add(grepExpressionGroup);
			} else {
				throw new IllegalStateException("unexpected tree node" + o);
			}
		}
	}

	private class DeleteListener extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			final int keyCode = e.getKeyCode();
			if (keyCode == KeyEvent.VK_DELETE) {
				delete();
			}
		}
	}

	private void delete() {
		TreeNode selectNode = null;
		TreeTableTree tree = table.getTree();
		int[] selectionRows = tree.getSelectionRows();
		if (selectionRows == null) {
			return;
		}
		Arrays.sort(selectionRows);
		selectionRows = ArrayUtil.reverseArray(selectionRows);
		for (int selectionRow : selectionRows) {
			TreePath treePath = tree.getPathForRow(selectionRow);
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();

			if (selectNode == null || selectNode == selectedNode || selectNode.getParent() == selectedNode) {
				int index = parent.getIndex(selectedNode);
				if (index + 1 < parent.getChildCount()) {
					selectNode = parent.getChildAt(index + 1);
				} else if (index > 0) {
					selectNode = parent.getChildAt(index - 1);
				} else {
					selectNode = parent;
				}
			}
			parent.remove(selectedNode);
		}
		rebuildProfile();
		TableUtils.reloadTree(this.table);
		TableUtils.selectNode((DefaultMutableTreeNode) selectNode, table);
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) table.getTree().getModel().getRoot();
		if (root.getChildCount() == 0) {
			disableCopyDeleteButton();
		}
	}

	private class AddNewItemAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			DefaultMutableTreeNode selectedNode = getSelectedNode();
			CheckedTreeNode newChild;
			if (selectedNode == null) {
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) table.getTree().getModel().getRoot();
				GrepExpressionGroupTreeNode aNew = new GrepExpressionGroupTreeNode(new GrepExpressionGroup("new"));
				newChild = new GrepExpressionItemTreeNode(newItem());
				aNew.add(newChild);
				root.add(aNew);
			} else if (selectedNode.getUserObject() instanceof GrepExpressionGroup) {
				newChild = new GrepExpressionItemTreeNode(newItem());
				selectedNode.add(newChild);
			} else {
				GrepExpressionGroupTreeNode parent = (GrepExpressionGroupTreeNode) selectedNode.getParent();
				newChild = new GrepExpressionItemTreeNode(newItem());
				parent.insert(newChild, parent.getIndex(selectedNode) + 1);
			}
			rebuildProfile();
			TableUtils.reloadTree(table);
			TableUtils.selectNode(newChild, table);
		}
	}

	private class AddNewGroupAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) table.getTree().getModel().getRoot();
			GrepExpressionGroupTreeNode aNew = new GrepExpressionGroupTreeNode(new GrepExpressionGroup("new"));
			root.add(aNew);
			rebuildProfile();
			TableUtils.reloadTree(table);
			TableUtils.selectNode(aNew, table);
		}
	}

	private class ResetToDefaultAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			SettingsDialog.this.settings.setProfiles(DefaultState.createDefault());
			disableCopyDeleteButton();
			importFrom(SettingsDialog.this.settings);
		}
	}

	private class DuplicateAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			DefaultMutableTreeNode selectedNode = getSelectedNode();
			if (selectedNode instanceof GrepExpressionItemTreeNode) {
				GrepExpressionItemTreeNode newChild = new GrepExpressionItemTreeNode(copy(selectedNode));
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
				parent.insert(newChild, parent.getIndex(selectedNode) + 1);

				TableUtils.reloadTree(SettingsDialog.this.table);
				TableUtils.selectNode(newChild, SettingsDialog.this.table);
			} else if (selectedNode instanceof GrepExpressionGroupTreeNode) {
				GrepExpressionGroup group = copy((GrepExpressionGroup) selectedNode.getUserObject());
				GrepExpressionGroupTreeNode newChild = new GrepExpressionGroupTreeNode(group);
				for (GrepExpressionItem grepExpressionItem : group.getGrepExpressionItems()) {
					newChild.add(new GrepExpressionItemTreeNode(grepExpressionItem));
				}
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
				parent.insert(newChild, parent.getIndex(selectedNode) + 1);

				TableUtils.reloadTree(SettingsDialog.this.table);
				TableUtils.expand(newChild, SettingsDialog.this.table);
				TableUtils.selectNode(newChild, SettingsDialog.this.table);
			}
			rebuildProfile();
		}

		private GrepExpressionGroup copy(final GrepExpressionGroup grepExpressionGroup) {
			GrepExpressionGroup group = deepClone(grepExpressionGroup);
			for (GrepExpressionItem grepExpressionItem : group.getGrepExpressionItems()) {
				grepExpressionItem.generateNewId();
			}
			return group;
		}

		private GrepExpressionItem copy(DefaultMutableTreeNode selectedNode) {
			GrepExpressionItem expressionItem = deepClone((GrepExpressionItem) selectedNode.getUserObject());
			expressionItem.generateNewId();
			return expressionItem;
		}
	}

	private class DeleteAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			delete();
		}
	}

	private class FileTailSettings implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			final TailIntegrationForm form = new TailIntegrationForm();
			form.setData(settings.getTailSettings());

			DialogBuilder builder = new DialogBuilder(SettingsDialog.this.getRootComponent());
			builder.setCenterPanel(form.getRoot());
			builder.setDimensionServiceKey("GrepConsoleTailFileDialog");
			builder.setTitle("Tail File settings");
			builder.removeAllActions();
			builder.addOkAction();
			builder.addCancelAction();

			boolean isOk = builder.show() == DialogWrapper.OK_EXIT_CODE;
			if (isOk) {
				form.getData(settings.getTailSettings());
				GrepConsoleApplicationComponent.getInstance().getState().setTailSettings(settings.getTailSettings());
				form.rebind(settings.getTailSettings());
			}
		}
	}
}
