package org.testng.eclipse.launch.components;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGMainTab;
import org.testng.eclipse.util.signature.MethodDescriptor;

public class InternalAnnotationVisitor implements ITestContent {

	private IAnnotation fTypeAnnotation;

	private IMemberValuePair[] fvaluePairs;

	private Set fGroups = new HashSet();

	private boolean fTypeIsTest;

	private Set fTestMethods = new HashSet();

	private Collection fFactoryMethods = new HashSet();

	public static final String TESTNG_ANNOTATION_PACKAGE = "org.testng.annotations";

	public static final String TESTNG_ANNOTATION_NAME = "Test";

	public static final String TESTNG_ANNOTATION_NAME_FACTORY = "Factory";

	public InternalAnnotationVisitor(IType type) {
		try {
			ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
			findTypeAnnotation(type, hierarchy);
			if (fTypeAnnotation != null) {
				analyzeAnnotation(fTypeAnnotation);
				fTypeIsTest = true;
			}
			analyzeMethods(type);
		} catch (JavaModelException e) {
			TestNGPlugin.log(e);
			e.printStackTrace(System.err);
		}

	}

	private void analyzeMethods(IType type) throws JavaModelException {
		IMethod[] methods = type.getMethods();
		for (int il = 0; il < methods.length; il++) {
			analyzeMethod(methods[il], type);
		}

	}

	private void analyzeMethod(IMethod method, IType type)
			throws JavaModelException {
		if (!Flags.isPublic(method.getFlags()))
			return;
		if (findTestNGAnnotation(method, type, TESTNG_ANNOTATION_NAME_FACTORY) != null) {
			fFactoryMethods.add(new MethodDescriptor(method));
			return;
		}
		if (!fTypeIsTest) {
			if (findTestNGAnnotation(method, type, TESTNG_ANNOTATION_NAME) != null)
				fTestMethods.add(new MethodDescriptor(method));
		} else
			fTestMethods.add(new MethodDescriptor(method));
	}

	private void analyzeAnnotation(IAnnotation typeAnnotation)
			throws JavaModelException {
		fvaluePairs = typeAnnotation.getMemberValuePairs();
		for (int il = 0; il < fvaluePairs.length; il++) {
			IMemberValuePair fvaluePair = fvaluePairs[il];
			if ("groups".equals(fvaluePair.getMemberName())) {
				Object value = fvaluePair.getValue();
				if (value instanceof String)
					fGroups.add(value);
				if (value instanceof String[]) {
					String[] values = (String[]) value;
					fGroups.addAll(Arrays.asList(values));
				}
			}
		}
	}

	private IAnnotation findTestNGAnnotation(IAnnotatable annotatable,
			IType type, String name) throws JavaModelException {
		IAnnotation[] annotations = annotatable.getAnnotations();
		for (int il = 0; il < annotations.length; il++) {
			IAnnotation annotation = annotations[il];
			String[][] resolved = type.resolveType(annotation.getElementName());
			if (resolved.length > 0) {
				String pack = resolved[0][0];
				String clazz = resolved[0][1];
				if (TESTNG_ANNOTATION_PACKAGE.equals(pack)
						&& name.equals(clazz))
					return annotation;
			}
		}
		return null;
	}

	private void findTypeAnnotation(IType type, ITypeHierarchy hierarchy)
			throws JavaModelException {
		if (type == null)
			return;
		if (fTypeAnnotation != null)
			return;
		fTypeAnnotation = findTestNGAnnotation(type, type,
				TESTNG_ANNOTATION_NAME);
		try {
			String superClass = type.getSuperclassName();
			if (superClass == null)
				return;
			if (fTypeAnnotation == null)
				findTypeAnnotation(hierarchy.getSuperclass(type), hierarchy);
		} catch (JavaModelException e) {
			System.err.println(e);
			TestNGPlugin.log(e);
		}

	}

	public String getAnnotationType() {
		return fTypeAnnotation.getElementName();
	}

	public Collection getGroups() {
		return fGroups;
	}

	public Set getTestMethods() {
		return fTestMethods;
	}

	public boolean hasTestMethods() {
		return fTypeIsTest || fTestMethods.size() > 0
				|| fFactoryMethods.size() > 0;
	}

	public boolean isTestMethod(IMethod imethod) {
		return fTestMethods.contains(new MethodDescriptor(imethod));
	}

	public boolean isTestNGClass() {
		return fTestMethods.size() > 0 || fFactoryMethods.size() > 0;
	}

}
