/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.compiler.env.IConstants;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.IImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.preferences.CodeGenerationPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.SuperInterfaceSelectionDialog;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonStatusDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * <code>NewTypeWizardPage</code> contains controls and validation routines for a 'New Type WizardPage'
 * Implementors decide which components to add and to enable. Implementors can also
 * customize the validation code.
 * <code>NewTypeWizardPage</code> is intended to serve as base class of all wizards that create types.
 * Applets, Servlets, Classes, Interfaces...
 * See <code>NewClassWizardPage</code> or <code>NewInterfaceWizardPage</code> for an
 * example usage of NewTypeWizardPage.
 * @since 2.0
 */
public abstract class NewTypeWizardPage extends NewContainerWizardPage {

	/**
	 * Class used in stub creation routines to add imports needed in the created stubs.
	 */
	public static class ImportsManager {

		private IImportsStructure fImportsStructure;

		/**
		 * For internal use only. Package visible.
		 */
		ImportsManager(IImportsStructure structure) {
			fImportsStructure= structure;
		}

		/**
		 * For internal use only. Package visible.
		 */	
		IImportsStructure getImportsStructure() {
			return fImportsStructure;
		}
				
		/**
		 * Adds a new import declaration that is sorted in the existing imports.
		 * If an import already exists or the import would conflict with another import
		 * of an other type with the same simple name  the import is not added.
		 * @param qualifiedTypeName The fully qualified name of the type to import
		 *        (dot separated)
		 * @return Retuns the simple type name that can be used in the code or the
		 * fully qualified type name if an import conflict prevented the import.
		 */				
		public String addImport(String qualifiedTypeName) {
			return fImportsStructure.addImport(qualifiedTypeName);
		}


	}
	
	/**
	 * Flags used in setModifiers and getModifiers (compatible with
	 * org.eclipse.jdt.core.Flags)
	 */
	public int F_PUBLIC = IConstants.AccPublic;
	public int F_PRIVATE = IConstants.AccPrivate;
	public int F_PROTECTED = IConstants.AccProtected;
	public int F_STATIC = IConstants.AccStatic;
	public int F_FINAL = IConstants.AccFinal;
	public int F_ABSTRACT = IConstants.AccAbstract;

	
	private final static String PAGE_NAME= "NewTypeWizardPage"; //$NON-NLS-1$
	
	protected final static String PACKAGE= PAGE_NAME + ".package";	 //$NON-NLS-1$
	protected final static String ENCLOSING= PAGE_NAME + ".enclosing"; //$NON-NLS-1$
	protected final static String ENCLOSINGSELECTION= ENCLOSING + ".selection"; //$NON-NLS-1$
	
	protected final static String TYPENAME= PAGE_NAME + ".typename"; //$NON-NLS-1$
	protected final static String SUPER= PAGE_NAME + ".superclass"; //$NON-NLS-1$
	protected final static String INTERFACES= PAGE_NAME + ".interfaces"; //$NON-NLS-1$
	protected final static String MODIFIERS= PAGE_NAME + ".modifiers"; //$NON-NLS-1$
	protected final static String METHODS= PAGE_NAME + ".methods"; //$NON-NLS-1$

	private class InterfacesListLabelProvider extends LabelProvider {
		
		private Image fInterfaceImage;
		
		public InterfacesListLabelProvider() {
			super();
			fInterfaceImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE);
		}
		
