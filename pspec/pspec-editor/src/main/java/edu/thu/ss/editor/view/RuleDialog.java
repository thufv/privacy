package edu.thu.ss.editor.view;

import static edu.thu.ss.editor.util.MessagesUtil.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import edu.thu.ss.editor.util.EditorUtil;
import edu.thu.ss.spec.lang.analyzer.PolicyResolver;
import edu.thu.ss.spec.lang.pojo.Action;
import edu.thu.ss.spec.lang.pojo.DataAssociation;
import edu.thu.ss.spec.lang.pojo.DataCategory;
import edu.thu.ss.spec.lang.pojo.DataRef;
import edu.thu.ss.spec.lang.pojo.Desensitization;
import edu.thu.ss.spec.lang.pojo.DesensitizeOperation;
import edu.thu.ss.spec.lang.pojo.ObjectRef;
import edu.thu.ss.spec.lang.pojo.Policy;
import edu.thu.ss.spec.lang.pojo.Restriction;
import edu.thu.ss.spec.lang.pojo.Rule;
import edu.thu.ss.spec.lang.pojo.UserCategory;
import edu.thu.ss.spec.lang.pojo.UserRef;
import edu.thu.ss.spec.util.PSpecUtil;

public class RuleDialog extends Dialog {
	private class RuleModel {
		List<UserRef> userRefs = new ArrayList<>();
		List<DataRef> dataRefs = new ArrayList<>();
		//only non-forbid restrictions
		List<Restriction> restrictions = new ArrayList<>();
		boolean forbid = false;

		public RuleModel(Rule rule) {
			for (UserRef ref : rule.getUserRefs()) {
				userRefs.add(ref.clone());
			}

			List<DataRef> list = rule.isSingle() ? rule.getDataRefs() : rule.getAssociation()
					.getDataRefs();
			for (DataRef ref : list) {
				dataRefs.add(ref.clone());
			}

			if (rule.getRestrictions().size() == 0 || rule.getRestriction().isForbid()) {
				forbid = true;
			} else {
				for (Restriction res : rule.getRestrictions()) {
					restrictions.add(res.clone());
				}
			}

		}

	}

	private static final String Label_Separator = ",";

	private Shell dialog;
	private Rule rule;
	private RuleModel model;

	private Policy policy;

	private int retCode;

	private Text ruleId;
	private Text shortDescription;
	private Text longDescription;
	private Button dataSingleType;
	private Button dataAssociationType;

	private Table userTable;
	private Table dataTable;

	private Button restrictType;
	private Button forbidType;

	private Composite effectComposite;
	private Composite restrictComposite;
	private Table selectedRestrictTable;
	private List<Table> restrictTables = new ArrayList<>();
	private List<Composite> restrictTableComposites = new ArrayList<>();

	private Button addRestriction;
	private Button deleteRestriction;

	private ScrolledComposite scroll;
	private Composite scrollContent;

	private final static double tableHeightRatio = (double) 1 / 6;

	private final static double longDescriptionHeightRatio = (double) 1 / 6;

	public RuleDialog(Shell parent, Rule rule, Policy policy) {
		super(parent, SWT.NONE);
		this.policy = policy;
		this.rule = rule;
		this.model = new RuleModel(rule);

		dialog = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
		dialog.setBackground(EditorUtil.getDefaultBackground());
		Display display = Display.getCurrent();
		dialog.setSize(display.getClientArea().width / 2, display.getClientArea().height * 2 / 3);
		dialog
				.setMinimumSize(display.getClientArea().width / 3, display.getClientArea().height * 2 / 3);

		dialog.setText(getMessage(Rule));
		dialog.setLayout(new FillLayout());
	}

