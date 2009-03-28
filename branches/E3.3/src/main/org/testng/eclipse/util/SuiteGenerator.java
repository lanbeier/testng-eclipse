package org.testng.eclipse.util;


import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.testng.xml.LaunchSuite;

/**
 * Factory to create custom suites.
 * 
 * @author Hani Suleiman
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class SuiteGenerator {
  public static LaunchSuite createProxiedXmlSuite(final File xmlSuitePath) {
    return new LaunchSuite.ExistingSuite(xmlSuitePath);
  }

  /**
   * 
   * @param projectName
   * @param packageNames
   * @param classNames
   * @param methodNames Map<String, Collection<String>>: classname -> collection of method names
   * @param groupNames
   * @param parameters
   * @param annotationType
   * @param logLevel
   * @return
   */
  public static LaunchSuite createCustomizedSuite(final String projectName,
                                                  final Collection packageNames,
                                                  final Collection classNames,
                                                  final Map methodNames,
                                                  final Collection groupNames,
                                                  final Map parameters,
                                                  final String annotationType,
                                                  final int logLevel) {
    if((null != groupNames) && !groupNames.isEmpty()) {
      return new GroupListSuite(projectName,
                                packageNames, 
                                classNames,
                                groupNames,
                                parameters,
                                annotationType,
                                logLevel);
    }
    else if(null != packageNames && !packageNames.isEmpty()) {
      return new PackageSuite(projectName,
                              packageNames, 
                              parameters,
                              annotationType,
                              logLevel);
    }
    else {
      return new ClassMethodsSuite(projectName,
                                   classNames,
                                   methodNames,
                                   parameters,
                                   annotationType,
                                   logLevel);
    }
  }
}