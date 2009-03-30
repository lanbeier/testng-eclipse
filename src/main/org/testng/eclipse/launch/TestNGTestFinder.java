/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.testng.eclipse.launch;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.testng.eclipse.launch.components.InternalAnnotationVisitor;
import org.testng.eclipse.ui.util.TypeParser;


public class TestNGTestFinder {
		
	public void findTestsInContainer(IJavaElement element, Set result, IProgressMonitor pm) throws CoreException {
		if (element == null || result == null) {
			throw new IllegalArgumentException();
		}
		
		if (element instanceof IType) {
			//TODO: check for type annotation?
		}

		if (pm == null)
			pm= new NullProgressMonitor();
		
		try {
			pm.beginTask("Searching for tests", 4);
			
			IRegion region= TestSearchEngine.getRegion(element);
			ITypeHierarchy hierarchy= JavaCore.newTypeHierarchy(region, null, new SubProgressMonitor(pm, 1));
			IType[] allClasses= hierarchy.getAllClasses();
			
			// search for all types with references to RunWith and Test and all subclasses
			HashSet candidates= new HashSet(allClasses.length);
			SearchRequestor requestor= new AnnotationSearchRequestor(hierarchy, candidates);
			
			IJavaSearchScope scope= SearchEngine.createJavaSearchScope(allClasses, IJavaSearchScope.SOURCES);
			int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			SearchPattern testPattern= SearchPattern.createPattern(InternalAnnotationVisitor.TESTNG_ANNOTATION_FULLNAME, IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, matchRule);
			
			SearchPattern annotationsPattern= testPattern;
			SearchParticipant[] searchParticipants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
			new SearchEngine().search(annotationsPattern, searchParticipants, scope, requestor, new SubProgressMonitor(pm, 2));
			
			// find all classes in the region
			for (Iterator iterator= candidates.iterator(); iterator.hasNext();) {
				IType curr= (IType) iterator.next();
				if (TestSearchEngine.isAccessibleClass(curr) && !Flags.isAbstract(curr.getFlags()) && region.contains(curr)) {
					result.add(curr);
				}
			}
						
		} finally {
			pm.done();
		}
	}

	private static class AnnotationSearchRequestor extends SearchRequestor {
		
		private final Collection fResult;
		private final ITypeHierarchy fHierarchy;

		public AnnotationSearchRequestor(ITypeHierarchy hierarchy, Collection result) {
			fHierarchy= hierarchy;
			fResult= result;
		}

		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
				Object element= match.getElement();
				if (element instanceof IType || element instanceof IMethod) {
					IMember member= (IMember) element;
					if (member.getNameRange().getOffset() > match.getOffset()) {
						IType type= member.getElementType() == IJavaElement.TYPE ? (IType) member : member.getDeclaringType();
						addTypeAndSubtypes(type);
					}
				}
			}
		}

		private void addTypeAndSubtypes(IType type) {
			if (fResult.add(type)) {
				IType[] subclasses= fHierarchy.getSubclasses(type);
				for (int i= 0; i < subclasses.length; i++) {
					addTypeAndSubtypes(subclasses[i]);
				}
			}
		}
	}
	
	public boolean isTest(IType type) throws JavaModelException {
		if (TestSearchEngine.isAccessibleClass(type)) {
			return TypeParser.parseType(type).isTestNGClass();
		}
		return false;
	}
		

}