	public int open() {
		scroll = new ScrolledComposite(dialog, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
				| SWT.NO_BACKGROUND);
		scroll.setBackground(EditorUtil.getDefaultBackground());
		scrollContent = EditorUtil.newComposite(scroll);
		scrollContent.setLayout(new GridLayout(2, false));

		initializeContent(scrollContent);

		scroll.setContent(scrollContent);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setMinSize(scrollContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		dialog.open();
		dialog.layout();
		Display display = getParent().getDisplay();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return retCode;
	}

	private void layoutScroll() {
		//scroll.setMinSize();
		scroll.setMinHeight(scrollContent.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

	}

	private void initializeContent(final Composite parent) {
		EditorUtil.newLabel(parent, getMessage(Rule_ID), EditorUtil.labelData());
		ruleId = EditorUtil.newText(parent, EditorUtil.textData());
		ruleId.setText(rule.getId());

		EditorUtil.newLabel(parent, getMessage(Short_Description), EditorUtil.labelData());
		shortDescription = EditorUtil.newText(parent, EditorUtil.textData());
		shortDescription.setText(rule.getShortDescription());

		EditorUtil.newLabel(parent, getMessage(Long_Description), EditorUtil.labelData());
		longDescription = new Text(parent, SWT.BORDER | SWT.V_SCROLL);
		longDescription.setText(rule.getLongDescription());
		GridData longData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		longData.minimumHeight = (int) (dialog.getSize().y * longDescriptionHeightRatio);
		longDescription.setLayoutData(longData);

		initializeUser(parent);
		initializeData(parent);
		initializeRestrictions(parent);

		Composite buttons = new Composite(parent, SWT.RIGHT_TO_LEFT);
		buttons.setBackground(EditorUtil.getDefaultBackground());
		GridData buttonsData = new GridData(SWT.END, SWT.CENTER, true, false, 2, 1);
		buttons.setLayoutData(buttonsData);
		buttons.setLayout(new RowLayout());

		Button ok = EditorUtil.newButton(buttons, getMessage(OK));

		ok.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//check user reference
				if (ruleId.getText().trim().isEmpty()) {
					EditorUtil.showMessageBox(dialog, "", getMessage(Rule_ID_Non_Empty_Message));
					return;

				}

				for (UserRef ref : model.userRefs) {
					if (ref.getRefid().isEmpty()) {
						EditorUtil.showMessageBox(dialog, "", getMessage(Rule_User_Ref_Non_Empty_Message));
						return;
					}
				}
				//check data reference
				for (DataRef ref : model.dataRefs) {
					if (ref.getRefid().isEmpty()) {
						EditorUtil.showMessageBox(dialog, "", getMessage(Rule_Data_Ref_Non_Empty_Message));
						return;
					}
				}
				if (dataAssociationType.getSelection()) {
					//check data association overlapping
					PolicyResolver resolver = new PolicyResolver();
					for (DataRef ref : model.dataRefs) {
						resolver.resolveDataRef(ref, policy.getDataContainer(), null);
					}
					for (int i = 0; i < model.dataRefs.size(); i++) {
						for (int j = i + 1; j < model.dataRefs.size(); j++) {
							if (PSpecUtil.intersects(model.dataRefs.get(i).getMaterialized(),
									model.dataRefs.get(j).getMaterialized())) {
								EditorUtil.showMessageBox(dialog, "",
										getMessage(Rule_Data_Association_Non_Overlap_Message));
								return;
							}
						}
					}
				}
				//check restrictions
				for (int i = 0; i < model.restrictions.size(); i++) {
					Restriction res = model.restrictions.get(i);
					boolean effective = false;
					for (Desensitization de : res.getDesensitizations()) {
						if (de.effective()) {
							effective = true;
						}
					}
					if (!effective) {
						EditorUtil.showMessageBox(dialog, "",
								getMessage(Rule_Restriction_Effective_Message, String.valueOf(i)));
						return;
					}
				}

				//set
				rule.setId(ruleId.getText().trim());
				rule.setShortDescription(shortDescription.getText().trim());
				rule.setLongDescription(longDescription.getText().trim());

				rule.setUserRefs(model.userRefs);

				if (dataSingleType.getSelection()) {
					rule.setDataRefs(model.dataRefs);
				} else {
					rule.getDataRefs().clear();
					DataAssociation association = new DataAssociation();
					association.setDataRefs(model.dataRefs);
					rule.setAssociation(association);
				}

				if (forbidType.getSelection()) {
					rule.getRestrictions().clear();
					Restriction res = new Restriction();
					res.setForbid(true);
					rule.getRestrictions().add(res);
				} else {
					rule.setRestrictions(model.restrictions);
				}

				retCode = SWT.OK;
				dialog.dispose();
			}
		});

