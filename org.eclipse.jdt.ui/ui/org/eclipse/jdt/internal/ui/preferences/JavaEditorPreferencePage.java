/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.WorkbenchChainedTextFontFieldEditor;

import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;

/*
 * The page for setting the editor options.
 */
public class JavaEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
			
	
	public final OverlayPreferenceStore.OverlayKey[] fKeys= new OverlayPreferenceStore.OverlayKey[] {
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_MULTI_LINE_COMMENT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_MULTI_LINE_COMMENT + "_bold"),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT + "_bold"),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_KEYWORD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_KEYWORD + "_bold"),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_TYPE),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_TYPE + "_bold"),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_STRING),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_STRING + "_bold"),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVA_DEFAULT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVA_DEFAULT + "_bold"),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVADOC_KEYWORD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVADOC_KEYWORD + "_bold"),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVADOC_TAG),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVADOC_TAG + "_bold"),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVADOC_LINK),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVADOC_LINK + "_bold"),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, IJavaColorConstants.JAVADOC_DEFAULT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, IJavaColorConstants.JAVADOC_DEFAULT + "_bold"),
				
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CompilationUnitEditor.MATCHING_BRACKETS_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, CompilationUnitEditor.MATCHING_BRACKETS),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.AUTOACTIVATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, ContentAssistPreference.AUTOACTIVATION_DELAY),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.AUTOINSERT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.PROPOSALS_BACKGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.PROPOSALS_FOREGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.PARAMETERS_BACKGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.PARAMETERS_FOREGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVA),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVADOC),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.SHOW_VISIBLE_PROPOSALS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.ORDER_PROPOSALS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, ContentAssistPreference.CASE_SENSITIVITY)
		
	};
	
	private final String[][] fListModel= new String[][] {
		{ "Multi-line comment", IJavaColorConstants.JAVA_MULTI_LINE_COMMENT },
		{ "Single-line comment", IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT },
		{ "Keywords", IJavaColorConstants.JAVA_KEYWORD },
		{ "Built-in types", IJavaColorConstants.JAVA_TYPE },
		{ "Strings", IJavaColorConstants.JAVA_STRING },
		{ "Others", IJavaColorConstants.JAVA_DEFAULT },
		{ "JavaDoc keywords", IJavaColorConstants.JAVADOC_KEYWORD },
		{ "JavaDoc HTML tags", IJavaColorConstants.JAVADOC_TAG },
		{ "JavaDoc links", IJavaColorConstants.JAVADOC_LINK },
		{"JavaDoc others", IJavaColorConstants.JAVADOC_DEFAULT }
	};
	
	private OverlayPreferenceStore fOverlayStore;
	private JavaTextTools fJavaTextTools;
	
	private Map fColorButtons= new HashMap();
	private SelectionListener fColorButtonListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			ColorEditor editor= (ColorEditor) e.widget.getData();
			PreferenceConverter.setValue(fOverlayStore, (String) fColorButtons.get(editor), editor.getColorValue());
		}
	};
	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	private Map fTextFields= new HashMap();
	private ModifyListener fTextFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			Text text= (Text) e.widget;
			fOverlayStore.setValue((String) fTextFields.get(text), text.getText());
		}
	};
	
	private WorkbenchChainedTextFontFieldEditor fFontEditor;
	private List fList;
	private ColorEditor fColorEditor;
	private Button fBoldCheckBox;
	private SourceViewer fPreviewViewer;	
	
	public JavaEditorPreferencePage() {
		setDescription(JavaUIMessages.getString("JavaEditorPreferencePage.description"));
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		fOverlayStore= new OverlayPreferenceStore(getPreferenceStore(), fKeys);
	}
	
	public static void initDefaults(IPreferenceStore store) {
		
		Color color;
		Display display= Display.getDefault();
				
		store.setDefault(CompilationUnitEditor.MATCHING_BRACKETS, true);
		color= display.getSystemColor(SWT.COLOR_BLUE);
		PreferenceConverter.setDefault(store, CompilationUnitEditor.MATCHING_BRACKETS_COLOR,  color.getRGB());
		
		WorkbenchChainedTextFontFieldEditor.startPropagate(store, JFaceResources.TEXT_FONT);

		color= display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		PreferenceConverter.setDefault(store,  AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND, color.getRGB());
		
		color= display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		PreferenceConverter.setDefault(store,  AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, color.getRGB());		
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_MULTI_LINE_COMMENT, new RGB(63, 127, 95));
		store.setDefault(IJavaColorConstants.JAVA_MULTI_LINE_COMMENT + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT, new RGB(63, 127, 95));
		store.setDefault(IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_KEYWORD, new RGB(127, 0, 85));
		store.setDefault(IJavaColorConstants.JAVA_KEYWORD + "_bold", true);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_TYPE, new RGB(127, 0, 85));
		store.setDefault(IJavaColorConstants.JAVA_TYPE + "_bold", true);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_STRING, new RGB(42, 0, 255));
		store.setDefault(IJavaColorConstants.JAVA_STRING + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_DEFAULT, new RGB(0, 0, 0));
		store.setDefault(IJavaColorConstants.JAVA_DEFAULT + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_KEYWORD, new RGB(127, 159, 191));
		store.setDefault(IJavaColorConstants.JAVADOC_KEYWORD + "_bold", true);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_TAG, new RGB(127, 127, 159));
		store.setDefault(IJavaColorConstants.JAVADOC_TAG + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_LINK, new RGB(63, 63, 191));
		store.setDefault(IJavaColorConstants.JAVADOC_LINK + "_bold", false);
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_DEFAULT, new RGB(63, 95, 191));
		store.setDefault(IJavaColorConstants.JAVADOC_DEFAULT + "_bold", false);		
		
		store.setDefault(ContentAssistPreference.AUTOACTIVATION, true);
		store.setDefault(ContentAssistPreference.AUTOACTIVATION_DELAY, 500);
		
		store.setDefault(ContentAssistPreference.AUTOINSERT, false);
		PreferenceConverter.setDefault(store, ContentAssistPreference.PROPOSALS_BACKGROUND, new RGB(254, 241, 233));
		PreferenceConverter.setDefault(store, ContentAssistPreference.PROPOSALS_FOREGROUND, new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, ContentAssistPreference.PARAMETERS_BACKGROUND, new RGB(254, 241, 233));
		PreferenceConverter.setDefault(store, ContentAssistPreference.PARAMETERS_FOREGROUND, new RGB(0, 0, 0));
		store.setDefault(ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVA, ".,");
		store.setDefault(ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVADOC, "@");
	}

	/*
	 * @see IWorkbenchPreferencePage#init()
	 */	
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE));
	}

	private void handleListSelection() {	
		int i= fList.getSelectionIndex();
		String key= fListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fColorEditor.setColorValue(rgb);
		fBoldCheckBox.setSelection(fOverlayStore.getBoolean(key + "_bold"));
	}
	
	private Control  createColorPage(Composite parent) {
		
		Composite colorComposite= new Composite(parent, SWT.NULL);
		colorComposite.setLayout(new GridLayout());

		Label label= new Label(colorComposite, SWT.LEFT);
		label.setText("Colors");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite editorComposite= new Composite(colorComposite, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		editorComposite.setLayoutData(gd);
		
		fList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL);
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fList.setLayoutData(gd);
		
		Composite stylesComposite= new Composite(editorComposite, SWT.NULL);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText("Foreground:");
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);

		fColorEditor= new ColorEditor(stylesComposite);
		Button colorButton= fColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		colorButton.setLayoutData(gd);
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText("Bold:");
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);
		
		fBoldCheckBox= new Button(stylesComposite, SWT.CHECK);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		fBoldCheckBox.setLayoutData(gd);
		
		label= new Label(colorComposite, SWT.LEFT);
		label.setText("Preview");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control previewer= createPreviewer(colorComposite);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(80);
		gd.heightHint= convertHeightInCharsToPixels(15);
		previewer.setLayoutData(gd);

		
		fList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleListSelection();
			}
		});
		
		colorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fList.getSelectionIndex();
				String key= fListModel[i][1];
				PreferenceConverter.setValue(fOverlayStore, key, fColorEditor.getColorValue());
			}
		});
		
		fBoldCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fList.getSelectionIndex();
				String key= fListModel[i][1];
				fOverlayStore.setValue(key + "_bold", fBoldCheckBox.getSelection());
			}
		});
				
		return colorComposite;
	}
	
	private Control createPreviewer(Composite parent) {
		
		fJavaTextTools= new JavaTextTools(fOverlayStore);
		
		fPreviewViewer= new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL);
		fPreviewViewer.configure(new JavaSourceViewerConfiguration(fJavaTextTools, null));
		fPreviewViewer.getTextWidget().setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		fPreviewViewer.setEditable(false);
		
		String content= loadPreviewContentFromFile("ColorSettingPreviewCode.txt");
		IDocument document= new Document(content);
		IDocumentPartitioner partitioner= fJavaTextTools.createDocumentPartitioner();
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		
		fPreviewViewer.setDocument(document);
		
		fOverlayStore.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				fPreviewViewer.invalidateTextPresentation();
			}
		});
		
		return fPreviewViewer.getControl();
	}
	
	private Control createBehaviorPage(Composite parent) {

		Composite behaviorComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		behaviorComposite.setLayout(layout);
				
		String label= "Highlight matching brackets";
		addCheckBox(behaviorComposite, label, CompilationUnitEditor.MATCHING_BRACKETS, 0);
		
		label= "Matching brackets highlight color:";
		addColorButton(behaviorComposite, label, CompilationUnitEditor.MATCHING_BRACKETS_COLOR, 0);
		
		label= "Text font:";
		addTextFontEditor(behaviorComposite, label, AbstractTextEditor.PREFERENCE_FONT);
		
		return behaviorComposite;
	}
	
	private Control createContentAssistPage(Composite parent) {

		Composite contentAssistComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		contentAssistComposite.setLayout(layout);
				
		String label= "Insert &single proposals automatically";
		addCheckBox(contentAssistComposite, label, ContentAssistPreference.AUTOINSERT, 0);
		
		label= "Show only proposals visible in the invocation context";
		addCheckBox(contentAssistComposite, label, ContentAssistPreference.SHOW_VISIBLE_PROPOSALS, 0);
		
//		label= "Show only proposals with matching cases";
//		addCheckBox(contentAssistComposite, label, ContentAssistPreference.CASE_SENSITIVITY, 0);
		
		label= "Present proposals in alphabetical order";
		addCheckBox(contentAssistComposite, label, ContentAssistPreference.ORDER_PROPOSALS, 0);
		
		label= "&Enable auto activation";
		addCheckBox(contentAssistComposite, label, ContentAssistPreference.AUTOACTIVATION, 0);
		
		label= "Auto activation &delay:";
		addTextField(contentAssistComposite, label, ContentAssistPreference.AUTOACTIVATION_DELAY, 4, 0);
		
		label= "Auto activation &triggers for Java:";
		addTextField(contentAssistComposite, label, ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVA, 25, 0);
		
		label= "Auto activation tri&ggers for JavaDoc:";
		addTextField(contentAssistComposite, label, ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVADOC, 25, 0);
				
		label= "&Background for completion proposals:";
		addColorButton(contentAssistComposite, label, ContentAssistPreference.PROPOSALS_BACKGROUND, 0);
		
		label= "&Foreground for completion proposals:";
		addColorButton(contentAssistComposite, label, ContentAssistPreference.PROPOSALS_FOREGROUND, 0);
		
		label= "B&ackground for method parameters:";
		addColorButton(contentAssistComposite, label, ContentAssistPreference.PARAMETERS_BACKGROUND, 0);
		
		label= "F&oreground for method parameters:";
		addColorButton(contentAssistComposite, label, ContentAssistPreference.PARAMETERS_FOREGROUND, 0);
				
		return contentAssistComposite;
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		fOverlayStore.load();
		fOverlayStore.start();
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText("&General");
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(createBehaviorPage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText("&Colors");
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(createColorPage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText("Code &Assist");
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(createContentAssistPage(folder));
		
		initialize();
		
		return folder;
	}
	
	private void initialize() {
		
		fFontEditor.setPreferenceStore(getPreferenceStore());
		fFontEditor.setPreferencePage(this);
		fFontEditor.load();
		
		initializeFields();
		
		for (int i= 0; i < fListModel.length; i++)
			fList.add(fListModel[i][0]);
			
		fList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				fList.select(0);
				handleListSelection();
			}
		});
	}
	
	private void initializeFields() {
		
		Iterator e= fColorButtons.keySet().iterator();
		while (e.hasNext()) {
			ColorEditor c= (ColorEditor) e.next();
			String key= (String) fColorButtons.get(c);
			RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
			c.setColorValue(rgb);
		}
		
		e= fCheckBoxes.keySet().iterator();
		while (e.hasNext()) {
			Button b= (Button) e.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fOverlayStore.getBoolean(key));
		}
		
		e= fTextFields.keySet().iterator();
		while (e.hasNext()) {
			Text t= (Text) e.next();
			String key= (String) fTextFields.get(t);
			t.setText(fOverlayStore.getString(key));
		}
	}
	
	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		fFontEditor.store();
		fOverlayStore.propagate();
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		
		fFontEditor.loadDefault();
		
		fOverlayStore.loadDefaults();
		initializeFields();
		handleListSelection();
		
		super.performDefaults();
		
		fPreviewViewer.invalidateTextPresentation();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		
		if (fJavaTextTools != null) {
			fJavaTextTools= null;
		}
		
		fFontEditor.setPreferencePage(null);
		fFontEditor.setPreferenceStore(null);
		
		if (fOverlayStore != null) {
			fOverlayStore.stop();
			fOverlayStore= null;
		}
		
		super.dispose();
	}
	
	private void addColorButton(Composite parent, String label, String key, int indentation) {
				
		Label labelControl= new Label(parent, SWT.NONE);
		labelControl.setText(label);
		
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		ColorEditor editor= new ColorEditor(parent);
		Button button= editor.getButton();
		button.setData(editor);
		
		gd= new GridData();
		gd.horizontalAlignment= GridData.END;
		button.setLayoutData(gd);
		button.addSelectionListener(fColorButtonListener);
		
		fColorButtons.put(editor, key);
	}
	
	private void addCheckBox(Composite parent, String label, String key, int indentation) {		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 2;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);
		
		fCheckBoxes.put(checkBox, key);
	}
	
	private void addTextField(Composite parent, String label, String key, int textLimit, int indentation) {
		Label labelControl= new Label(parent, SWT.NONE);
		labelControl.setText(label);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		Text textControl= new Text(parent, SWT.BORDER | SWT.SINGLE);		
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(textLimit + 1);
		gd.horizontalAlignment= GridData.END;
		textControl.setLayoutData(gd);
		
		textControl.setTextLimit(textLimit);
		textControl.addModifyListener(fTextFieldListener);
		
		fTextFields.put(textControl, key);
	}
	
	private void addTextFontEditor(Composite parent, String label, String key) {
		
		Composite editorComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		editorComposite.setLayout(layout);		
		fFontEditor= new WorkbenchChainedTextFontFieldEditor(key, label, editorComposite);
				
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);
	}
	
	private String loadPreviewContentFromFile(String filename) {
		String line;
		String separator= System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer= new StringBuffer(512);
		BufferedReader reader= null;
		try {
			reader= new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
			while ((line= reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(separator);
			}
		} catch (IOException io) {
			JavaPlugin.log(io);
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException e) {}
			}
		}
		return buffer.toString();
	}
}


