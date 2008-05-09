package org.testng.eclipse.launch;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.CheckBoxTable;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.TestSearchEngine;

public class GroupSelector extends TestngTestSelector {

  private Map m_groupMap = new HashMap();

  GroupSelector(TestNGMainTab callback, Composite comp) {
    super();
    init(callback, createButtonHandler(), TestNGLaunchConfigurationConstants.GROUP, comp,
        "TestNGMainTab.label.group");
    //setTextEditable(false); // allow hand entry of group names
  }

  public void initializeFrom(ILaunchConfiguration configuration) {
    List groupNames = ConfigurationHelper.getGroups(configuration);
    setText(Utils.listToString(groupNames));
    m_groupMap.clear();
    List groupClassNames = ConfigurationHelper.getGroupClasses(configuration);
    groupNames = ConfigurationHelper.getGroups(configuration);
    if(null != groupNames) {
      for(int i = 0; i < groupNames.size(); i++) {
        m_groupMap.put(groupNames.get(i), groupClassNames);
      }
    }
  }

  public Map getGroupMap() {
    return m_groupMap;
  }

  private ButtonHandler createButtonHandler() {
    return new TestngTestSelector.ButtonHandler() {
      public void handleButton() {
        handleGroupSearchButtonSelected();
      };
    };
  }

  /**
  * Invoked when the Search button for groups is pressed.
  */
  public void handleGroupSearchButtonSelected() {
    Map groups = new HashMap();

    try {
      IJavaProject[] dependencies = new IJavaProject[0];
      try {
        String[] dependencyPrjNames = getCallback().getSelectedProject().getRequiredProjectNames();
        if(null != dependencyPrjNames) {
          dependencies = new IJavaProject[dependencyPrjNames.length];
          for(int i = 0; i < dependencyPrjNames.length; i++) {
            dependencies[i] = JDTUtil.getJavaProject(dependencyPrjNames[i]);
          }
        }
      }
      catch(JavaModelException jmex) {
        ; // ignore for the moment
      }

      Object[] projects = new Object[1 + dependencies.length];
      projects[0] = getCallback().getSelectedProject();
      System.arraycopy(dependencies, 0, projects, 1, dependencies.length);
      Object[] types = TestSearchEngine.findTests(getCallback().getLaunchConfigurationDialog(),
          projects, Filters.SINGLE_TEST);

      for(int i = 0; i < types.length; i++) {
        Object t = types[i];
        if(t instanceof IType) {
          IType type = (IType) t;
          ITestContent content = TypeParser.parseType(type);
          Collection groupNames = content.getGroups();
          if(!groupNames.isEmpty()) {
            for(Iterator it = groupNames.iterator(); it.hasNext();) {
              String groupName = (String) it.next();
              List rtypes = (List) groups.get(groupName);
              if(null == rtypes) {
                rtypes = new ArrayList();
                groups.put(groupName, rtypes);
              }

              rtypes.add(type.getFullyQualifiedName());
            }
          }
        }
      }
    }
    catch(InvocationTargetException e) {
      TestNGPlugin.log(e);
    }
    catch(InterruptedException e) {
      TestNGPlugin.log(e);
    }

    String[] uniqueGroups = (String[]) groups.keySet().toArray(new String[groups.size()]);
    Arrays.sort(uniqueGroups);
    final CheckBoxTable cbt = new CheckBoxTable(getCallback().getShell(), uniqueGroups);
    String content = getText();
    if(!Utils.isEmpty(content)) {
      List s = Utils.stringToList(content);
      String[] existingGroups = (String[]) s.toArray(new String[s.size()]);
      cbt.checkElements(existingGroups);
    }
    if(SelectionStatusDialog.CANCEL != cbt.open()) {
      String[] selectedGroups = cbt.getSelectedElements();

      m_groupMap = new HashMap();
      for(int i = 0; i < selectedGroups.length; i++) {
        m_groupMap.put(selectedGroups[i], groups.get(selectedGroups[i]));
      }

      setText(Utils.listToString(Arrays.asList(selectedGroups)));
    }

    getCallback().updateDialog();
  }

}
