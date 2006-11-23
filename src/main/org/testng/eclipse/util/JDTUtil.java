package org.testng.eclipse.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;
import org.testng.eclipse.ui.RunInfo;

/**
 * Class offering utility method to access different Eclipse resources.
 *
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class JDTUtil {
  public static final String PROJECT_TYPE = "P";
  public static final String DIRECTORY_TYPE = "D";
  public static final String CLASS_TYPE = "C";
  public static final String SOURCE_TYPE = "J";
  public static final String SUITE_TYPE = "S";

  public static final int NO_CTX = 0;
  public static final int TEST_CLASS_CTX = 1;
  public static final int TEST_SUITE_CTX = 2;

  private JDTUtil() {
  }

  /**
   * Convenience method to get the workspace root.
   */
  public static IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  /**
   * Convenience method to get access to the java model.
   */
  public static IJavaModel getJavaModel() {
    return JavaCore.create(getWorkspaceRoot());
  }

  public static IJavaProject getJavaProject(String projectName) {
    if ((null == projectName) || (projectName.length() < 1)) {
      return null;
    }

    return getJavaModel().getJavaProject(projectName);
  }

  public static IJavaProject[] getJavaProjects() {
    IJavaProject[] projects = null;

    try {
      projects = JavaCore.create(getWorkspaceRoot()).getJavaProjects();
    }
    catch (JavaModelException jme) {
      projects = new IJavaProject[0];
    }

    return projects;
  }

  public static IJavaProject getJavaProjectContext() {
    IWorkbenchPage page = getActivePage();
    if (null != page) {
      ISelection selection = page.getSelection();
      if ((null != selection) && (selection instanceof IStructuredSelection)) {
        IStructuredSelection ss = (IStructuredSelection) selection;
        if (!ss.isEmpty()) {
          Object obj = ss.getFirstElement();

          if (obj instanceof IJavaElement) {
            return ((IJavaElement) obj).getJavaProject();
          }

          if (obj instanceof IResource) {
            IProject pro = ((IResource) obj).getProject();
            IJavaProject ijp = JavaCore.create(pro);

            return ijp;
          }
        }
      }
    }

    return null;
  }

  public static String getResourceType(IResource resource) {
    if (resource instanceof IProject) {
      return "P";
    }
    if (resource instanceof IFolder) {
      return "D";
    }

    IFile file = (IFile) resource;
    if ("class".equals(file.getFileExtension())) {
      return "C";
    }
    else if ("java".equals(file.getFileExtension())) {
      return "J";
    }
    else if ("xml".equals(file.getFileExtension())) {
      return "S";
    }

    return "";
  }

  private static IWorkbenchPage getActivePage() {
    IWorkbenchWindow window = TestNGPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();

    if (null != window) {
      return window.getActivePage();
    }

    return null;
  }

  public static int getResourceContextType() {
    IWorkbenchPage page = getActivePage();

    if (null != page) {
      ISelection selection = page.getSelection();

      if ((null != selection) && (selection instanceof IStructuredSelection)) {
        IStructuredSelection ss = (IStructuredSelection) selection;

        if (!ss.isEmpty()) {
          Object obj = ss.getFirstElement();

          if (obj instanceof IJavaElement) {
            IJavaElement ije = (IJavaElement) obj;
            ppp("getResourceContextType():IJavaElement: " + ije.getElementName());
            ppp("getResourceContextType():Element type: " + ije.getElementType());

            if (IJavaElement.COMPILATION_UNIT <= ije.getElementType()) {
              return TEST_CLASS_CTX;
            }
            else {
              return NO_CTX;
            }
          }

          if (obj instanceof IResource) {
            IResource ir = (IResource) obj;

            if (IResource.FILE == ir.getType()) {
              if ("class".equals(ir.getFileExtension())) { // IClassFile
                return TEST_CLASS_CTX;
              }
              else if ("xml".equals(ir.getFileExtension())) {
                return TEST_SUITE_CTX;
              }

              return NO_CTX;
            }
            else {
              return NO_CTX;
            }
          }
        }
      }
    }

    return NO_CTX;
  }

  public static ICompilationUnit getJavaElement(IFile file) {
    IJavaElement ije = JavaCore.create(file);

    return (ICompilationUnit) ije.getAncestor(IJavaElement.COMPILATION_UNIT);
  }

  /**
   * @return
   */
  public static Object getResourceContext() {
    IWorkbenchPage page = getActivePage();

    if (null != page) {
      ISelection selection = page.getSelection();

      if ((null != selection) && (selection instanceof IStructuredSelection)) {
        IStructuredSelection ss = (IStructuredSelection) selection;

        if (!ss.isEmpty()) {
          Object obj = ss.getFirstElement();

          if (obj instanceof IJavaElement) {
            IJavaElement ije = (IJavaElement) obj;
            ppp("IJavaElement: " + ije.getElementName());
            ppp("Element type: " + ije.getElementType());
            if (IJavaElement.COMPILATION_UNIT <= ije.getElementType()) {
              return ije.getAncestor(IJavaElement.COMPILATION_UNIT);
            }
          }

          if (obj instanceof IResource) {
            IResource ir = (IResource) obj;

            if (IResource.FILE == ir.getType()) {
              if ("class".equals(ir.getFileExtension())) {
                IJavaElement ije = JavaCore.create((IFile) ir);

                return ije.getAncestor(IJavaElement.COMPILATION_UNIT);
              }
              else if ("xml".equals(ir.getFileExtension())) {
                return ir;
              }
            }
          }
        }
      }
    }

    return null;
  }

  private static void ppp(final Object msg) {
    System.out.println("[JDTUtil]: " + msg);
  }

  /**
   * Returns a list of files (List<File>) containing the source directories
   * defined in the specified project.
   *
   * @param jproject
   * @return
   */
  public static List getSourceDirFileList(IJavaProject jproject) {
    if ((null == jproject) || !jproject.exists()) {
      return Collections.EMPTY_LIST;
    }

    List sourcePaths = new ArrayList();

    try {
      IPackageFragmentRoot[] sourceRoots = jproject.getAllPackageFragmentRoots();

      for (int i = 0; i < sourceRoots.length; i++) {
        if (IPackageFragmentRoot.K_SOURCE == sourceRoots[i].getKind()) {
          IResource sourceRes = sourceRoots[i].getCorrespondingResource();
          if (null != sourceRes) {
            sourcePaths.add(sourceRes.getLocation().toFile());
          }
        }
      }
    }
    catch (JavaModelException jme) {
      TestNGPlugin.log(jme);
    }

    return sourcePaths;
  }

  public static String getProjectVMVersion(IJavaProject ijp) {
    if(null == ijp) {
      return "";
    }
    
    try {
      IType itype = ijp.findType("java.lang.annotation.Annotation");

      return (itype == null) ? TestNGLaunchConfigurationConstants.JDK14_COMPLIANCE
                             : TestNGLaunchConfigurationConstants.JDK15_COMPLIANCE;
    }
    catch (JavaModelException jme) {
      TestNGPlugin.log(jme);
    }

    return "";
  }
  
  public static IJavaElement findElement(IJavaProject javaProject, RunInfo runInfo) 
  throws JavaModelException {
    IType type = javaProject.findType(runInfo.getClassName());
    if(null == type) {
      return null;
    }

    if(null == runInfo.getMethodName()) {
      return type;
    }

    String[] paramTypes= runInfo.getParameterTypes();
    if(null == paramTypes) {
      paramTypes= new String[0];
    }
    List params= new ArrayList(paramTypes.length);
    for(int i= 0; i < paramTypes.length; i++) {
      int idx= paramTypes[i].lastIndexOf('.');
      String typeName= idx == -1 ? paramTypes[i] : paramTypes[i].substring(idx + 1);
      params.add(Signature.createTypeSignature(typeName, false));
    }
    IMethod method= findMethodInTypeHierarchy(type, runInfo.getMethodName(), (String[]) params.toArray(new String[paramTypes.length]));
    if(null == method) {
      method = fuzzyFindMethodInTypeHierarchy(type, runInfo.getMethodName(), paramTypes);
    }
    
    return method;
  }
  
  public static IJavaElement findElement(IJavaProject javaProject, String className) 
  throws JavaModelException {
    return javaProject.findType(className);
  }
  
  /**
   * Retrieves in the <code>IJavaProject</code> the class or the method element.
   * @param javaProject
   * @param className
   * @param methodName
   * @return
   * @throws JavaModelException
   */
