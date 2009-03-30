package org.testng.eclipse.launch.components;

import org.testng.eclipse.util.ResourceUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

/**
 * 
 * A Table with checkboxes to present group names.
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class CheckBoxTable extends SelectionStatusDialog {
  protected CheckboxTableViewer m_viewer;

  private String[] m_elements;
  private List m_selection = new ArrayList();
  
  public CheckBoxTable(Shell shell, Collection elements) {
    this(shell, (String[]) elements.toArray(new String[elements.size()]));
  }
  
  public CheckBoxTable(Shell shell, String[] elements) {
    super(shell);
    
    m_elements = elements;
    
    setSelectionResult(null);
    setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
    setBlockOnOpen(true);
    setTitle(ResourceUtil.getString("CheckBokTable.title"));
  }
  
  /**
   * Handles cancel button pressed event.
   */
  protected void cancelPressed() {
    setSelectionResult(null);
    super.cancelPressed();
  }
  
  /**
   * Creates the tree viewer.
   * 
   * @param parent the parent composite
   * @return the tree viewer
   */
  protected CheckboxTableViewer createTableViewer(Composite parent) {
    m_viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);

    m_viewer.setContentProvider(new GroupNamesContentProvider(m_elements));
    m_viewer.setLabelProvider(new GroupNameLabelProvider());

    m_viewer.setInput(m_elements);
    for (Iterator it = m_selection.iterator(); it.hasNext(); ) {
      m_viewer.setChecked(it.next().toString(), true);
    }

    return m_viewer;
  }
  
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    final CheckboxTableViewer tableViewer = createTableViewer(composite);

    GridData data = new GridData(GridData.FILL_BOTH);
    data.widthHint = convertWidthInCharsToPixels(60);
    data.heightHint = convertHeightInCharsToPixels(18);

    Table tableWidget = tableViewer.getTable();
    tableWidget.setLayoutData(data);
    tableWidget.setFont(composite.getFont());

    tableViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        Object element = event.getElement();
        boolean add = event.getChecked();
        
        if(add) {
          m_selection.add(element);
        } else {
          m_selection.remove(element);
        }
        
        updateOKStatus();
      }
    });

    applyDialogFont(composite);
    
    return composite;
  }
  
  protected void updateOKStatus() {
    computeResult();

    if(null != getResult()) {
      updateStatus(new StatusInfo());
    } else {
      updateStatus(new StatusInfo(IStatus.ERROR, "")); //$NON-NLS-1$
    }
  }
  
  public String[] getSelectedElements() {
    return (String[]) m_selection.toArray(new String[m_selection.size()]);
  }

  protected void computeResult() {
    setSelectionResult(m_selection.toArray());
  }
  
  private static class GroupNamesContentProvider implements IStructuredContentProvider {
    private String[] m_groupNames;
    
    public GroupNamesContentProvider(final String[] groups) {
      m_groupNames = groups;
    }
    
    public Object[] getElements(Object inputElement) {
      return m_groupNames;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }
  
  private static class GroupNameLabelProvider implements ILabelProvider {
    /**
     * Returns the image
     * 
     * @param arg0 the file
     * @return Image
     */
    public Image getImage(Object element) {
      return null;
    }

    /**
     * Returns the name of the file
     * 
     * @param arg0 the name of the file
     * @return String
     */
    public String getText(Object element) {
      return (String) element;
    }

    /**
     * Adds a listener
     * 
     * @param arg0 the listener
     */
    public void addListener(ILabelProviderListener listener) {
    // Throw it away
    }

    /**
     * Disposes any created resources
     */
    public void dispose() {
    // Nothing to dispose
    }

    /**
     * Returns whether changing this property for this element affects the label
     * 
     * @param arg0 the element
     * @param arg1 the property
     */
    public boolean isLabelProperty(Object arg0, String arg1) {
      return false;
    }

    /**
     * Removes a listener
     * 
     * @param arg0 the listener
     */
    public void removeListener(ILabelProviderListener arg0) {
    // Ignore
    }
  }

  public void checkElements(String[] elements) {
    for (int i = 0; i < elements.length; i++) {
      m_selection.add(elements[i]);
    }
  }
}

