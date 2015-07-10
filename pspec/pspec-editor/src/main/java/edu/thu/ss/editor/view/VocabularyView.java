package edu.thu.ss.editor.view;

import static edu.thu.ss.editor.util.MessagesUtil.*;

import java.net.URI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.TreeItem;

import edu.thu.ss.editor.util.EditorUtil;
import edu.thu.ss.spec.global.CategoryManager;
import edu.thu.ss.spec.lang.parser.VocabularyParser;
import edu.thu.ss.spec.lang.pojo.ContactInfo;
import edu.thu.ss.spec.lang.pojo.DataContainer;
import edu.thu.ss.spec.lang.pojo.UserContainer;
import edu.thu.ss.spec.lang.pojo.Vocabulary;
import edu.thu.ss.spec.util.XMLUtil;

public class VocabularyView extends Composite {

	private Shell shell;
	private Text name;
	private Text email;
	private Text country;
	private Text vocabularyID;
	private Text baseVocabulary;
	private Text location;
	private Text organization;
	private Text address;
	private Text longDescription;
	private Text shortDescription;
	private Vocabulary vocabulary;
	private TreeItem item;

	/**
	 * Create the composite
	 * @param parent
	 * @param style
	 */
	public VocabularyView(final Shell shell, Composite parent, int style, Vocabulary vocabulary,
			TreeItem item) {
		super(parent, style);
		this.shell = shell;
		this.item = item;
		this.vocabulary = vocabulary;

		this.setBackground(EditorUtil.getDefaultBackground());
		this.setBackgroundMode(SWT.INHERIT_FORCE);

		this.setLayout(new FillLayout());

		Group content = EditorUtil.newGroup(this, getMessage(Vocabulary_Info));
		content.setLayout(new GridLayout(1, false));

		initializeContent(content);
	}

	private void initializeContent(Composite parent) {
		Group basicGroup = EditorUtil.newInnerGroup(parent, getMessage(Basic_Info));
		((GridData) basicGroup.getLayoutData()).grabExcessVerticalSpace = true;
		((GridData) basicGroup.getLayoutData()).verticalAlignment = SWT.FILL;
		initializeBasic(basicGroup);

		Group issuerGroup = EditorUtil.newInnerGroup(parent, getMessage(Issuer));
		initializeIssuer(issuerGroup);

	}

	private void initializeBasic(Composite parent) {
		EditorUtil.newLabel(parent, getMessage(Vocabulary_ID), EditorUtil.labelData());
		vocabularyID = EditorUtil.newText(parent, EditorUtil.textData());
		vocabularyID.setText(vocabulary.getInfo().getId());
		vocabularyID.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				String text = vocabularyID.getText().trim();
				if (text.isEmpty()) {
					EditorUtil.showMessage(shell, getMessage(Vocabulary_ID_Empty_Message), vocabularyID);

					vocabularyID.setText(vocabulary.getInfo().getId());
					vocabularyID.selectAll();
				} else {
					vocabulary.getInfo().setId(text);
					item.setText(text);
				}

			}
		});

		EditorUtil.newLabel(parent, getMessage(Base_Vocabualry), EditorUtil.labelData());

		Composite baseComposite = EditorUtil.newComposite(parent);
		baseComposite.setLayout(EditorUtil.newNoMarginGridLayout(2, false));
		baseComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		baseVocabulary = EditorUtil.newText(baseComposite, EditorUtil.textData());
		if (vocabulary.getBase() != null) {
			baseVocabulary.setText(vocabulary.getBase().toString());
		} else {
			baseVocabulary.setText("...");
		}
		baseVocabulary.setEnabled(false);
		GridData baseData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		baseVocabulary.setLayoutData(baseData);

		Button open = EditorUtil.newButton(baseComposite, getMessage(Open));
		open.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dlg = EditorUtil.newFileDialog(shell);
				String file = dlg.open();
				if (file != null) {
					baseVocabulary.setText(file);
					VocabularyParser parser = new VocabularyParser();
					try {
						parser.parse(file);
						Vocabulary base = CategoryManager.getVocab(XMLUtil.toUri(file));
						UserContainer baseUser = base.getUserContainer();
						DataContainer baseData = base.getDataContainer();

						vocabulary.setBase(XMLUtil.toUri(file));
						vocabulary.getUserContainer().setBaseContainer(baseUser);
						vocabulary.getDataContainer().setBaseContainer(baseData);
					} catch (Exception e1) {
						e1.printStackTrace();
					}

				}
			}
		});

		vocabularyID.setText(vocabulary.getInfo().getId());
		vocabularyID.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				//TODO: verify
				vocabulary.getInfo().setId(vocabularyID.getText().trim());
				item.setText(vocabularyID.getText().trim());
			}
		});

		EditorUtil.newLabel(parent, getMessage(Short_Description), EditorUtil.labelData());
		shortDescription = EditorUtil.newText(parent, EditorUtil.textData());
		shortDescription.setText(vocabulary.getInfo().getShortDescription());
		shortDescription.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				vocabulary.getInfo().setShortDescription(shortDescription.getText().trim());
			}
		});

		EditorUtil.newLabel(parent, getMessage(Long_Description), EditorUtil.labelData());
		longDescription = new Text(parent, SWT.BORDER | SWT.V_SCROLL);
		longDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		longDescription.setText(vocabulary.getInfo().getLongDescription());

		longDescription.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				vocabulary.getInfo().setLongDescription(longDescription.getText().trim());
			}
		});

	}

	private void initializeIssuer(Composite parent) {
		final ContactInfo contact = vocabulary.getInfo().getContact();

		EditorUtil.newLabel(parent, getMessage(Issuer_Name), EditorUtil.labelData());
		name = EditorUtil.newText(parent, EditorUtil.textData());
		name.setText(contact.getName());
		name.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				contact.setName(name.getText().trim());
			}
		});

		EditorUtil.newLabel(parent, getMessage(Issuer_Email), EditorUtil.labelData());
		email = EditorUtil.newText(parent, EditorUtil.textData());
		email.setText(contact.getEmail());
		email.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				contact.setEmail(email.getText().trim());
			}
		});

		EditorUtil.newLabel(parent, getMessage(Issuer_Organization), EditorUtil.labelData());
		organization = EditorUtil.newText(parent, EditorUtil.textData());
		organization.setText(contact.getOrganization());
		organization.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				contact.setOrganization(organization.getText().trim());
			}
		});

		EditorUtil.newLabel(parent, getMessage(Issuer_Address), EditorUtil.labelData());
		address = EditorUtil.newText(parent, EditorUtil.textData());
		address.setText(contact.getAddress());
		address.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				contact.setAddress(address.getText().trim());
			}
		});

		EditorUtil.newLabel(parent, getMessage(Issuer_Country), EditorUtil.labelData());
		country = EditorUtil.newText(parent, EditorUtil.textData());
		country.setText(contact.getCountry());
		country.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				contact.setCountry(country.getText().trim());
			}
		});
	}
}