//  public static IJavaElement findElement(IJavaProject javaProject, String className, String methodName) 
//  throws JavaModelException {
//    IType type = javaProject.findType(className);
//    if(null == type) {
//      return null;
//    }
//
//    if(null == methodName) {
//      return type;
//    }
//
//    // FIXME: we need some work here
//    String finalMethodName = methodName.indexOf('(') == -1 ? methodName : methodName.substring(0, methodName.indexOf('('));
//    
//    IMethod method = findMethodInTypeHierarchy(type, finalMethodName);
//    if (null == method) {
//      method = fuzzyFindMethodInTypeHierarchy(type, finalMethodName);
//    }
//
//    return method;
//  }
  
  private static IMethod findMethodInTypeHierarchy(IType type, String methodName, String[] paramTypes) throws JavaModelException {
    IMethod method = type.getMethod(methodName, paramTypes);
    if(method != null && method.exists()) { 
        return method;
    }

    ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
    IType[] types = typeHierarchy.getAllSuperclasses(type);
    for(int i = 0; i < types.length; i++) {
      method = types[i].getMethod(methodName, paramTypes);
      if((method != null) && method.exists()) {
        return method;
      }
    }

    return null;
  }

  private static IMethod fuzzyFindMethodInTypeHierarchy(IType type, String methodName, String[] paramTypes) throws JavaModelException {
    IMethod[] methods = type.getMethods();
    for(int i = 0; i < methods.length; i++) {
      if(methodName.equals(methods[i].getElementName()) && methods[i].exists() && methods[i].getNumberOfParameters() == paramTypes.length) {
        return methods[i];
      }
    }

    ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
    IType[] types = typeHierarchy.getAllSuperclasses(type);
    for(int i = 0; i < types.length; i++) {
      methods = types[i].getMethods();
      for(int j = 0; j < methods.length; j++) {
        if(methodName.equals(methods[j].getElementName()) && methods[j].exists()) {
          return methods[j];
        }
      }
    }

    return null;
  }
  
  /**
   * Tries to retrieve the dependsOn part of a method definition.
   * @param method
   * @param allMethods
   */
  public static void solveDependencies(IMethod method, Map allMethods) {
    try {
      DependencyVisitor dv= new DependencyVisitor();
      ASTParser parser= ASTParser.newParser(AST.JLS3);
      parser.setSource(method.getSource().toCharArray());
      parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
      ASTNode node= parser.createAST(null);
      node.accept(dv);
      if(!dv.dependsOnMethods.isEmpty()) {
        for(int i= 0; i < dv.dependsOnMethods.size(); i++) {
          String methodName= (String) dv.dependsOnMethods.get(i);
          if(!allMethods.containsKey(methodName)) {
            IMethod meth= solveMethod(method.getDeclaringType(), methodName);
            allMethods.put(methodName, meth);
            solveDependencies(meth, allMethods);
          }
        }
      }
      if(!dv.dependsOnGroups.isEmpty()) {
        ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
            "WARNING", 
            "Method defines group dependencies that will be ignored", 
            new Status(IStatus.WARNING, TestNGPlugin.PLUGIN_ID, 3333, 
                "Method " + method.getElementName() + " defines the following group dependencies " + dv.dependsOnGroups
                + " which due to a plugin limitation will be ignored", null));
      }
    }
    catch(JavaModelException jme) {
      ; //ignore
    }
  }
  
  private static IMethod solveMethod(IType type, String methodName) {
    try {
      IMethod[] typemethods= type.getMethods();
      
      for(int i=0; i < typemethods.length; i++) {
        if(methodName.equals(typemethods[i].getElementName())) {
          return typemethods[i];
        }
      }
      
      ITypeHierarchy typeHierarchy= type.newSupertypeHierarchy(null);
      IType[] superTypes= typeHierarchy.getAllSuperclasses(type);
      for(int i= 0; i < superTypes.length; i++) {
        IMethod[] methods= superTypes[i].getMethods();
        
        for(int j=0; j < methods.length; j++) {
          if(methodName.equals(methods[j].getElementName())) {
            return methods[j];
          }
        }
      }
    }
    catch(JavaModelException jme) {
      ; //ignore
    }
    
    return null;
  }
  
  private static class DependencyVisitor extends ASTVisitor {
    private static final String ANNOTATION_PACKAGE = "org.testng.annotations.";
    private static final String TEST_ANNOTATION = "Test";
    private static final String TEST_ANNOTATION_FQN = ANNOTATION_PACKAGE + TEST_ANNOTATION;
    List dependsOnMethods= new ArrayList();
    List dependsOnGroups= new ArrayList();

    public boolean visit(NormalAnnotation annotation) {
      if(!TEST_ANNOTATION.equals(annotation.getTypeName().getFullyQualifiedName()) 
          && !TEST_ANNOTATION_FQN.equals(annotation.getTypeName().getFullyQualifiedName())) {
        return false;
      }
      
      List values= annotation.values();
      
      if(null != values && !values.isEmpty()) {
        for(int i= 0; i < values.size(); i++) {
          MemberValuePair pair= (MemberValuePair) values.get(i);
          if("dependsOnMethods".equals(pair.getName().toString())) {
            dependsOnMethods.addAll(extractValues(pair.getValue()));
          }
          else if("dependsOnGroups".equals(pair.getName().toString())) {
            dependsOnGroups.addAll(extractValues(pair.getValue()));
          }
        }
      }
      
      return false;
    }
    
    private List extractValues(Expression paramAttr) {
      List values= new ArrayList();
      if(paramAttr instanceof ArrayInitializer) {
        List literals= ((ArrayInitializer) paramAttr).expressions();
        List paramNames= new ArrayList(literals.size());
        for(int j= 0; j < literals.size(); j++) {
          StringLiteral str= (StringLiteral) literals.get(j);
          values.add(str.getLiteralValue());
        }
      }

      return values;
    }
  }

}
