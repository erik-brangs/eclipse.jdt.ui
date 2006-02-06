/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.delegates;

import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringSessionDescriptor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDeprecationResolving;

/**
 * Delegate creator for static and non-static methods.
 * 
 * @since 3.2
 */
public class DelegateMethodCreator extends DelegateCreator {

	private ASTNode fDelegateInvocation;
	private MethodRef fDocMethodReference;

	protected void initialize() {

		Assert.isTrue(getDeclaration() instanceof MethodDeclaration);

		if (getNewElementName() == null)
			setNewElementName(((MethodDeclaration) getDeclaration()).getName().getIdentifier());
		
		setInsertBefore(true); 
	}

	protected ASTNode createBody(BodyDeclaration bd) throws JavaModelException {

		MethodDeclaration methodDeclaration= (MethodDeclaration) bd;

		// interface or abstract method ? => don't create a method body.
		if (methodDeclaration.getBody() == null)
			return null;

		return createDelegateMethodBody(methodDeclaration);
	}

	protected ASTNode createDocReference(final BodyDeclaration declaration) throws JavaModelException {
		fDocMethodReference= getAst().newMethodRef();
		fDocMethodReference.setName(getAst().newSimpleName(getNewElementName()));
		if (isMoveToAnotherFile())
			fDocMethodReference.setQualifier(createDestinationTypeName());
		createArguments((MethodDeclaration) declaration, fDocMethodReference.parameters(), false);
		return fDocMethodReference;
	}

	protected ASTNode getBodyHead(BodyDeclaration result) {
		return result;
	}

	protected ChildPropertyDescriptor getJavaDocProperty() {
		return MethodDeclaration.JAVADOC_PROPERTY;
	}

	protected ChildPropertyDescriptor getBodyProperty() {
		return MethodDeclaration.BODY_PROPERTY;
	}

	/**
	 * @return the delegate incovation, either a {@link ConstructorInvocation}
	 *         or a {@link MethodInvocation}. May be null if the delegate
	 *         method is abstract (and therefore has no body at all)
	 */
	public ASTNode getDelegateInvocation() {
		return fDelegateInvocation;
	}

	/**
	 * @return the javadoc reference to the old method in the javadoc comment.
	 * 		   May be null if no comment was created. 
	 */
	public MethodRef getJavadocReference() {
		return fDocMethodReference;
	}