		public Image getImage(Object element) {
			return fInterfaceImage;
		}
	}	

	private StringButtonStatusDialogField fPackageDialogField;
	
	private SelectionButtonDialogField fEnclosingTypeSelection;
	private StringButtonDialogField fEnclosingTypeDialogField;
		
	private boolean fCanModifyPackage;
	private boolean fCanModifyEnclosingType;
	
	private IPackageFragment fCurrPackage;
	
	private IType fCurrEnclosingType;	
	private StringDialogField fTypeNameDialogField;
	
	private StringButtonDialogField fSuperClassDialogField;
	private ListDialogField fSuperInterfacesDialogField;
	
	private IType fSuperClass;
	
	private SelectionButtonDialogFieldGroup fAccMdfButtons;
	private SelectionButtonDialogFieldGroup fOtherMdfButtons;
	
	private IType fCreatedType;
	
	protected IStatus fEnclosingTypeStatus;
	protected IStatus fPackageStatus;
	protected IStatus fTypeNameStatus;
	protected IStatus fSuperClassStatus;
	protected IStatus fModifierStatus;
	protected IStatus fSuperInterfacesStatus;	
	
	private boolean fIsClass;
	private int fStaticMdfIndex;
	
	private final int PUBLIC_INDEX= 0, DEFAULT_INDEX= 1, PRIVATE_INDEX= 2, PROTECTED_INDEX= 3;
	private final int ABSTRACT_INDEX= 0, FINAL_INDEX= 1;

	public NewTypeWizardPage(boolean isClass, String pageName) {
		super(pageName);
		fCreatedType= null;
		
		fIsClass= isClass;
		
		TypeFieldsAdapter adapter= new TypeFieldsAdapter();
		
		fPackageDialogField= new StringButtonStatusDialogField(adapter);
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(NewWizardMessages.getString("NewTypeWizardPage.package.label")); //$NON-NLS-1$
		fPackageDialogField.setButtonLabel(NewWizardMessages.getString("NewTypeWizardPage.package.button")); //$NON-NLS-1$
		fPackageDialogField.setStatusWidthHint(NewWizardMessages.getString("NewTypeWizardPage.default")); //$NON-NLS-1$
				
		fEnclosingTypeSelection= new SelectionButtonDialogField(SWT.CHECK);
		fEnclosingTypeSelection.setDialogFieldListener(adapter);
		fEnclosingTypeSelection.setLabelText(NewWizardMessages.getString("NewTypeWizardPage.enclosing.selection.label")); //$NON-NLS-1$
		
		fEnclosingTypeDialogField= new StringButtonDialogField(adapter);
		fEnclosingTypeDialogField.setDialogFieldListener(adapter);
		fEnclosingTypeDialogField.setButtonLabel(NewWizardMessages.getString("NewTypeWizardPage.enclosing.button")); //$NON-NLS-1$
		
		fTypeNameDialogField= new StringDialogField();
		fTypeNameDialogField.setDialogFieldListener(adapter);
		fTypeNameDialogField.setLabelText(NewWizardMessages.getString("NewTypeWizardPage.typename.label")); //$NON-NLS-1$
		
		fSuperClassDialogField= new StringButtonDialogField(adapter);
		fSuperClassDialogField.setDialogFieldListener(adapter);
		fSuperClassDialogField.setLabelText(NewWizardMessages.getString("NewTypeWizardPage.superclass.label")); //$NON-NLS-1$
		fSuperClassDialogField.setButtonLabel(NewWizardMessages.getString("NewTypeWizardPage.superclass.button")); //$NON-NLS-1$
		
		String[] addButtons= new String[] {
			/* 0 */ NewWizardMessages.getString("NewTypeWizardPage.interfaces.add"), //$NON-NLS-1$
			/* 1 */ null,
			/* 2 */ NewWizardMessages.getString("NewTypeWizardPage.interfaces.remove") //$NON-NLS-1$
		}; 
		fSuperInterfacesDialogField= new ListDialogField(adapter, addButtons, new InterfacesListLabelProvider());
		fSuperInterfacesDialogField.setDialogFieldListener(adapter);
		String interfaceLabel= fIsClass ? NewWizardMessages.getString("NewTypeWizardPage.interfaces.class.label") : NewWizardMessages.getString("NewTypeWizardPage.interfaces.ifc.label"); //$NON-NLS-1$ //$NON-NLS-2$
		fSuperInterfacesDialogField.setLabelText(interfaceLabel);
		fSuperInterfacesDialogField.setRemoveButtonIndex(2);
	
		String[] buttonNames1= new String[] {
			/* 0 == PUBLIC_INDEX */ NewWizardMessages.getString("NewTypeWizardPage.modifiers.public"), //$NON-NLS-1$
			/* 1 == DEFAULT_INDEX */ NewWizardMessages.getString("NewTypeWizardPage.modifiers.default"), //$NON-NLS-1$
			/* 2 == PRIVATE_INDEX */ NewWizardMessages.getString("NewTypeWizardPage.modifiers.private"), //$NON-NLS-1$
			/* 3 == PROTECTED_INDEX*/ NewWizardMessages.getString("NewTypeWizardPage.modifiers.protected") //$NON-NLS-1$
		};
		fAccMdfButtons= new SelectionButtonDialogFieldGroup(SWT.RADIO, buttonNames1, 4);
		fAccMdfButtons.setDialogFieldListener(adapter);
		fAccMdfButtons.setLabelText(NewWizardMessages.getString("NewTypeWizardPage.modifiers.acc.label"));		 //$NON-NLS-1$
		fAccMdfButtons.setSelection(0, true);
		
		String[] buttonNames2;
		if (fIsClass) {
			buttonNames2= new String[] {
				/* 0 == ABSTRACT_INDEX */ NewWizardMessages.getString("NewTypeWizardPage.modifiers.abstract"), //$NON-NLS-1$
				/* 1 == FINAL_INDEX */ NewWizardMessages.getString("NewTypeWizardPage.modifiers.final"), //$NON-NLS-1$
				/* 2 */ NewWizardMessages.getString("NewTypeWizardPage.modifiers.static") //$NON-NLS-1$
			};
			fStaticMdfIndex= 2; // index of the static checkbox is 2
		} else {
			buttonNames2= new String[] {
				NewWizardMessages.getString("NewTypeWizardPage.modifiers.static") //$NON-NLS-1$
			};
			fStaticMdfIndex= 0; // index of the static checkbox is 0
		}

		fOtherMdfButtons= new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames2, 4);
		fOtherMdfButtons.setDialogFieldListener(adapter);
		
		fAccMdfButtons.enableSelectionButton(PRIVATE_INDEX, false);
		fAccMdfButtons.enableSelectionButton(PROTECTED_INDEX, false);
		fOtherMdfButtons.enableSelectionButton(fStaticMdfIndex, false);

		fPackageStatus= new StatusInfo();
		fEnclosingTypeStatus= new StatusInfo();
		
		fCanModifyPackage= true;
		fCanModifyEnclosingType= true;
		updateEnableState();
					
		fTypeNameStatus= new StatusInfo();
		fSuperClassStatus= new StatusInfo();
		fSuperInterfacesStatus= new StatusInfo();
		fModifierStatus= new StatusInfo();
	}
	
	/**
	 * Initializes all fields provided by the type page with a given
	 * Java element as selection. To implement a different selection strategy do not call this
	 * method or overwrite it.
	 * @param elem The initial selection of this page or null if no
	 *             selection was available
	 */
	protected void initTypePage(IJavaElement elem) {
		String initSuperclass= "java.lang.Object"; //$NON-NLS-1$
		ArrayList initSuperinterfaces= new ArrayList(5);

		IPackageFragment pack= null;
		IType enclosingType= null;
				
		if (elem != null) {
			// evaluate the enclosing type
			pack= (IPackageFragment) elem.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			IType typeInCU= (IType) elem.getAncestor(IJavaElement.TYPE);
			if (typeInCU != null) {
				if (typeInCU.getCompilationUnit() != null) {
					enclosingType= typeInCU;
				}
			} else {
				ICompilationUnit cu= (ICompilationUnit) elem.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (cu != null) {
					enclosingType= cu.findPrimaryType();
				}
			}
			
			try {
				IType type= null;
				if (elem.getElementType() == IJavaElement.TYPE) {
					type= (IType)elem;
					if (type.exists()) {
						String superName= JavaModelUtil.getFullyQualifiedName(type);
						if (type.isInterface()) {
							initSuperinterfaces.add(superName);
						} else {
							initSuperclass= superName;
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				// ignore this exception now
			}
		}			

		setPackageFragment(pack, true);
		setEnclosingType(enclosingType, true);
		setEnclosingTypeSelection(false, true);
	
		setTypeName("", true); //$NON-NLS-1$
		setSuperClass(initSuperclass, true);
		setSuperInterfaces(initSuperinterfaces, true);
	}		
	
	// -------- UI Creation ---------
	
	/**
	 * Creates a separator line. Expects a GridLayout with at least 1 column.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */
	protected void createSeparator(Composite composite, int nColumns) {
		(new Separator(SWT.SEPARATOR | SWT.HORIZONTAL)).doFillIntoGrid(composite, nColumns, convertHeightInCharsToPixels(1));		
	}

	/**
	 * Creates the controls for the package name field. Expects a GridLayout with at least 4 columns.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */	
	protected void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, nColumns);
		LayoutUtil.setWidthHint(fPackageDialogField.getTextControl(null), getMaxFieldWidth());	
		LayoutUtil.setHorizontalGrabbing(fPackageDialogField.getTextControl(null));
	}

	/**
	 * Creates the controls for the enclosing type name field. Expects a GridLayout with at least 4 columns.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createEnclosingTypeControls(Composite composite, int nColumns) {
		// #6891
		Composite tabGroup= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
 		tabGroup.setLayout(layout);

		fEnclosingTypeSelection.doFillIntoGrid(tabGroup, 1);

		Control c= fEnclosingTypeDialogField.getTextControl(composite);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= getMaxFieldWidth();
		gd.horizontalSpan= 2;
		c.setLayoutData(gd);
		
		Button button= fEnclosingTypeDialogField.getChangeControl(composite);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.heightHint = SWTUtil.getButtonHeigthHint(button);
		gd.widthHint = SWTUtil.getButtonWidthHint(button);
		button.setLayoutData(gd);
	}	

	/**
	 * Creates the controls for the type name field. Expects a GridLayout with at least 2 columns.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createTypeNameControls(Composite composite, int nColumns) {
		fTypeNameDialogField.doFillIntoGrid(composite, nColumns - 1);
		DialogField.createEmptySpace(composite);
		
		LayoutUtil.setWidthHint(fTypeNameDialogField.getTextControl(null), getMaxFieldWidth());
	}

	/**
	 * Creates the controls for the modifiers radio/ceckbox buttons. Expects a GridLayout with at least 3 columns.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createModifierControls(Composite composite, int nColumns) {
		LayoutUtil.setHorizontalSpan(fAccMdfButtons.getLabelControl(composite), 1);
		
		Control control= fAccMdfButtons.getSelectionButtonsGroup(composite);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns - 2;
		control.setLayoutData(gd);
		
		DialogField.createEmptySpace(composite);
		
		DialogField.createEmptySpace(composite);
		
		control= fOtherMdfButtons.getSelectionButtonsGroup(composite);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns - 2;
		control.setLayoutData(gd);		

		DialogField.createEmptySpace(composite);
	}

	/**
	 * Creates the controls for the superclass name field. Expects a GridLayout with at least 3 columns.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createSuperClassControls(Composite composite, int nColumns) {
		fSuperClassDialogField.doFillIntoGrid(composite, nColumns);
		LayoutUtil.setWidthHint(fSuperClassDialogField.getTextControl(null), getMaxFieldWidth());
	}

	/**
	 * Creates the controls for the superclass name field. Expects a GridLayout with at least 3 columns.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */			
	protected void createSuperInterfacesControls(Composite composite, int nColumns) {
		fSuperInterfacesDialogField.doFillIntoGrid(composite, nColumns);
		GridData gd= (GridData)fSuperInterfacesDialogField.getListControl(null).getLayoutData();
		if (fIsClass) {
			gd.heightHint= convertHeightInCharsToPixels(3);
		} else {
			gd.heightHint= convertHeightInCharsToPixels(6);
		}
		gd.grabExcessVerticalSpace= false;
		gd.widthHint= getMaxFieldWidth();
	}

	
	/**
	 * Sets the focus on the type name.
	 */		
	protected void setFocus() {
		fTypeNameDialogField.setFocus();
	}
				
	// -------- TypeFieldsAdapter --------

	private class TypeFieldsAdapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter {
		
		// -------- IStringButtonAdapter
		public void changeControlPressed(DialogField field) {
			typePageChangeControlPressed(field);
		}
		
		// -------- IListAdapter
		public void customButtonPressed(DialogField field, int index) {
			typePageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(DialogField field) {}
		
		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
			typePageDialogFieldChanged(field);
		}
	}
	
	private void typePageChangeControlPressed(DialogField field) {
		if (field == fPackageDialogField) {
			IPackageFragment pack= choosePackage();	
			if (pack != null) {
				fPackageDialogField.setText(pack.getElementName());
			}
		} else if (field == fEnclosingTypeDialogField) {
			IType type= chooseEnclosingType();
			if (type != null) {
				fEnclosingTypeDialogField.setText(JavaModelUtil.getFullyQualifiedName(type));
			}
		} else if (field == fSuperClassDialogField) {
			IType type= chooseSuperType();
			if (type != null) {
				fSuperClassDialogField.setText(JavaModelUtil.getFullyQualifiedName(type));
			}
		}
	}
	
	private void typePageCustomButtonPressed(DialogField field, int index) {		
		if (field == fSuperInterfacesDialogField) {
			chooseSuperInterfaces();
		}
	}
	
	/*
	 * A field on the type has changed. The fields' status and all dependend
	 * status are updated.
	 */
	private void typePageDialogFieldChanged(DialogField field) {
		String fieldName= null;
		if (field == fPackageDialogField) {
			fPackageStatus= packageChanged();
			updatePackageStatusLabel();
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();			
			fieldName= PACKAGE;
		} else if (field == fEnclosingTypeDialogField) {
			fEnclosingTypeStatus= enclosingTypeChanged();
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();				
			fieldName= ENCLOSING;
		} else if (field == fEnclosingTypeSelection) {
			updateEnableState();
			boolean isEnclosedType= isEnclosingTypeSelected();
			if (!isEnclosedType) {
				if (fAccMdfButtons.isSelected(PRIVATE_INDEX) || fAccMdfButtons.isSelected(PROTECTED_INDEX)) {
					fAccMdfButtons.setSelection(PRIVATE_INDEX, false);
					fAccMdfButtons.setSelection(PROTECTED_INDEX, false); 
					fAccMdfButtons.setSelection(PUBLIC_INDEX, true);
				}
				if (fOtherMdfButtons.isSelected(fStaticMdfIndex)) {
					fOtherMdfButtons.setSelection(fStaticMdfIndex, false);
				}
			}
			fAccMdfButtons.enableSelectionButton(PRIVATE_INDEX, isEnclosedType && fIsClass);
			fAccMdfButtons.enableSelectionButton(PROTECTED_INDEX, isEnclosedType && fIsClass);
			fOtherMdfButtons.enableSelectionButton(fStaticMdfIndex, isEnclosedType);
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();
			fieldName= ENCLOSINGSELECTION;
		} else if (field == fTypeNameDialogField) {
			fTypeNameStatus= typeNameChanged();
			fieldName= TYPENAME;
		} else if (field == fSuperClassDialogField) {
			fSuperClassStatus= superClassChanged();
			fieldName= SUPER;
		} else if (field == fSuperInterfacesDialogField) {
			fSuperInterfacesStatus= superInterfacesChanged();
			fieldName= INTERFACES;
		} else if (field == fOtherMdfButtons) {
			fModifierStatus= modifiersChanged();
			fieldName= MODIFIERS;
		} else {
			fieldName= METHODS;
		}
		// tell all others
		handleFieldChanged(fieldName);
	}		
	
	
		
	// -------- update message ----------------		

	/**
	 * Called whenever a content of a field has changed.
	 * Implementors of NewTypeWizardPage can hook in.
	 * @see ContainerPage#handleFieldChanged
	 */			
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName == CONTAINER) {
			fPackageStatus= packageChanged();
			fEnclosingTypeStatus= enclosingTypeChanged();			
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();
			fSuperInterfacesStatus= superInterfacesChanged();
		}
	}
	
	// ---- set / get ----------------
	
	/**
	 * Gets the text of package field.
	 */
	public String getPackageText() {
		return fPackageDialogField.getText();
	}

	/**
	 * Gets the text of enclosing type field.
	 */	
	public String getEnclosingTypeText() {
		return fEnclosingTypeDialogField.getText();
	}	
	
	
	/**
	 * Returns the package fragment corresponding to the current input.
	 * @return Returns <code>null</code> if the input could not be resolved.
	 */
	public IPackageFragment getPackageFragment() {
		if (!isEnclosingTypeSelected()) {
			return fCurrPackage;
		} else {
			if (fCurrEnclosingType != null) {
				return fCurrEnclosingType.getPackageFragment();
			}
		}
		return null;
	}
	
	/**
	 * Sets the package fragment.
	 * This will update model and the text of the control.
	 * @param canBeModified Selects if the package fragment can be changed by the user
	 */
	public void setPackageFragment(IPackageFragment pack, boolean canBeModified) {
		fCurrPackage= pack;
		fCanModifyPackage= canBeModified;
		String str= (pack == null) ? "" : pack.getElementName(); //$NON-NLS-1$
		fPackageDialogField.setText(str);
		updateEnableState();
	}	

	/**
	 * Returns the enclosing type corresponding to the current input.
	 * @return Returns <code>null</code> if enclosing type is not selected or the input could not
	 * be resolved.
	 */
	public IType getEnclosingType() {
		if (isEnclosingTypeSelected()) {
			return fCurrEnclosingType;
		}
		return null;
	}

	/**
	 * Sets the enclosing type.
	 * This will update model and the text of the control.
	 * @param canBeModified Selects if the enclosing type can be changed by the user
	 */	
	public void setEnclosingType(IType type, boolean canBeModified) {
		fCurrEnclosingType= type;
		fCanModifyEnclosingType= canBeModified;
		String str= (type == null) ? "" : JavaModelUtil.getFullyQualifiedName(type); //$NON-NLS-1$
		fEnclosingTypeDialogField.setText(str);
		updateEnableState();
	}
	
	/**
	 * Returns <code>true</code> if the enclosing type selection check box is enabled.
	 */
	public boolean isEnclosingTypeSelected() {
		return fEnclosingTypeSelection.isSelected();
	}

	/**
	 * Sets the enclosing type selection checkbox.
	 * @param canBeModified Selects if the enclosing type selection can be changed by the user
	 */	
	public void setEnclosingTypeSelection(boolean isSelected, boolean canBeModified) {
		fEnclosingTypeSelection.setSelection(isSelected);
		fEnclosingTypeSelection.setEnabled(canBeModified);
		updateEnableState();
	}
	
	/**
	 * Gets the type name.
	 */
	public String getTypeName() {
		return fTypeNameDialogField.getText();
	}

	/**
	 * Sets the type name.
	 * @param canBeModified Selects if the type name can be changed by the user
	 */	
	public void setTypeName(String name, boolean canBeModified) {
		fTypeNameDialogField.setText(name);
		fTypeNameDialogField.setEnabled(canBeModified);
	}	
	
	/**
	 * Gets the selected modifiers.
	 * @see Flags 
	 */	
	public int getModifiers() {
		int mdf= 0;
		if (fAccMdfButtons.isSelected(PUBLIC_INDEX)) {
			mdf+= F_PUBLIC;
		} else if (fAccMdfButtons.isSelected(PRIVATE_INDEX)) {
			mdf+= F_PRIVATE;
		} else if (fAccMdfButtons.isSelected(PROTECTED_INDEX)) {	
			mdf+= F_PROTECTED;
		}
		if (fOtherMdfButtons.isSelected(ABSTRACT_INDEX) && (fStaticMdfIndex != 0)) {	
			mdf+= F_ABSTRACT;
		}
		if (fOtherMdfButtons.isSelected(FINAL_INDEX)) {	
			mdf+= F_FINAL;
		}
		if (fOtherMdfButtons.isSelected(fStaticMdfIndex)) {	
			mdf+= F_STATIC;
		}
		return mdf;
	}

	/**
	 * Sets the modifiers.
	 * @param modifiers F_PUBLIC, F_PRIVATE, F_PROTECTED, F_ABSTRACT, F_FINAL
	 * or  F_STATIC or a valid combination.
	 * @param canBeModified Selects if the modifiers can be changed by the user
	 * @see Flags 
	 */		
	public void setModifiers(int modifiers, boolean canBeModified) {
		if (Flags.isPublic(modifiers)) {
			fAccMdfButtons.setSelection(PUBLIC_INDEX, true);
		} else if (Flags.isPrivate(modifiers)) {
			fAccMdfButtons.setSelection(PRIVATE_INDEX, true);
		} else if (Flags.isProtected(modifiers)) {
			fAccMdfButtons.setSelection(PROTECTED_INDEX, true);
		} else {
			fAccMdfButtons.setSelection(DEFAULT_INDEX, true);
		}
		if (Flags.isAbstract(modifiers)) {
			fOtherMdfButtons.setSelection(ABSTRACT_INDEX, true);
		}
		if (Flags.isFinal(modifiers)) {
			fOtherMdfButtons.setSelection(FINAL_INDEX, true);
		}		
		if (Flags.isStatic(modifiers)) {
			fOtherMdfButtons.setSelection(fStaticMdfIndex, true);
		}
		
		fAccMdfButtons.setEnabled(canBeModified);
		fOtherMdfButtons.setEnabled(canBeModified);
	}
		
	/**
	 * Gets the content of the super class text field.
	 */
	public String getSuperClass() {
		return fSuperClassDialogField.getText();
	}

	/**
	 * Sets the super class name.
	 * @param canBeModified Selects if the super class can be changed by the user
	 */		
	public void setSuperClass(String name, boolean canBeModified) {
		fSuperClassDialogField.setText(name);
		fSuperClassDialogField.setEnabled(canBeModified);
	}	
	
	/**
	 * Gets the currently chosen super interfaces.
	 * @return returns a list of String
	 */
	public List getSuperInterfaces() {
		return fSuperInterfacesDialogField.getElements();
	}

	/**
	 * Sets the super interfaces.
	 * @param canBeModified Selects if the modifiers can be changed by the user.
	 * @param interfacesNames a list of String
	 */	
	public void setSuperInterfaces(List interfacesNames, boolean canBeModified) {
		fSuperInterfacesDialogField.setElements(interfacesNames);
		fSuperInterfacesDialogField.setEnabled(canBeModified);
	}
			
	// ----------- validation ----------
		
	/**
	 * Called when the package field has changed.
	 * The method validates the package name and returns the status of the validation
	 * This also updates the package fragment model.
	 * Can be extended to add more validation
	 */
	protected IStatus packageChanged() {
		StatusInfo status= new StatusInfo();
		fPackageDialogField.enableButton(getPackageFragmentRoot() != null);
		
		String packName= getPackageText();
		if (packName.length() > 0) {
			IStatus val= JavaConventions.validatePackageName(packName);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(NewWizardMessages.getFormattedString("NewTypeWizardPage.error.InvalidPackageName", val.getMessage())); //$NON-NLS-1$
				return status;
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(NewWizardMessages.getFormattedString("NewTypeWizardPage.warning.DiscouragedPackageName", val.getMessage())); //$NON-NLS-1$
				// continue
			}
		}
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null) {
			if (root.getJavaProject().exists() && packName.length() > 0) {
				try {
					IPath rootPath= root.getPath();
					IPath outputPath= root.getJavaProject().getOutputLocation();
					if (rootPath.isPrefixOf(outputPath) && !rootPath.equals(outputPath)) {
						// if the bin folder is inside of our root, dont allow to name a package
						// like the bin folder
						IPath packagePath= rootPath.append(packName.replace('.', '/'));
						if (outputPath.isPrefixOf(packagePath)) {
							status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.ClashOutputLocation")); //$NON-NLS-1$
							return status;
						}
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					// let pass			
				}
			}
			
			fCurrPackage= root.getPackageFragment(packName);
		} else {
			status.setError(""); //$NON-NLS-1$
		}
		return status;
	}

	/*
	 * Updates the 'default' label next to the package field.
	 */	
	private void updatePackageStatusLabel() {
		String packName= getPackageText();
		
		if (packName.length() == 0) {
			fPackageDialogField.setStatus(NewWizardMessages.getString("NewTypeWizardPage.default")); //$NON-NLS-1$
		} else {
			fPackageDialogField.setStatus(""); //$NON-NLS-1$
		}
	}
	
	/*
	 * Updates the enable state of buttons related to the enclosing type selection checkbox.
	 */
	private void updateEnableState() {
		boolean enclosing= isEnclosingTypeSelected();
		fPackageDialogField.setEnabled(fCanModifyPackage && !enclosing);
		fEnclosingTypeDialogField.setEnabled(fCanModifyEnclosingType && enclosing);
	}	

	/**
	 * Called when the enclosing type name has changed.
	 * The method validates the enclosing type and returns the status of the validation
	 * This also updates the enclosing type model.
	 * Can be extended to add more validation
	 */
	protected IStatus enclosingTypeChanged() {
		StatusInfo status= new StatusInfo();
		fCurrEnclosingType= null;
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		
		fEnclosingTypeDialogField.enableButton(root != null);
		if (root == null) {
			status.setError(""); //$NON-NLS-1$
			return status;
		}
		
		String enclName= getEnclosingTypeText();
		if (enclName.length() == 0) {
			status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.EnclosingTypeEnterName")); //$NON-NLS-1$
			return status;
		}
		try {
			IType type= root.getJavaProject().findType(enclName);
			if (type == null) {
				status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.EnclosingTypeNotExists")); //$NON-NLS-1$
				return status;
			}

			if (type.getCompilationUnit() == null) {
				status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.EnclosingNotInCU")); //$NON-NLS-1$
				return status;
			}
			if (!JavaModelUtil.isEditable(type.getCompilationUnit())) {
				status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.EnclosingNotEditable")); //$NON-NLS-1$
				return status;			
			}
			
			fCurrEnclosingType= type;
			IPackageFragmentRoot enclosingRoot= JavaModelUtil.getPackageFragmentRoot(type);
			if (!enclosingRoot.equals(root)) {
				status.setWarning(NewWizardMessages.getString("NewTypeWizardPage.warning.EnclosingNotInSourceFolder")); //$NON-NLS-1$
			}
			return status;
		} catch (JavaModelException e) {
			status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.EnclosingTypeNotExists")); //$NON-NLS-1$
			JavaPlugin.log(e);
			return status;
		}
	}
	
	/**
	 * Called when the type name has changed.
	 * The method validates the type name and returns the status of the validation.
	 * Can be extended to add more validation
	 */
	protected IStatus typeNameChanged() {
		StatusInfo status= new StatusInfo();
		String typeName= getTypeName();
		// must not be empty
		if (typeName.length() == 0) {
			status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.EnterTypeName")); //$NON-NLS-1$
			return status;
		}
		if (typeName.indexOf('.') != -1) {
			status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.QualifiedName")); //$NON-NLS-1$
			return status;
		}
		IStatus val= JavaConventions.validateJavaTypeName(typeName);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(NewWizardMessages.getFormattedString("NewTypeWizardPage.error.InvalidTypeName", val.getMessage())); //$NON-NLS-1$
			return status;
		} else if (val.getSeverity() == IStatus.WARNING) {
			status.setWarning(NewWizardMessages.getFormattedString("NewTypeWizardPage.warning.TypeNameDiscouraged", val.getMessage())); //$NON-NLS-1$
			// continue checking
		}		

		// must not exist
		if (!isEnclosingTypeSelected()) {
			IPackageFragment pack= getPackageFragment();
			if (pack != null) {
				ICompilationUnit cu= pack.getCompilationUnit(typeName + ".java"); //$NON-NLS-1$
				if (cu.exists()) {
					status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.TypeNameExists")); //$NON-NLS-1$
					return status;
				}
			}
		} else {
			IType type= getEnclosingType();
			if (type != null) {
				IType member= type.getType(typeName);
				if (member.exists()) {
					status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.TypeNameExists")); //$NON-NLS-1$
					return status;
				}
			}
		}
		return status;
	}
	
	/**
	 * Called when the superclass name has changed.
	 * The method validates the superclass name and returns the status of the validation.
	 * Can be extended to add more validation
	 */
	protected IStatus superClassChanged() {
		StatusInfo status= new StatusInfo();
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fSuperClassDialogField.enableButton(root != null);
		
		fSuperClass= null;
		
		String sclassName= getSuperClass();
		if (sclassName.length() == 0) {
			// accept the empty field (stands for java.lang.Object)
			return status;
		}
		IStatus val= JavaConventions.validateJavaTypeName(sclassName);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.InvalidSuperClassName")); //$NON-NLS-1$
			return status;
		} 
		if (root != null) {
			try {		
				IType type= resolveSuperTypeName(root.getJavaProject(), sclassName);
				if (type == null) {
					status.setWarning(NewWizardMessages.getString("NewTypeWizardPage.warning.SuperClassNotExists")); //$NON-NLS-1$
					return status;
				} else {
					if (type.isInterface()) {
						status.setWarning(NewWizardMessages.getFormattedString("NewTypeWizardPage.warning.SuperClassIsNotClass", sclassName)); //$NON-NLS-1$
						return status;
					}
					int flags= type.getFlags();
					if (Flags.isFinal(flags)) {
						status.setWarning(NewWizardMessages.getFormattedString("NewTypeWizardPage.warning.SuperClassIsFinal", sclassName)); //$NON-NLS-1$
						return status;
					} else if (!JavaModelUtil.isVisible(type, getPackageFragment())) {
						status.setWarning(NewWizardMessages.getFormattedString("NewTypeWizardPage.warning.SuperClassIsNotVisible", sclassName)); //$NON-NLS-1$
						return status;
					}
				}
				fSuperClass= type;
			} catch (JavaModelException e) {
				status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.InvalidSuperClassName")); //$NON-NLS-1$
				JavaPlugin.log(e);
			}							
		} else {
			status.setError(""); //$NON-NLS-1$
		}
		return status;
		
	}
	
	private IType resolveSuperTypeName(IJavaProject jproject, String sclassName) throws JavaModelException {
		IType type= null;
		if (isEnclosingTypeSelected()) {
			// search in the context of the enclosing type
			IType enclosingType= getEnclosingType();
			if (enclosingType != null) {
				String[][] res= enclosingType.resolveType(sclassName);
				if (res != null && res.length > 0) {
					type= jproject.findType(res[0][0], res[0][1]);
				}
			}
		} else {
			IPackageFragment currPack= getPackageFragment();
			if (type == null && currPack != null) {
				String packName= currPack.getElementName();
				// search in own package
				if (!currPack.isDefaultPackage()) {
					type= jproject.findType(packName, sclassName);
				}
				// search in java.lang
				if (type == null && !"java.lang".equals(packName)) { //$NON-NLS-1$
					type= jproject.findType("java.lang", sclassName); //$NON-NLS-1$
				}
			}
			// search fully qualified
			if (type == null) {
				type= jproject.findType(sclassName);
			}
		}
		return type;
	}		
	
	/**
	 * Called when the list of super interface has changed.
	 * The method validates the superinterfaces and returns the status of the validation.
	 * Can be extended to add more validation.
	 */
	protected IStatus superInterfacesChanged() {
		StatusInfo status= new StatusInfo();
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fSuperInterfacesDialogField.enableButton(0, root != null);
						
		if (root != null) {
			List elements= fSuperInterfacesDialogField.getElements();
			int nElements= elements.size();
			for (int i= 0; i < nElements; i++) {
				String intfname= (String)elements.get(i);
				try {
					IType type= root.getJavaProject().findType(intfname);
					if (type == null) {
						status.setWarning(NewWizardMessages.getFormattedString("NewTypeWizardPage.warning.InterfaceNotExists", intfname)); //$NON-NLS-1$
						return status;
					} else {
						if (type.isClass()) {
							status.setWarning(NewWizardMessages.getFormattedString("NewTypeWizardPage.warning.InterfaceIsNotInterface", intfname)); //$NON-NLS-1$
							return status;
						}
						if (!JavaModelUtil.isVisible(type, getPackageFragment())) {
							status.setWarning(NewWizardMessages.getFormattedString("NewTypeWizardPage.warning.InterfaceIsNotVisible", intfname)); //$NON-NLS-1$
							return status;
						}
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					// let pass, checking is an extra
				}					
			}				
		}
		return status;
	}

	/**
	 * Called when the modifiers have changed.
	 * The method validates the modifiers and returns the status of the validation.
	 * Can be extended to add more validation.
	 */
	protected IStatus modifiersChanged() {
		StatusInfo status= new StatusInfo();
		int modifiers= getModifiers();
		if (Flags.isFinal(modifiers) && Flags.isAbstract(modifiers)) {
			status.setError(NewWizardMessages.getString("NewTypeWizardPage.error.ModifiersFinalAndAbstract")); //$NON-NLS-1$
		}
		return status;
	}
	
	// selection dialogs
	
	
	private IPackageFragment choosePackage() {
		IPackageFragmentRoot froot= getPackageFragmentRoot();
		IJavaElement[] packages= null;
		try {
			if (froot != null) {
				packages= froot.getChildren();
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		if (packages == null) {
			packages= new IJavaElement[0];
		}
		
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
		dialog.setIgnoreCase(false);
		dialog.setTitle(NewWizardMessages.getString("NewTypeWizardPage.ChoosePackageDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("NewTypeWizardPage.ChoosePackageDialog.description")); //$NON-NLS-1$
		dialog.setEmptyListMessage(NewWizardMessages.getString("NewTypeWizardPage.ChoosePackageDialog.empty")); //$NON-NLS-1$
		dialog.setElements(packages);
		if (fCurrPackage != null) {
			dialog.setInitialSelections(new Object[] { fCurrPackage });
		}

		if (dialog.open() == dialog.OK) {
			return (IPackageFragment) dialog.getFirstResult();
		}
		return null;
	}
	
	private IType chooseEnclosingType() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return null;
		}
		
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { root });
			
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), IJavaSearchConstants.TYPE, scope);
		dialog.setTitle(NewWizardMessages.getString("NewTypeWizardPage.ChooseEnclosingTypeDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("NewTypeWizardPage.ChooseEnclosingTypeDialog.description")); //$NON-NLS-1$
		if (fCurrEnclosingType != null) {
			dialog.setInitialSelections(new Object[] { fCurrEnclosingType });
			dialog.setFilter(fCurrEnclosingType.getElementName().substring(0, 1));
		}
		
		if (dialog.open() == dialog.OK) {	
			return (IType) dialog.getFirstResult();
		}
		return null;
	}	
	
	private IType chooseSuperType() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return null;
		}	

		IJavaElement[] elements= new IJavaElement[] { root.getJavaProject() };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);

		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), IJavaSearchConstants.CLASS, scope);
		dialog.setTitle(NewWizardMessages.getString("NewTypeWizardPage.SuperClassDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("NewTypeWizardPage.SuperClassDialog.message")); //$NON-NLS-1$
		if (fSuperClass != null) {
			dialog.setFilter(fSuperClass.getElementName());
		}

		if (dialog.open() == dialog.OK) {
			return (IType) dialog.getFirstResult();
		}
		return null;
	}
	
	private void chooseSuperInterfaces() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return;
		}	

		IJavaProject project= root.getJavaProject();
		SuperInterfaceSelectionDialog dialog= new SuperInterfaceSelectionDialog(getShell(), getWizard().getContainer(), fSuperInterfacesDialogField, project);
		dialog.setTitle(fIsClass ? NewWizardMessages.getString("NewTypeWizardPage.InterfacesDialog.class.title") : NewWizardMessages.getString("NewTypeWizardPage.InterfacesDialog.interface.title")); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setMessage(NewWizardMessages.getString("NewTypeWizardPage.InterfacesDialog.message")); //$NON-NLS-1$
		dialog.open();
		return;
	}	
	
	
		
	// ---- creation ----------------

	/**
	 * Creates a type using the current field values.
	 */
	public void createType(IProgressMonitor monitor) throws CoreException, InterruptedException {		
		monitor.beginTask(NewWizardMessages.getString("NewTypeWizardPage.operationdesc"), 10); //$NON-NLS-1$
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		IPackageFragment pack= getPackageFragment();
		if (pack == null) {
			pack= root.getPackageFragment(""); //$NON-NLS-1$
		}
		
		if (!pack.exists()) {
			String packName= pack.getElementName();
			pack= root.createPackageFragment(packName, true, null);
		}		
		
		monitor.worked(1);
		
		String clName= getTypeName();
		
		boolean isInnerClass= isEnclosingTypeSelected();
		
		IType createdType;
		ImportsStructure imports;
		int indent= 0;

		String[] prefOrder= ImportOrganizePreferencePage.getImportOrderPreference();
		int threshold= ImportOrganizePreferencePage.getImportNumberThreshold();			
		
		String lineDelimiter= null;	
		if (!isInnerClass) {
			lineDelimiter= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			
			String packStatement= pack.isDefaultPackage() ? "" : "package " + pack.getElementName() + ";" + lineDelimiter + lineDelimiter; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			ICompilationUnit parentCU= pack.createCompilationUnit(clName + ".java", packStatement, false, new SubProgressMonitor(monitor, 2)); //$NON-NLS-1$

			imports= new ImportsStructure(parentCU, prefOrder, threshold, false);
			
			String content= constructTypeStub(new ImportsManager(imports), lineDelimiter, parentCU);
			createdType= parentCU.createType(content, null, false, new SubProgressMonitor(monitor, 3));
		} else {
			IType enclosingType= getEnclosingType();
			
			// if we are working on a enclosed type that is open in an editor,
			// then replace the enclosing type with its working copy
			IType workingCopy= (IType) EditorUtility.getWorkingCopy(enclosingType);
			if (workingCopy != null) {
				enclosingType= workingCopy;
			}

			ICompilationUnit parentCU= enclosingType.getCompilationUnit();
			imports= new ImportsStructure(parentCU, prefOrder, threshold, true);
			
			lineDelimiter= StubUtility.getLineDelimiterUsed(enclosingType);
			String content= constructTypeStub(new ImportsManager(imports), lineDelimiter, parentCU);
			IJavaElement[] elems= enclosingType.getChildren();
			IJavaElement sibling= elems.length > 0 ? elems[0] : null;
			
			createdType= enclosingType.createType(content, sibling, false, new SubProgressMonitor(monitor, 1));
		
			indent= StubUtility.getIndentUsed(enclosingType) + 1;
		}
		
		// add imports for superclass/interfaces, so types can be resolved correctly
		imports.create(!isInnerClass, new SubProgressMonitor(monitor, 1));
		
		createTypeMembers(createdType, new ImportsManager(imports), new SubProgressMonitor(monitor, 1));

		// add imports
		imports.create(!isInnerClass, new SubProgressMonitor(monitor, 1));
		
		ICompilationUnit cu= createdType.getCompilationUnit();	
		ISourceRange range;
		if (isInnerClass) {
			synchronized(cu) {
				cu.reconcile();
			}
			range= createdType.getSourceRange();
		} else {
			range= cu.getSourceRange();
		}
		
		IBuffer buf= cu.getBuffer();
		String originalContent= buf.getText(range.getOffset(), range.getLength());
		String formattedContent= StubUtility.codeFormat(originalContent, indent, lineDelimiter);
		buf.replace(range.getOffset(), range.getLength(), formattedContent);
		if (!isInnerClass) {
			String fileComment= getFileComment(cu);
			if (fileComment != null && fileComment.length() > 0) {
				buf.replace(0, 0, fileComment + lineDelimiter);
			}
			buf.save(new SubProgressMonitor(monitor, 1), false);
		} else {
			monitor.worked(1);
		}
		fCreatedType= createdType;
		monitor.done();
	}	

	/**
	 * Returns the created type. Only valid after createType has been invoked
	 */			
	public IType getCreatedType() {
		return fCreatedType;
	}
	
	// ---- construct cu body----------------
		
	private void writeSuperClass(StringBuffer buf, ImportsManager imports) {
		String typename= getSuperClass();
		if (fIsClass && typename.length() > 0 && !"java.lang.Object".equals(typename)) { //$NON-NLS-1$
			buf.append(" extends "); //$NON-NLS-1$
			
			String qualifiedName= fSuperClass != null ? JavaModelUtil.getFullyQualifiedName(fSuperClass) : typename; 
			buf.append(imports.addImport(qualifiedName));
		}
	}
	
	private void writeSuperInterfaces(StringBuffer buf, ImportsManager imports) {
		List interfaces= getSuperInterfaces();
		int last= interfaces.size() - 1;
		if (last >= 0) {
			if (fIsClass) {
				buf.append(" implements "); //$NON-NLS-1$
			} else {
				buf.append(" extends "); //$NON-NLS-1$
			}
			for (int i= 0; i <= last; i++) {
				String typename= (String) interfaces.get(i);
				buf.append(imports.addImport(typename));
				if (i < last) {
					buf.append(',');
				}
			}
		}
	}

	/*
	 * Called from createType to construct the source for this type
	 */		
	private String constructTypeStub(ImportsManager imports, String lineDelimiter, ICompilationUnit parentCU) {	
		StringBuffer buf= new StringBuffer();
		String typeComment= getTypeComment(parentCU);
		if (typeComment != null && typeComment.length() > 0) {
			buf.append(typeComment);
			buf.append(lineDelimiter);
		}
		
		int modifiers= getModifiers();
		buf.append(Flags.toString(modifiers));
		if (modifiers != 0) {
			buf.append(' ');
		}
		buf.append(fIsClass ? "class " : "interface "); //$NON-NLS-2$ //$NON-NLS-1$
		buf.append(getTypeName());
		writeSuperClass(buf, imports);
		writeSuperInterfaces(buf, imports);	
		buf.append('{');
		buf.append(lineDelimiter);
		buf.append(lineDelimiter);
		buf.append('}');
		buf.append(lineDelimiter);
		return buf.toString();
	}

	/**
	 * @deprecated Overwrite createTypeMembers(IType, IImportsManager, IProgressMonitor) instead
	 */		
	protected void createTypeMembers(IType newType, IImportsStructure imports, IProgressMonitor monitor) throws CoreException {
		//deprecated
	}
	
	/**
	 * Called from createType to allow adding methods, fields, inner types ect for the newly created type.
	 * Implementors can use the create methods on IType.
	 * Formatting will be applied to the content by the createType. Imports are added after this call by the wizard
	 * @param newType The new type to add members to
	 * @param imports To add the needed imports to. 
	 * @param monitor Progress monitor
	 */		
	protected void createTypeMembers(IType newType, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		// call for compatibility
		createTypeMembers(newType, ((ImportsManager)imports).getImportsStructure(), monitor);
		
		// default implementation does nothing
		// example would be
		// String mainMathod= "public void foo(Vector vec) {}"
		// createdType.createMethod(main, null, false, null);
		// imports.addImport("java.lang.Vector");
	}	
	
		
	/**
	 * Called from createType to get a file comment. By default the content of template
	 * 'filecomment' is taken.
	 * Returns source or null, if no file comment should be added
	 */		
	protected String getFileComment(ICompilationUnit parentCU) {
		if (CodeGenerationPreferencePage.doFileComments()) {
			return getTemplate("filecomment", parentCU, 0); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * Called from createType to get a type comment. 
	 * Returns source or null, if no type comment should be added
	 */		
	protected String getTypeComment(ICompilationUnit parentCU) {
		if (CodeGenerationPreferencePage.doCreateComments()) {
			return getTemplate("typecomment", parentCU, 0); //$NON-NLS-1$
		}
		return null;
	}


	/**
	 * @deprecated Use getTemplate(String,ICompilationUnit,int)
	 */
	protected String getTemplate(String name, ICompilationUnit parentCU) {
		return getTemplate(name, parentCU, 0);
	
	}
	
	/**
	 * Gets the template of the given name, evaluated in the context of a CU.
	 */
	protected String getTemplate(String name, ICompilationUnit parentCU, int pos) {
		try {
			Template[] templates= Templates.getInstance().getTemplates(name);
			if (templates.length > 0) {
				return JavaContext.evaluateTemplate(templates[0], parentCU, pos);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return null;
	}	
	

	/**
	 * @deprecated Use createInheritedMethods(IType,boolean,boolean,IImportsManager,IProgressMonitor)
	 */
	protected IMethod[] createInheritedMethods(IType type, boolean doConstructors, boolean doUnimplementedMethods, IImportsStructure imports, IProgressMonitor monitor) throws CoreException {
		return createInheritedMethods(type, doConstructors, doUnimplementedMethods, new ImportsManager(imports), monitor);
	}


	/**
	 * Creates the bodies of all unimplemented methods or/and all constructors and adds them to the type
	 * Can be used by implementors of NewTypeWizardPage to add method stub checkboxes.
	 */
	protected IMethod[] createInheritedMethods(IType type, boolean doConstructors, boolean doUnimplementedMethods, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		ArrayList newMethods= new ArrayList();
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(monitor);
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();

		if (doConstructors) {
			IType superclass= hierarchy.getSuperclass(type);
			if (superclass != null) {
				String[] constructors= StubUtility.evalConstructors(type, superclass, settings, imports.getImportsStructure());
				if (constructors != null) {
					for (int i= 0; i < constructors.length; i++) {
						newMethods.add(constructors[i]);
					}
				}
			
			}
		}
		if (doUnimplementedMethods) {
			String[] unimplemented= StubUtility.evalUnimplementedMethods(type, hierarchy, false, settings, null, imports.getImportsStructure());
			if (unimplemented != null) {
				for (int i= 0; i < unimplemented.length; i++) {
					newMethods.add(unimplemented[i]);					
				}
			}
		}
		IMethod[] createdMethods= new IMethod[newMethods.size()];
		for (int i= 0; i < newMethods.size(); i++) {
			String content= (String) newMethods.get(i) + '\n'; // content will be formatted, ok to use \n
			createdMethods[i]= type.createMethod(content, null, false, null);
		}
		return createdMethods;
	}
	
	// ---- creation ----------------

	/**
	 * Returns a runnable that creates a type using the current settings.
	 * To be called in the UI thread.
	 */		
	public IRunnableWithProgress getRunnable() {				
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					createType(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 				
			}
		};
	}	

}