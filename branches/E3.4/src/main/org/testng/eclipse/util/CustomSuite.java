package org.testng.eclipse.util;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jdt.core.IJavaProject;
import org.testng.TestNG;
import org.testng.reporters.XMLStringBuffer;
import org.testng.xml.LaunchSuite;
import org.testng.xml.Parser;

/**
 * A custom suite generator.
 *
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
abstract public class CustomSuite extends LaunchSuite {
  protected String m_projectName;
  protected String m_suiteName;
  protected String m_annotationType;
  protected Map m_parameters;
  protected int m_logLevel;

  private XMLStringBuffer m_suiteBuffer;

  public CustomSuite(final IJavaProject project,
                     final String suiteName,
                     final Map parameters,
                     final String annotationType,
                     final int logLevel) {
    super(true);

    m_projectName= project.getProject().getName();
    if (suiteName == null)
    	m_suiteName = m_projectName;
    else
    	m_suiteName= suiteName;
    m_parameters= parameters;

    // TODO: move to AnnotationTypeEnum ?
    if("1.4".equals(annotationType) || TestNG.JAVADOC_ANNOTATION_TYPE.equals(annotationType)) {
      m_annotationType= TestNG.JAVADOC_ANNOTATION_TYPE;
    }
    else {
      m_annotationType= TestNG.JDK_ANNOTATION_TYPE;
    }
    
    m_logLevel= logLevel;
  }

  abstract protected String getTestName();

  protected String getSuiteName() {
    return m_suiteName;
  }

  protected XMLStringBuffer createContentBuffer() {
    XMLStringBuffer suiteBuffer= new XMLStringBuffer(""); //$NON-NLS-1$
    suiteBuffer.setDocType("suite SYSTEM \"" + Parser.TESTNG_DTD_URL + "\"");

    Properties attrs= new Properties();
    attrs.setProperty("name", getSuiteName());
    suiteBuffer.push("suite", attrs);

    if(m_parameters != null) {
      for(Iterator it= m_parameters.entrySet().iterator(); it.hasNext();) {
        Map.Entry entry= (Map.Entry) it.next();
        Properties paramAttrs= new Properties();
        paramAttrs.setProperty("name", (String) entry.getKey());
        paramAttrs.setProperty("value", (String) entry.getValue());
        suiteBuffer.addEmptyElement("parameter", paramAttrs);
      }
    }

    initContentBuffer(suiteBuffer);

    suiteBuffer.pop("suite");

    return suiteBuffer;
  }

  private XMLStringBuffer getSuiteBuffer() {
    if(null == m_suiteBuffer) {
      m_suiteBuffer= createContentBuffer();
    }

    return m_suiteBuffer;
  }

  /** 
   * Generate the current suite to a file.
   * @see org.testng.xml.LaunchSuite#save(java.io.File)
   */
  public File save(File directory) {
    final File suiteFile= new File(directory, "temp-testng-customsuite.xml");

    saveSuiteContent(suiteFile, getSuiteBuffer());

    return suiteFile;
  }

  protected void saveSuiteContent(final File file, final XMLStringBuffer content) {
    FileWriter fw= null;
    BufferedWriter bw= null;
    try {
      fw= new FileWriter(file);
      bw= new BufferedWriter(fw);
      bw.write(content.getStringBuffer().toString());
      bw.flush();
    }
    catch(IOException ioe) {
    }
    finally {
      if(null != bw) {
        try {
          bw.close();
        }
        catch(IOException ioe) {
        }
      }
      if(null != fw) {
        try {
          fw.close();
        }
        catch(IOException ioe) {
        }
      }
    }
  }

  protected void initContentBuffer(XMLStringBuffer suiteBuffer) {
    Properties testAttrs= new Properties();
    testAttrs.setProperty("name", getTestName());
    if(m_annotationType != null) {
      testAttrs.setProperty("annotations", m_annotationType);
    }
    testAttrs.setProperty("verbose", String.valueOf(m_logLevel));

    suiteBuffer.push("test", testAttrs);

    groupsElement(suiteBuffer);

    packagesElement(suiteBuffer);

    classesElement(suiteBuffer);

    suiteBuffer.pop("test");
  }

  /**
   * Override to generate the groups element.
   */
  protected void groupsElement(XMLStringBuffer suiteBuffer) {
  }

  /**
   * Override to generate the packages element.
   */
  protected void packagesElement(XMLStringBuffer suiteBuffer) {
  }

  /**
   * Override to generate the classes element.
   */
  protected void classesElement(XMLStringBuffer suiteBuffer) {
  }

  /**
   * An utility method that generates the groups element using the passed in 
   * collection of names (order of group names ia not important).
   */
  protected void generateDefaultGroupsElement(XMLStringBuffer suiteBuffer, Collection /*<String>*/ groupNames) {
    if((null == groupNames) || groupNames.isEmpty()) {
      return;
    }

    suiteBuffer.push("groups");
    suiteBuffer.push("run");

    for(Iterator it= groupNames.iterator(); it.hasNext();) {
      String groupName= (String) it.next();
      Properties includeAttrs= new Properties();
      includeAttrs.setProperty("name", groupName);
      suiteBuffer.addEmptyElement("include", includeAttrs);
    }

    suiteBuffer.pop("run");
    suiteBuffer.pop("groups");
  }

  protected void generateDefaultPackagesElement(XMLStringBuffer suiteBuffer,
                                                Collection /*<String>*/ packageNames) {
    if((null == packageNames) || packageNames.isEmpty()) {
      return;
    }

    suiteBuffer.push("packages");

    for(Iterator it= packageNames.iterator(); it.hasNext();) {
      String packageName= (String) it.next();
      Properties packageAttrs= new Properties();
      packageAttrs.setProperty("name", packageName);
      suiteBuffer.addEmptyElement("package", packageAttrs);
    }

    suiteBuffer.pop("packages");
  }

  protected void generateDefaultClassesElement(XMLStringBuffer suiteBuffer,
                                               Collection /*<String>*/ classNames) {
    if((null == classNames) || classNames.isEmpty()) {
      return;
    }

    suiteBuffer.push("classes");

    for(Iterator it= classNames.iterator(); it.hasNext();) {
      String className= (String) it.next();
      Properties classAttrs= new Properties();
      classAttrs.setProperty("name", className);
      suiteBuffer.addEmptyElement("class", classAttrs);
    }

    suiteBuffer.pop("classes");
  }
}