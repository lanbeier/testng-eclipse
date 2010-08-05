package org.testng.eclipse.launch.components;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Iterator;
import java.util.List;

/**
 * An AST visitor to collect all the groups defined in a compilation unit.
 * This visitor extends JavaDocVisitor so it's able to visit both annotations
 * and javadoc annotations, so maybe it should actually be renamed.
 * 
 * @author cbeust
 */
public class AnnotationVisitor extends BaseVisitor {
  
  @Override
  public boolean visit(MethodDeclaration node) {
    if(m_typeIsTest) {
      addTestMethod(node, JDK15_ANNOTATION);
//      return false; // no need to continue
    }
    
    return true;
  }

  @Override
  public boolean visit(MarkerAnnotation node) {
    ASTNode parent = node.getParent();
    if (isTestAnnotation(node.getTypeName().toString())) {
      if (parent instanceof MethodDeclaration) {
        addTestMethod((MethodDeclaration) parent, JDK15_ANNOTATION);
      }
      else if (parent instanceof TypeDeclaration) { // TESTNG-24
        m_typeIsTest = true;
        m_annotationType = JDK15_ANNOTATION;
      }
    }
    else if (isFactoryAnnotation(node.getTypeName().toString())) {
      if (parent instanceof MethodDeclaration) {
        m_annotationType = JDK15_ANNOTATION;
        addFactoryMethod((MethodDeclaration) parent, JDK15_ANNOTATION);
      }
    }
    
    return false;
  }
  
  @Override
  public boolean visit(NormalAnnotation node) {
    //
    // Test method?
    //
    if(isTestAnnotation(node.getTypeName().toString())) {
      ASTNode parent = node.getParent();
      if (parent instanceof MethodDeclaration) {
        addTestMethod((MethodDeclaration) parent, JDK15_ANNOTATION);
      } else if(parent instanceof TypeDeclaration) {
        m_typeIsTest = true;
        m_annotationType = JDK15_ANNOTATION;
      }
      
      List pairs = node.values();
      for (Iterator it = pairs.iterator(); it.hasNext(); ) {
        MemberValuePair mvp = (MemberValuePair) it.next();
        Name attribute = mvp.getName();
        String name = attribute.getFullyQualifiedName();
        if ("groups".equals(name)) {
          Expression value = mvp.getValue();
          // Array?
          if (value instanceof ArrayInitializer) {
            ArrayInitializer ai = (ArrayInitializer) value;
            List expressions = ai.expressions();
            for (Iterator it2 = expressions.iterator(); it2.hasNext(); ) {
              Expression e = (Expression) it2.next();
              addGroup(e.toString());
            }
          }
          else if (value instanceof SimpleName) {
            Object boundValue = value.resolveConstantExpressionValue();
            addGroup(boundValue.toString());
          }
          else if(value instanceof StringLiteral) {
            addGroup(value.toString());
          }
        }
      }
    }
    else if (isFactoryAnnotation(node.getTypeName().toString())) {
      if (node.getParent() instanceof MethodDeclaration) {
        m_annotationType = JDK15_ANNOTATION;
        addFactoryMethod((MethodDeclaration) node.getParent(), JDK15_ANNOTATION);
      }
    }

    return false;
  }
  
  public boolean isTestAnnotation(String annotation) {
    return "Test".equals(annotation) || annotation.endsWith(".Test");
  }
  
  public boolean isFactoryAnnotation(String annotation) {
    return "Factory".equals(annotation) || annotation.endsWith(".Factory");    
  }
  
  public static void ppp(String s) {
    System.out.println("[AnnotationVisitor] " + s);
  }

}