		Button cancel = EditorUtil.newButton(buttons, getMessage(Cancel));
		cancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				retCode = SWT.CANCEL;
				dialog.dispose();
			}
		});

	}

	private void initializeUser(Composite parent) {
		EditorUtil.newLabel(parent, getMessage(User_Ref), EditorUtil.labelData());

		Composite userComposite = newComposite(parent);

		newDummyLabel(userComposite);
		Button addUser = EditorUtil.newButton(userComposite, getMessage(Add));
		final Button deleteUser = EditorUtil.newButton(userComposite, getMessage(Delete));
		deleteUser.setEnabled(false);
		Composite tableComposite = newTableComposite(userComposite);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableComposite.setLayout(tableLayout);

		userTable = newTable(tableComposite);

		TableColumn[] columns = new TableColumn[5];
		String[] titles = new String[] { getMessage(User_Category), getMessage(Exclude), "", "", "" };
		for (int i = 0; i < columns.length; i++) {
			columns[i] = new TableColumn(userTable, SWT.NONE);
			columns[i].setText(titles[i]);
			columns[i].setResizable(false);
		}
		//add a new row
		for (UserRef ref : model.userRefs) {
			addUserRow(ref);
		}
		if (model.userRefs.size() == 0) {
			UserRef ref = new UserRef();
			addUserRow(ref);
			model.userRefs.add(ref);
		}
		tableLayout.setColumnData(columns[0], new ColumnWeightData(1, columns[0].getWidth()));
		tableLayout.setColumnData(columns[1], new ColumnWeightData(2, columns[1].getWidth()));
		tableLayout.setColumnData(columns[2], new ColumnWeightData(1, columns[2].getWidth()));
		tableLayout.setColumnData(columns[3], new ColumnWeightData(0));
		tableLayout.setColumnData(columns[4], new ColumnWeightData(0));

		addUser.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				UserRef ref = new UserRef();
				addUserRow(ref);
				model.userRefs.add(ref);
			}
		});

		deleteUser.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (userTable.getSelectionCount() == 0) {
					return;
				}
				if (userTable.getItemCount() - userTable.getSelectionCount() == 0) {
					EditorUtil.showMessageBox(dialog, "", getMessage(Rule_User_Ref_Non_Empty_Message));
					return;
				}

				for (TableItem item : userTable.getSelection()) {
					EditorUtil.dispose(item);
					model.userRefs.remove((UserRef) item.getData());
				}
				userTable.remove(userTable.getSelectionIndices());
				deleteUser.setEnabled(false);
			}

		});

		userTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (userTable.getSelectionCount() > 0) {
					deleteUser.setEnabled(true);
				} else {
					deleteUser.setEnabled(false);
				}
			}
		});

		resize(userComposite);
	}

	private void initializeData(Composite parent) {
		EditorUtil.newLabel(parent, getMessage(Data_Ref), EditorUtil.labelData());

		Composite dataComposite = newComposite(parent);

		Composite typeComposite = newRadioComposite(dataComposite);
		dataSingleType = EditorUtil.newRadio(typeComposite, getMessage(Single));
		dataAssociationType = EditorUtil.newRadio(typeComposite, getMessage(Association));
		if (rule.isSingle()) {
			dataSingleType.setSelection(true);
		} else {
			dataAssociationType.setSelection(true);
		}

		Button addData = EditorUtil.newButton(dataComposite, getMessage(Add));
		final Button deleteData = EditorUtil.newButton(dataComposite, getMessage(Delete));
		deleteData.setEnabled(false);

		Composite tableComposite = newTableComposite(dataComposite);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableComposite.setLayout(tableLayout);

		dataTable = newTable(tableComposite);

		TableColumn[] columns = new TableColumn[6];
		String[] titles = new String[] { getMessage(Data_Category), getMessage(ACTION),
				getMessage(Exclude), "", "", "" };
		for (int i = 0; i < columns.length; i++) {
			columns[i] = new TableColumn(dataTable, SWT.NONE);
			columns[i].setText(titles[i]);
			columns[i].setResizable(false);
		}
		for (DataRef ref : model.dataRefs) {
			addDataRow(ref);
		}
		if (model.dataRefs.size() == 0) {
			DataRef ref = new DataRef();
			addDataRow(ref);
			model.dataRefs.add(ref);
		}

		tableLayout.setColumnData(columns[0], new ColumnWeightData(1, columns[0].getWidth()));
		tableLayout.setColumnData(columns[1], new ColumnWeightData(1, columns[1].getWidth()));
		tableLayout.setColumnData(columns[2], new ColumnWeightData(2, columns[2].getWidth()));
		tableLayout.setColumnData(columns[3], new ColumnWeightData(1, columns[2].getWidth()));
		tableLayout.setColumnData(columns[4], new ColumnWeightData(0));
		tableLayout.setColumnData(columns[5], new ColumnWeightData(0));

		dataSingleType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!dataSingleType.getSelection()) {
					return;
				}
				model.restrictions.clear();
				disposeRestrictTables();
				Restriction res = newRestriction();
				model.restrictions.add(res);

				addRestrictTable(restrictComposite, res);
				adjustEffectLayout();

				addRestriction.setEnabled(false);
				deleteRestriction.setEnabled(false);
			}
		});

		dataAssociationType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!dataAssociationType.getSelection()) {
					return;
				}
				addRestriction.setEnabled(true);
				deleteRestriction.setEnabled(true);

				model.restrictions.clear();
				disposeRestrictTables();
				Restriction res = newRestriction();
				model.restrictions.add(res);
				addRestrictTable(restrictComposite, res);
				adjustEffectLayout();

			}
		});

		addData.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DataRef ref = new DataRef();
				addDataRow(ref);
				model.dataRefs.add(ref);
				//update restrictions
				if (dataAssociationType.getSelection()) {
					for (Table table : restrictTables) {
						Restriction res = (Restriction) table.getData();
						Desensitization de = new Desensitization(ref);
						addDesensitizeRow(table, de);
						res.getDesensitizations().add(de);
					}
				}
			}

		});

		deleteData.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (dataTable.getSelectionCount() == 0) {
					return;
				}
				if (dataTable.getItemCount() - dataTable.getSelectionCount() == 0) {
					EditorUtil.showMessageBox(dialog, "", getMessage(Rule_Data_Ref_Non_Empty_Message));
					return;
				}
				boolean associate = dataAssociationType.getSelection();
				int[] index = dataTable.getSelectionIndices();
				for (int i : index) {
					TableItem item = dataTable.getItem(i);
					DataRef ref = (DataRef) item.getData();
					//update restrictions
					if (associate) {
						for (Table table : restrictTables) {
							Restriction res = (Restriction) table.getData();
							Desensitization de = res.getDesensitization(ref.getRefid());
							if (de != null) {
								res.getDesensitizations().remove(de);
							}
							EditorUtil.dispose(table.getItem(i));
						}
					}
					EditorUtil.dispose(item);
					model.dataRefs.remove(ref);
				}

				dataTable.remove(index);
				if (associate) {
					for (Table table : restrictTables) {
						table.remove(index);
					}
				}

				deleteData.setEnabled(false);

			}
		});

		dataTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (dataTable.getSelectionCount() > 0) {
					deleteData.setEnabled(true);
				} else {
					deleteData.setEnabled(false);
				}
			}
		});

		resize(dataComposite);
	}

	private void initializeRestrictions(final Composite parent) {
		EditorUtil.newLabel(parent, getMessage(Rule_Type), EditorUtil.labelData());

		effectComposite = newComposite(parent);

		Composite typeComposite = newRadioComposite(effectComposite);
		forbidType = EditorUtil.newRadio(typeComposite, getMessage(Forbid));
		restrictType = EditorUtil.newRadio(typeComposite, getMessage(Restrict));

		if (model.forbid) {
			forbidType.setSelection(true);
			model.restrictions.add(newRestriction());
		} else {
			restrictType.setSelection(true);
		}

		addRestriction = EditorUtil.newButton(effectComposite, getMessage(Add));
		deleteRestriction = EditorUtil.newButton(effectComposite, getMessage(Delete));
		if (dataSingleType.getSelection()) {
			addRestriction.setEnabled(false);
			deleteRestriction.setEnabled(false);
		}

		restrictComposite = newComposite(effectComposite);
		GridData restrictData = (GridData) restrictComposite.getLayoutData();
		restrictData.horizontalSpan = 3;

		if (forbidType.getSelection()) {
			addRestrictTable(restrictComposite, model.restrictions.get(0));
			forbidType.setSelection(true);
			hideRestrictions();
		} else {
			for (Restriction res : model.restrictions) {
				addRestrictTable(restrictComposite, res);
			}
			showRestrictions();
		}

		forbidType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!forbidType.getSelection()) {
					return;
				}
				hideRestrictions();
				model.forbid = true;
			}
		});

		restrictType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!restrictType.getSelection()) {
					return;
				}
				showRestrictions();
				model.forbid = false;
			}
		});

		addRestriction.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Restriction res = newRestriction();
				model.restrictions.add(res);
				addRestrictTable(restrictComposite, res);
				adjustEffectLayout();
			}
		});

		deleteRestriction.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (selectedRestrictTable == null) {
					return;
				}
				if (restrictTables.size() == 1) {
					EditorUtil.showMessageBox(dialog, "", getMessage(Rule_Restriction_Not_Empty_Message));
					return;
				}

				int index = restrictTables.indexOf(selectedRestrictTable);
				for (TableItem item : selectedRestrictTable.getItems()) {
					EditorUtil.dispose(item);
				}
				selectedRestrictTable.dispose();
				restrictTableComposites.get(index).dispose();

				restrictTables.remove(index);
				restrictTableComposites.remove(index);
				selectedRestrictTable = null;

				deleteRestriction.setEnabled(false);
			}

		});
		resize(effectComposite);
	}

	private void addUserRow(UserRef ref) {
		List<TableEditor> editors = new ArrayList<>();
		final TableItem item = new TableItem(userTable, SWT.NULL);
		item.setData(EditorUtil.Table_Editor, editors);
		item.setData(ref);
		final Combo userCombo = EditorUtil.newCombo(userTable, null);
		userCombo.setItems(EditorUtil.getCategoryItems(policy.getUserContainer()));
		EditorUtil.setSelectedItem(userCombo, ref.getRefid());

		editors.add(newTableEditor(userTable, userCombo, item, 0));

		item.setText(1, PSpecUtil.format(ref.getExcludeRefs(), Label_Separator));

		final Combo excludeUser = EditorUtil.newCombo(userTable, null);
		editors.add(newTableEditor(userTable, excludeUser, item, 2));
		if (!ref.getRefid().isEmpty()) {
			UserCategory user = policy.getUserCategory(ref.getRefid());
			excludeUser.setItems(EditorUtil.getChildCategoryItems(user, policy.getUserContainer()));
		}

		Button addExclude = EditorUtil.newButton(userTable, "+");
		editors.add(newTableEditor(userTable, addExclude, item, 3));

		Button deleteExclude = EditorUtil.newButton(userTable, "-");
		editors.add(newTableEditor(userTable, deleteExclude, item, 4));

		userCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				UserRef ref = (UserRef) item.getData();
				String text = userCombo.getText().trim();

				if (!text.isEmpty()) {
					//check duplicate
					for (UserRef exist : model.userRefs) {
						if (exist.getRefid().equals(text) && ref != exist) {
							EditorUtil.showMessage(dialog, getMessage(Rule_User_Ref_Unique_Message, text),
									Display.getCurrent().getCursorLocation());
							EditorUtil.setSelectedItem(userCombo, ref.getRefid());
							return;
						}
					}
				}

				UserCategory user = policy.getUserContainer().get(text);
				excludeUser.removeAll();
				excludeUser.setItems(EditorUtil.getChildCategoryItems(user, policy.getUserContainer()));

				ref.setRefid(text);
				ref.getExcludeRefs().clear();
				item.setText(1, PSpecUtil.format(ref.getExcludeRefs(), Label_Separator));
			}
		});

		addExclude.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				UserRef ref = (UserRef) item.getData();
				String text = excludeUser.getText().trim();
				if (text.isEmpty()) {
					return;
				}
				ObjectRef newRef = new ObjectRef(text);
				if (ref.getExcludeRefs().contains(newRef)) {
					EditorUtil.showMessage(dialog, getMessage(User_Category_Exclude_Unique_Message, text),
							Display.getCurrent().getCursorLocation());
					return;
				}
				ref.getExcludeRefs().add(newRef);
				item.setText(1, PSpecUtil.format(ref.getExcludeRefs(), Label_Separator));
			}
		});

		deleteExclude.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				UserRef ref = (UserRef) item.getData();
				String text = excludeUser.getText().trim();
				if (text.isEmpty()) {
					return;
				}
				ObjectRef newRef = new ObjectRef(text);
				if (!ref.getExcludeRefs().contains(newRef)) {
					EditorUtil.showMessage(dialog, getMessage(User_Category_Exclude_Not_Exist_Message, text),
							Display.getCurrent().getCursorLocation());
					return;
				}
				ref.getExcludeRefs().remove(newRef);
				item.setText(1, PSpecUtil.format(ref.getExcludeRefs(), Label_Separator));
			}
		});

	}

	private void addDataRow(DataRef ref) {
		List<TableEditor> editors = new ArrayList<>();
		final TableItem item = new TableItem(dataTable, SWT.NULL);
		item.setData(EditorUtil.Table_Editor, editors);
		item.setData(ref);

		final Combo dataCombo = EditorUtil.newCombo(dataTable, null);
		dataCombo.setItems(EditorUtil.getCategoryItems(policy.getDataContainer()));
		EditorUtil.setSelectedItem(dataCombo, ref.getRefid());
		editors.add(newTableEditor(dataTable, dataCombo, item, 0));

		final Combo actionCombo = EditorUtil.newCombo(dataTable, null);
		actionCombo.setItems(EditorUtil.getActionItems());
		actionCombo.setText(ref.getAction().getId());
		editors.add(newTableEditor(dataTable, actionCombo, item, 1));

		item.setText(2, PSpecUtil.format(ref.getExcludeRefs(), Label_Separator));

		final Combo excludeData = EditorUtil.newCombo(dataTable, null);
		editors.add(newTableEditor(dataTable, excludeData, item, 3));
		if (!ref.getRefid().isEmpty()) {
			DataCategory data = policy.getDataCategory(ref.getRefid());
			excludeData.setItems(EditorUtil.getChildCategoryItems(data, policy.getDataContainer()));
		}

		Button addExclude = EditorUtil.newButton(dataTable, "+");
		editors.add(newTableEditor(dataTable, addExclude, item, 4));

		Button deleteExclude = EditorUtil.newButton(dataTable, "-");
		editors.add(newTableEditor(dataTable, deleteExclude, item, 5));

		dataCombo.addSelectionListener(new SelectionAdapter() {

			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(SelectionEvent e) {
				DataRef ref = (DataRef) item.getData();
				int index = dataTable.indexOf(item);

				String text = dataCombo.getText().trim();
				if (!text.isEmpty()) {
					//check duplicate
					for (DataRef exist : model.dataRefs) {
						if (exist.getRefid().equals(text) && ref != exist) {
							EditorUtil.showMessage(dialog, getMessage(Rule_Data_Ref_Unique_Message, text),
									Display.getCurrent().getCursorLocation());
							EditorUtil.setSelectedItem(dataCombo, ref.getRefid());
							return;
						}
					}
				}

				DataCategory data = policy.getDataContainer().get(text);
				ref.setRefid(text);
				ref.getExcludeRefs().clear();
				excludeData.removeAll();
				excludeData.setItems(EditorUtil.getChildCategoryItems(data, policy.getDataContainer()));
				item.setText(2, PSpecUtil.format(ref.getExcludeRefs(), Label_Separator));

				if (dataSingleType.getSelection()) {
					//single
					if (!text.isEmpty()) {
						assert (restrictTables.size() == 1);
						Table table = restrictTables.get(0);
						TableItem resItem = table.getItem(0);
						List<TableEditor> editors = (List<TableEditor>) resItem
								.getData(EditorUtil.Table_Editor);

						//update desensitize operations
						Desensitization de = (Desensitization) resItem.getData();
						de.getOperations().retainAll(data.getAllOperations());
						resItem.setText(1, PSpecUtil.format(de.getOperations(), Label_Separator));

						//update desensitize operation items
						Combo operationCombo = (Combo) editors.get(0).getEditor();
						operationCombo.setItems(EditorUtil.getOperationItems(model.dataRefs,
								policy.getDataContainer()));
					}
				} else {
					for (Table table : restrictTables) {
						TableItem item = table.getItem(index);
						List<TableEditor> editors = (List<TableEditor>) item.getData(EditorUtil.Table_Editor);
						Desensitization de = (Desensitization) item.getData();

						//update data ref
						de.setDataRef(ref);
						item.setText(0, ref.getRefid());

						//update desensitize operations
						de.getOperations().clear();
						item.setText(1, PSpecUtil.format(de.getOperations(), Label_Separator));

						//update desensitize operation items
						Combo operationCombo = (Combo) editors.get(0).getEditor();
						operationCombo.setItems(EditorUtil.getOperationItems(ref, policy.getDataContainer()));
					}
				}
			}
		});

		actionCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DataRef ref = (DataRef) item.getData();
				String text = actionCombo.getText().trim();
				ref.setAction(Action.get(text));
			}
		});

		addExclude.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DataRef ref = (DataRef) item.getData();
				String text = excludeData.getText().trim();
				if (text.isEmpty()) {
					return;
				}
				ObjectRef newRef = new ObjectRef(text);
				if (ref.getExcludeRefs().contains(newRef)) {
					EditorUtil.showMessage(dialog, getMessage(Data_Category_Exclude_Unique_Message, text),
							Display.getCurrent().getCursorLocation());
					return;
				}
				ref.getExcludeRefs().add(newRef);
				item.setText(2, PSpecUtil.format(ref.getExcludeRefs(), Label_Separator));
			}
		});

		deleteExclude.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DataRef ref = (DataRef) item.getData();
				String text = excludeData.getText().trim();
				if (text.isEmpty()) {
					return;
				}
				ObjectRef newRef = new ObjectRef(text);
				if (!ref.getExcludeRefs().contains(newRef)) {
					EditorUtil.showMessage(dialog, getMessage(Data_Category_Exclude_Not_Exist_Message, text),
							Display.getCurrent().getCursorLocation());
					return;
				}
				ref.getExcludeRefs().remove(newRef);
				item.setText(2, PSpecUtil.format(ref.getExcludeRefs(), Label_Separator));
			}
		});

	}

	private void addRestrictTable(Composite parent, Restriction res) {

		Composite tableComposite = newTableComposite(parent);
		restrictTableComposites.add(tableComposite);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableComposite.setLayout(tableLayout);

		final Table restrictTable = newTable(tableComposite);
		restrictTable.setData(res);
		restrictTables.add(restrictTable);

		TableColumn[] columns = new TableColumn[5];
		String[] titles = new String[] { getMessage(Data_Category), getMessage(Desensitize_Operation),
				"", "", "" };
		for (int i = 0; i < columns.length; i++) {
			columns[i] = new TableColumn(restrictTable, SWT.NONE);
			columns[i].setText(titles[i]);
			columns[i].setResizable(false);
		}
		//add a new row
		if (dataSingleType.getSelection()) {
			//only one desensization
			Desensitization de = res.getDesensitizations().get(0);
			addDesensitizeRow(restrictTable, de);
		} else {
			for (int i = 0; i < dataTable.getItems().length; i++) {
				//process desensitization list
				Desensitization de = res.getDesensitizations().get(i);
				addDesensitizeRow(restrictTable, de);
			}
		}

		tableLayout.setColumnData(columns[0], new ColumnWeightData(1, columns[0].getWidth()));
		tableLayout.setColumnData(columns[1], new ColumnWeightData(2, columns[2].getWidth()));
		tableLayout.setColumnData(columns[2], new ColumnWeightData(1, columns[2].getWidth()));
		tableLayout.setColumnData(columns[3], new ColumnWeightData(0));
		tableLayout.setColumnData(columns[4], new ColumnWeightData(0));

		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (selectedRestrictTable != null) {
					selectedRestrictTable.deselectAll();
				}
				restrictTable.selectAll();
				if (dataAssociationType.getSelection()) {
					deleteRestriction.setEnabled(true);
				}
				selectedRestrictTable = restrictTable;
			}
		};
		restrictTable.addListener(SWT.Selection, listener);
		restrictTable.addListener(SWT.MouseDown, listener);

	}

	private void addDesensitizeRow(Table table, Desensitization de) {
		final TableItem item = new TableItem(table, SWT.NULL);
		List<TableEditor> editors = new ArrayList<>();
		item.setData(EditorUtil.Table_Editor, editors);
		item.setData(de);

		if (de.getDataRefId().isEmpty()) {
			item.setText(0, "*");
		} else {
			item.setText(0, de.getDataRefId());
		}

		item.setText(1, PSpecUtil.format(de.getOperations(), Label_Separator));

		final Combo operation = EditorUtil.newCombo(table, null);
		if (dataSingleType.getSelection()) {
			operation.setItems(EditorUtil.getOperationItems(model.dataRefs, policy.getDataContainer()));
		} else {
			operation.setItems(EditorUtil.getOperationItems(de.getDataRef(), policy.getDataContainer()));
		}

		editors.add(newTableEditor(table, operation, item, 2));

		Button addOperation = EditorUtil.newButton(table, "+");
		editors.add(newTableEditor(table, addOperation, item, 3));

		Button deleteOperation = EditorUtil.newButton(table, "-");
		editors.add(newTableEditor(table, deleteOperation, item, 4));

		addOperation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Desensitization de = (Desensitization) item.getData();
				String text = operation.getText().trim();
				if (text.isEmpty()) {
					return;
				}
				DesensitizeOperation op = DesensitizeOperation.get(text);
				if (de.getOperations().contains(op)) {
					EditorUtil.showMessage(dialog,
							getMessage(Rule_Desensitize_Operation_Unique_Message, text), Display.getCurrent()
									.getCursorLocation());
					return;
				}
				de.getOperations().add(op);
				item.setText(1, PSpecUtil.format(de.getOperations(), Label_Separator));
			}
		});

		deleteOperation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Desensitization de = (Desensitization) item.getData();
				String text = operation.getText().trim();
				if (text.isEmpty()) {
					return;
				}
				DesensitizeOperation op = DesensitizeOperation.get(text);
				if (!de.getOperations().contains(op)) {
					EditorUtil.showMessage(dialog,
							getMessage(Rule_Desensitize_Operation_Not_Exist_Message, text), Display.getCurrent()
									.getCursorLocation());
					return;
				}
				de.getOperations().remove(op);
				item.setText(1, PSpecUtil.format(de.getOperations(), Label_Separator));
			}
		});
	}

	private Composite newComposite(Composite parent) {
		Composite composite = EditorUtil.newComposite(parent);
		GridLayout layout = EditorUtil.newNoMarginGridLayout(3, false);
		composite.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		composite.setLayoutData(data);
		return composite;
	}

	private Label newDummyLabel(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		label.setLayoutData(data);
		return label;
	}

	private Composite newTableComposite(Composite parent) {
		Composite composite = EditorUtil.newComposite(parent);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		tableData.heightHint = (int) (dialog.getSize().y * tableHeightRatio);
		composite.setLayoutData(tableData);
		return composite;

	}

	private Table newTable(Composite parent) {
		return newTable(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI
				| SWT.FULL_SELECTION);
	}

	private Table newTable(Composite parent, int style) {
		Table table = new Table(parent, style);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		return table;
	}

	private Composite newRadioComposite(Composite parent) {
		Composite composite = EditorUtil.newComposite(parent);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		composite.setLayoutData(data);
		GridLayout layout = EditorUtil.newNoMarginGridLayout(2, false);
		composite.setLayout(layout);
		return composite;
	}

	private TableEditor newTableEditor(Table table, Control control, TableItem item, int column) {
		TableEditor editor = new TableEditor(table);
		editor.grabHorizontal = true;
		editor.setEditor(control, item, column);
		return editor;
	}

	private Restriction newRestriction() {
		Restriction res = new Restriction();
		if (dataSingleType.getSelection()) {
			res.getDesensitizations().add(new Desensitization());
		} else {
			for (DataRef ref : model.dataRefs) {
				res.getDesensitizations().add(new Desensitization(ref));
			}
		}
		return res;
	}

	private void hideRestrictions() {
		exclude(addRestriction);
		exclude(deleteRestriction);
		exclude(restrictComposite);
		adjustEffectLayout();
	}

	private void showRestrictions() {
		include(addRestriction);
		include(deleteRestriction);
		include(restrictComposite);
		adjustEffectLayout();
	}

	private void exclude(Control composite) {
		composite.setVisible(false);
		GridData data = (GridData) composite.getLayoutData();
		if (data == null) {
			data = new GridData();
			composite.setLayoutData(data);
		}
		data.exclude = true;
	}

	private void include(Control composite) {
		composite.setVisible(true);
		GridData data = (GridData) composite.getLayoutData();
		if (data == null) {
			data = new GridData();
			composite.setLayoutData(data);
		}
		data.exclude = false;
	}

	private void resize(Composite composite) {
		GridData dataData = (GridData) composite.getLayoutData();
		dataData.heightHint = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
	}

	private void disposeRestrictTables() {
		for (int i = 0; i < restrictTables.size(); i++) {
			Table table = restrictTables.get(i);
			for (TableItem item : table.getItems()) {
				EditorUtil.dispose(item);
			}
			table.dispose();
			restrictTableComposites.get(i).dispose();
		}

		restrictTables.clear();
		restrictTableComposites.clear();
	}

	private void adjustEffectLayout() {
		resize(effectComposite);
		effectComposite.pack();
		scrollContent.pack();
		layoutScroll();
	}

}