	/**
	 * Creates the corresponding statement for the method invocation, based on
	 * the return type.
	 * 
	 * @param declaration the method declaration where the invocation statement
	 *            is inserted
	 * @param invocation the method invocation being encapsulated by the
	 *            resulting statement
	 * @return the corresponding statement
	 */
	protected Statement createMethodInvocation(final MethodDeclaration declaration, final MethodInvocation invocation) {
		Assert.isNotNull(declaration);
		Assert.isNotNull(invocation);
		Statement statement= null;
		final Type type= declaration.getReturnType2();
		if (type == null)
			statement= createExpressionStatement(invocation);
		else {
			if (type instanceof PrimitiveType) {
				final PrimitiveType primitive= (PrimitiveType) type;
				if (primitive.getPrimitiveTypeCode().equals(PrimitiveType.VOID))
					statement= createExpressionStatement(invocation);
				else
					statement= createReturnStatement(invocation);
			} else
				statement= createReturnStatement(invocation);
		}
		return statement;
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringSessionDescriptor createRefactoringScript() {
		final MethodDeclaration declaration= (MethodDeclaration) getDeclaration();
		final IMethodBinding binding= declaration.resolveBinding();
		if (binding != null) {
			final IJavaElement element= binding.getJavaElement();
			if (element instanceof IMethod) {
				final IMethod method= (IMethod) element;
				final IDeprecationResolving resolving= new InlineMethodRefactoring(method);
				if (resolving.canEnableDeprecationResolving())
					return resolving.createDeprecationResolution();
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String createRefactoringScriptName() {
		final MethodDeclaration declaration= (MethodDeclaration) getDeclaration();
		final IMethodBinding binding= declaration.resolveBinding();
		if (binding != null) {
			final StringBuffer buffer= new StringBuffer();
			buffer.append(SCRIPT_NAME_PREFIX);
			final IJavaElement element= binding.getDeclaringClass().getJavaElement();
			if (element instanceof IType) {
				final IType type= (IType) element;
				buffer.append(type.getFullyQualifiedName());
				buffer.append('.');
				buffer.append(binding.getName());
				buffer.append('(');
				final ITypeBinding[] parameters= binding.getParameterTypes();
				for (int index= 0; index < parameters.length; index++) {
					if (index != 0)
						buffer.append(',');
					final IJavaElement javaElem= parameters[index].getJavaElement();
					if (javaElem instanceof IType)
						buffer.append(((IType) javaElem).getFullyQualifiedName());
					else if (javaElem instanceof ITypeParameter)
						buffer.append(((ITypeParameter) javaElem).getElementName());
					else
						buffer.append(parameters[index].getQualifiedName());
				}
				buffer.append(')');
				buffer.append(".xml"); //$NON-NLS-1$
				return buffer.toString();
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	protected IPackageFragment getRefactoringScriptPackage() {
		final MethodDeclaration declaration= (MethodDeclaration) getDeclaration();
		final IMethodBinding binding= declaration.resolveBinding();
		if (binding != null) {
			final ITypeBinding declaring= binding.getDeclaringClass();
			if (declaring != null) {
				final IPackageBinding pack= declaring.getPackage();
				if (pack != null) {
					return (IPackageFragment) pack.getJavaElement();
				}
			}
		}
		return null;
	}

	// ******************* INTERNAL HELPERS ***************************

	private void createArguments(final MethodDeclaration declaration, final List arguments, boolean methodInvocation) throws JavaModelException {
		Assert.isNotNull(declaration);
		Assert.isNotNull(arguments);
		SingleVariableDeclaration variable= null;
		final int size= declaration.parameters().size();
		for (int index= 0; index < size; index++) {
			variable= (SingleVariableDeclaration) declaration.parameters().get(index);

			if (methodInvocation) {
				// we are creating method invocation parameters
				final SimpleName expression= getAst().newSimpleName(variable.getName().getIdentifier());
				arguments.add(expression);
			} else {
				// we are creating type info for the javadoc
				final MethodRefParameter parameter= getAst().newMethodRefParameter();
				parameter.setType(ASTNodeFactory.newType(getAst(), variable));
				if ((index == size - 1) && declaration.isVarargs())
					parameter.setVarargs(true);
				arguments.add(parameter);
			}
		}
	}

	private Block createDelegateMethodBody(final MethodDeclaration declaration) throws JavaModelException {
		Assert.isNotNull(declaration);

		MethodDeclaration old= (MethodDeclaration) getDeclaration();
		List arguments;
		Statement call;
		if (old.isConstructor()) {
			ConstructorInvocation invocation= getAst().newConstructorInvocation();
			arguments= invocation.arguments();
			call= invocation;
			fDelegateInvocation= invocation;
		} else {
			MethodInvocation invocation= getAst().newMethodInvocation();
			invocation.setName(getAst().newSimpleName(getNewElementName()));
			invocation.setExpression(getAccess());
			arguments= invocation.arguments();
			call= createMethodInvocation(declaration, invocation);
			fDelegateInvocation= invocation;
		}
		createArguments(declaration, arguments, true);

		final Block body= getAst().newBlock();
		body.statements().add(call);

		return body;
	}

	/**
	 * Creates a new expression statement for the method invocation.
	 * 
	 * @param invocation the method invocation
	 * @return the corresponding statement
	 */
	private ExpressionStatement createExpressionStatement(final MethodInvocation invocation) {
		Assert.isNotNull(invocation);
		return invocation.getAST().newExpressionStatement(invocation);
	}

	/**
	 * Creates a new return statement for the method invocation.
	 * 
	 * @param invocation the method invocation to create a return statement for
	 * @return the corresponding statement
	 */
	private ReturnStatement createReturnStatement(final MethodInvocation invocation) {
		Assert.isNotNull(invocation);
		final ReturnStatement statement= invocation.getAST().newReturnStatement();
		statement.setExpression(invocation);
		return statement;
	}
}