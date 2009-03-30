package org.testng.eclipse.ui.util;

import org.testng.eclipse.launch.components.AnnotationVisitor;
import org.testng.eclipse.launch.components.BaseVisitor;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.launch.components.InternalAnnotationVisitor;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;


/**
 * This class parses an IType into an ITestContent
 * 
 * @author cbeust
 */
public class TypeParser {
  
  public static ITestContent parseType(IType type) {
	  return new InternalAnnotationVisitor(type);
  }
  
  public static void ppp(String s) {
    System.out.println("[TypeParser] " + s);
  }
}
