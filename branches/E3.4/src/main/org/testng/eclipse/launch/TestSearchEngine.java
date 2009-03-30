/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.testng.eclipse.launch;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.testng.TestNG;
import org.testng.eclipse.launch.components.InternalAnnotationVisitor;

/**
 * Custom Search engine for suite() methods
 */
public class TestSearchEngine {
	
	private static final TestNGTestFinder FINDER = new TestNGTestFinder();
	
	public static TestNGTestFinder getFinder() {
		return FINDER;
	}

	public static boolean isTestOrTestSuite(IType declaringType) throws CoreException {
		return getFinder().isTest(declaringType);
	}	
	
	public static IType[] findTests(IRunnableContext context, final IJavaElement element) throws InvocationTargetException, InterruptedException {
		final Set result= new HashSet();

		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InterruptedException, InvocationTargetException {		
				try {
					getFinder().findTestsInContainer(element, result, pm);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		context.run(true, true, runnable);
		return (IType[]) result.toArray(new IType[result.size()]);
	}
	
	public static boolean isAccessibleClass(IType type) throws JavaModelException {
		int flags= type.getFlags();
		if (Flags.isInterface(flags)) {
			return false;
		}
		IJavaElement parent= type.getParent();
		while (true) {
			if (parent instanceof ICompilationUnit || parent instanceof IClassFile) {
				return true;
			}
			if (!(parent instanceof IType) || !Flags.isStatic(flags) || !Flags.isPublic(flags)) {
				return false;
			}
			flags= ((IType) parent).getFlags();
			parent= parent.getParent();
		}
	}
	
	public static boolean isAccessibleClass(ITypeBinding type) {
		if (type.isInterface()) {
			return false;
		}
		int modifiers= type.getModifiers();
		while (true) {
			if (type.getDeclaringMethod() != null) {
				return false;
			}
			ITypeBinding declaringClass= type.getDeclaringClass();
			if (declaringClass == null) {
				return true;
			}
			if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
				return false;
			}
			modifiers= declaringClass.getModifiers();
			type= declaringClass;
		}
	}

	public static boolean hasTestAnnotation(IJavaProject project) {
		try {
			return project != null && project.findType(InternalAnnotationVisitor.TESTNG_ANNOTATION_FULLNAME) != null;
		} catch (JavaModelException e) {
			// not available
		}
		return false;
	}
	
	public static IRegion getRegion(IJavaElement element) throws JavaModelException {
		IRegion result= JavaCore.newRegion();
		if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
			// for projects only add the contained source folders
			IPackageFragmentRoot[] roots= ((IJavaProject) element).getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				if (!roots[i].isArchive()) {
					result.add(roots[i]);
				}
			}
		} else {
			result.add(element);
		}
		return result;
	}
	
}
