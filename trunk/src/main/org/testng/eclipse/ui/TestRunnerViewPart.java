/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julien Ruaux: jruaux@octo.com see bug 25324 Ability to know when tests are finished [junit]
 *     Vincent Massol: vmassol@octo.com 25324 Ability to know when tests are finished [junit]
 *     Sebastian Davids: sdavids@gmx.de 35762 JUnit View wasting a lot of screen space [JUnit]
 *     
 * Modified by:
 *     Alexandru Popescu: the_mindstorm@evolva.ro
 ******************************************************************************/
package org.testng.eclipse.ui;


import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;
import org.testng.ITestResult;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.LaunchUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.remote.RemoteTestNG;
import org.testng.remote.strprotocol.GenericMessage;
import org.testng.remote.strprotocol.IRemoteSuiteListener;
import org.testng.remote.strprotocol.IRemoteTestListener;
import org.testng.remote.strprotocol.SuiteMessage;
import org.testng.remote.strprotocol.TestMessage;
import org.testng.remote.strprotocol.TestResultMessage;
import org.testng.reporters.FailedReporter;

/**
 * A ViewPart that shows the results of a test run.
 */
public class TestRunnerViewPart extends ViewPart 
implements IPropertyChangeListener, IRemoteSuiteListener, IRemoteTestListener {

  //orientations
  static final int VIEW_ORIENTATION_VERTICAL = 0;
  static final int VIEW_ORIENTATION_HORIZONTAL = 1;
  static final int VIEW_ORIENTATION_AUTOMATIC = 2;

  /** used by IWorkbenchSiteProgressService */
  private static final Object FAMILY_RUN = new Object();

  /** set from IPartListener2 part lifecycle listener. */
  protected boolean m_partIsVisible = false;

  /** store the state. */ 
  private IMemento m_stateMemento;
  
  /** 
   * The launcher that has started the test.
   * May be used for reruns. 
   */
  private ILaunch m_LastLaunch;

  /** The launched project */
  private IJavaProject m_workingProject;
  
  /** status text. */
  protected volatile String  m_statusMessage;
  
  // view components
  private Composite   m_parentComposite;
  private CTabFolder m_tabFolder;
  
  /** The collection of TestRunTab. */
  protected Vector m_tabsList = new Vector();

  /** The currently active run tab. */
  private TestRunTab m_activeRunTab;

  private FailureTrace m_failureTraceComponent;
  
  private SashForm   m_sashForm;

  protected CounterPanel     m_counterPanel;
  private Composite   m_counterComposite;
  
  final Image m_viewIcon = TestNGPlugin.getImageDescriptor("main16/testng_noshadow.gif").createImage();//$NON-NLS-1$
  final Image fStackViewIcon = TestNGPlugin.getImageDescriptor("eview16/stackframe.gif").createImage(); //$NON-NLS-1$

  /**
   * Actions
   */
  private Action fNextAction;
  private Action fPrevAction;
  private ToggleOrientationAction[] fToggleOrientationActions;
  private Action m_rerunAction;
  private Action m_rerunFailedAction;
  private Action m_openReportAction;
  private boolean m_hasFailures;
  
  private long m_startTime;
  private long m_stopTime;
  
  /**
   * Whether the output scrolls and reveals tests as they are executed.
   */
  protected boolean fAutoScroll = true;

  /**
   * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
   * <code>VIEW_ORIENTATION_VERTICAL</code>, or <code>VIEW_ORIENTATION_AUTOMATIC</code>.
   */
  private int fOrientation = VIEW_ORIENTATION_AUTOMATIC;

  /**
   * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
   * <code>VIEW_ORIENTATION_VERTICAL</code>.
   */
  private int fCurrentOrientation;

  protected JUnitProgressBar fProgressBar;

  private Color fOKColor;
  private Color fFailureColor;

  
  private boolean m_isDisposed = false;
  
  // JOBS
  private UpdateUIJob m_updateUIJob;
  /**
   * A Job that runs as long as a test run is running. 
   * It is used to get the progress feedback for running jobs in the view.
   */
  private IsRunningJob m_isRunningJob;
  private ILock        m_runLock;
  private boolean      m_testIsRunning = false;
  
  /**
   * Queue used for processing Tree Entries
   */
  private List m_treeEntriesQueue = new ArrayList();
  
  /**
   * Indicates an instance of TreeEntryQueueDrainer is already running, or scheduled to
   */
  private boolean fQueueDrainRequestOutstanding;
  
  public static final String NAME = "org.testng.eclipse.ResultView"; //$NON-NLS-1$
  public static final String ID_EXTENSION_POINT_TESTRUN_TABS = TestNGPlugin.PLUGIN_ID + "." //$NON-NLS-1$
      + "internal_testRunTabs";  //$NON-NLS-1$

  static final int REFRESH_INTERVAL = 200;

  // Persistence tags.
  static final String TAG_PAGE = "page"; //$NON-NLS-1$
  static final String TAG_RATIO = "ratio"; //$NON-NLS-1$
  static final String TAG_ORIENTATION = "orientation"; //$NON-NLS-1$

  
  //~ counters
  protected int m_suitesTotalCount;
  protected int m_testsTotalCount;
  protected int m_methodTotalCount;
  protected volatile int m_suiteCount;
  protected volatile int m_testCount;
  protected volatile int m_methodCount;
  protected volatile int m_passedCount;
  protected volatile int m_failedCount;
  protected volatile int m_skippedCount;
  protected volatile int m_successPercentageFailed;
  
  /**
   * The client side of the remote test runner
   */
  private EclipseTestRunnerClient fTestRunnerClient;
  

  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);
    m_stateMemento = memento;

    IWorkbenchSiteProgressService progressService = getProgressService();
    if(progressService != null) {
      progressService.showBusyForFamily(TestRunnerViewPart.FAMILY_RUN);
    }
  }

  private IWorkbenchSiteProgressService getProgressService() {
    Object siteService = getSite().getAdapter(IWorkbenchSiteProgressService.class);
    if(siteService != null) {
      return (IWorkbenchSiteProgressService) siteService;
    }

    return null;
  }


  private void restoreLayoutState(IMemento memento) {
    Integer page = memento.getInteger(TAG_PAGE);
    if(page != null) {
      int p = page.intValue();
      m_tabFolder.setSelection(p);
      m_activeRunTab = (TestRunTab) m_tabsList.get(p);
    }

    Integer ratio = memento.getInteger(TAG_RATIO);
    if(ratio != null) {
      m_sashForm.setWeights(new int[] { ratio.intValue(), 1000 - ratio.intValue() });
    }

    Integer orientation = memento.getInteger(TAG_ORIENTATION);
    if(orientation != null) {
      fOrientation = orientation.intValue();
    }
    computeOrientation();
  }

  /**
   * Stops the currently running test and shuts down the RemoteTestRunner.
   */
  private void stopTest() {
    if(null != fTestRunnerClient) {
      fTestRunnerClient.stopTest();
    }
    stopUpdateJobs();
  }

  public void selectNextFailure() {
    m_activeRunTab.selectNext();
  }

  public void selectPreviousFailure() {
    m_activeRunTab.selectPrevious();
  }

  public void showTest(RunInfo test) {
    m_activeRunTab.setSelectedTest(test.getId());
    new OpenTestAction(this, test.getClassName(), test.getMethodName(), false).run();
  }


  public void reset() {
    reset(0, 0);
    clearStatus();
  }

  private void stopUpdateJobs() {
    if(m_updateUIJob != null) {
      m_updateUIJob.stop();
      m_updateUIJob = null;
    }
    if((m_isRunningJob != null) && (m_runLock != null)) {
      m_runLock.release();
      m_isRunningJob = null;
    }
  }

  protected void selectFirstFailure() {
    // TODO
  }

  private boolean hasErrors() {
    return (m_failedCount + m_skippedCount + m_successPercentageFailed > 0);
  }

  private String elapsedTimeAsString(long runTime) {
    return NumberFormat.getInstance().format((double) runTime / 1000);
  }

  private void handleStopped() {
    postSyncRunnable(new Runnable() {
        public void run() {
          if(isDisposed()) {
            return;
          }

          fProgressBar.stopped();
        }
      });
    stopUpdateJobs();
  }

  public void startTestRunListening(IJavaProject project, 
                                    String subName, 
                                    int port, 
                                    ILaunch launch) {
    m_LastLaunch = launch;
    m_workingProject = project;
    m_hasFailures= false;
    
    aboutToLaunch(subName);
    
    if(null != fTestRunnerClient) {
      stopTest();
    }
    fTestRunnerClient = new EclipseTestRunnerClient();
    fTestRunnerClient.startListening(this, this, port);
    
    m_rerunAction.setEnabled(true);
    m_rerunFailedAction.setEnabled(false);
    m_openReportAction.setEnabled(true);
//    getViewSite().getActionBars().updateActionBars();
  }

  protected void aboutToLaunch(final String message) {
    String msg = ResourceUtil.getFormattedString("TestRunnerViewPart.message.launching", message); //$NON-NLS-1$
    firePropertyChange(IWorkbenchPart.PROP_TITLE);
  }

  public synchronized void dispose() {
    m_isDisposed = true;
    stopTest();

    TestNGPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
    getViewSite().getPage().removePartListener(fPartListener);
    fStackViewIcon.dispose();
    m_viewIcon.dispose();
    fOKColor.dispose();
    fFailureColor.dispose();
  }

  private void resetProgressBar(final int total) {
    fProgressBar.reset(total);
    fProgressBar.setMaximum(total, total);
  }

  private void postSyncRunnable(Runnable r) {
    if(!isDisposed()) {
      getDisplay().syncExec(r);
    }
  }

  private void aboutToStart() {
    postSyncRunnable(new Runnable() {
        public void run() {
          if(!isDisposed()) {
            for(Enumeration e = m_tabsList.elements(); e.hasMoreElements();) {
              TestRunTab v = (TestRunTab) e.nextElement();
              v.aboutToStart();
            }
            fNextAction.setEnabled(false);
            fPrevAction.setEnabled(false);
          }
        }
      });
  }

  private void postEndTest(final String testId, final String testName) {
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
        m_testIsRunning = false;

        if(hasErrors()) {
          fNextAction.setEnabled(true);
          fPrevAction.setEnabled(true);
        }
      }
    });
  }

  private void refreshCounters() {
    m_counterPanel.setMethodCount(m_methodCount);
    m_counterPanel.setPassedCount(m_passedCount);
    m_counterPanel.setFailedCount(m_failedCount);
    m_counterPanel.setSkippedCount(m_skippedCount);
    String msg= "";
    if(m_startTime != 0L && m_stopTime != 0L) {
      msg= " (" + (m_stopTime - m_startTime) + " ms)";
    }
    
    fProgressBar.refresh(hasErrors(), msg);
  }

  protected void postShowTestResultsView() {
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
        showTestResultsView();
      }
    });
  }

  /**
   * Show the result view.
   */
  public void showTestResultsView() {
    IWorkbenchWindow   window = getSite().getWorkbenchWindow();
    IWorkbenchPage     page = window.getActivePage();
    TestRunnerViewPart testRunner = null;

    if(page != null) {
      try { 
        testRunner = (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
        if(testRunner == null) {

          IWorkbenchPart activePart = page.getActivePart();
          testRunner = (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME);

          //restore focus
          page.activate(activePart);
        }
        else {
          page.bringToTop(testRunner);
        }
      }
      catch(PartInitException pie) {
        TestNGPlugin.log(pie);
      }
    }
  }

  /**
   * Can display addition infos.
   * FIXME
   */
//  protected void doShowStatus() {
//    setContentDescription(m_statusMessage);
//  }

  /**
   * FIXME
   */
  protected void setInfoMessage(final String message) {
    m_statusMessage = message;
  }

  /**
   * FIXME
   */
//  private void showMessage(String msg) {
//    postError(msg);
//  }

  /**
   * FIXME
   */
//  protected void postError(final String message) {
//    m_statusMessage = message;
//  }

  /**
   * FIXME
   */
  private void clearStatus() {
    getStatusLine().setMessage(null);
    getStatusLine().setErrorMessage(null);
  }


  protected CTabFolder createTestRunTabs(Composite parent) {
    CTabFolder tabFolder = new CTabFolder(parent, SWT.TOP);
    tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

    loadTestRunTabs(tabFolder);
    tabFolder.setSelection(0);
    m_activeRunTab = (TestRunTab) m_tabsList.firstElement();

    tabFolder.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent event) {
          testTabChanged(event);
        }
      });

    return tabFolder;
  }

  private void loadTestRunTabs(CTabFolder tabFolder) {

    IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(ID_EXTENSION_POINT_TESTRUN_TABS);
    if(extensionPoint == null) {
      return;
    }

    IConfigurationElement[] configs = extensionPoint.getConfigurationElements();
    MultiStatus status = new MultiStatus(TestNGPlugin.PLUGIN_ID,
                                         IStatus.OK,
                                         "Could not load some testRunTabs extension points", //$NON-NLS-1$
                                         null); 

    for(int i = 0; i < configs.length; i++) {
      try {

        TestRunTab testRunTab = (TestRunTab) configs[i].createExecutableExtension("class"); //$NON-NLS-1$
        testRunTab.createTabControl(tabFolder, this);
        m_tabsList.addElement(testRunTab);
      }
      catch(CoreException e) {
        status.add(e.getStatus());
      }
    }
    if(!status.isOK()) {
      TestNGPlugin.log(status);
    }
  }

  private void testTabChanged(SelectionEvent event) {
    String selectedTestId = m_activeRunTab.getSelectedTestId();
    
    for(Enumeration e = m_tabsList.elements(); e.hasMoreElements();) {
      TestRunTab v = (TestRunTab) e.nextElement();

      v.setSelectedTest(selectedTestId);
      
      if(((CTabFolder) event.widget).getSelection().getText() == v.getName()) {
        m_activeRunTab = v;
        m_activeRunTab.activate();
      }
    }
  }

  private SashForm createSashForm(Composite parent) {
    m_sashForm = new SashForm(parent, SWT.VERTICAL);

    ViewForm top = new ViewForm(m_sashForm, SWT.NONE);
    m_tabFolder = createTestRunTabs(top);
    m_tabFolder.setLayoutData(new Layout() {
        protected Point computeSize (Composite composite, int wHint, int hHint, boolean flushCache) {
            if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
                return new Point(wHint, hHint);
                
            Control [] children = composite.getChildren ();
            int count = children.length;
            int maxWidth = 0, maxHeight = 0;
            for (int i=0; i<count; i++) {
                Control child = children [i];
                Point pt = child.computeSize (SWT.DEFAULT, SWT.DEFAULT, flushCache);
                maxWidth = Math.max (maxWidth, pt.x);
                maxHeight = Math.max (maxHeight, pt.y);
            }
            
            if (wHint != SWT.DEFAULT)
                maxWidth= wHint;
            if (hHint != SWT.DEFAULT)
                maxHeight= hHint;
            
            return new Point(maxWidth, maxHeight);
        }
        
        protected void layout (Composite composite, boolean flushCache) {
            Rectangle rect= composite.getClientArea();
            Control[] children = composite.getChildren();
            for (int i = 0; i < children.length; i++) {
                children[i].setBounds(rect);
            }
        }
    });
    top.setContent(m_tabFolder);

    ViewForm bottom = new ViewForm(m_sashForm, SWT.NONE);
    CLabel   label = new CLabel(bottom, SWT.NONE);
    label.setText(ResourceUtil.getString("TestRunnerViewPart.label.failure")); //$NON-NLS-1$
    label.setImage(fStackViewIcon);
    bottom.setTopLeft(label);

    ToolBar failureToolBar = new ToolBar(bottom, SWT.FLAT | SWT.WRAP);
    bottom.setTopCenter(failureToolBar);
    m_failureTraceComponent = new FailureTrace(bottom, this, failureToolBar);
    bottom.setContent(m_failureTraceComponent.getComposite());

    m_sashForm.setWeights(new int[] { 50, 50 });

    return m_sashForm;
  }

  private void reset(final int suiteCount, final int testCount) {
    m_suitesTotalCount = suiteCount;
    m_testsTotalCount = testCount;
    m_methodTotalCount = 0;
    m_suiteCount = 0;
    m_testCount = 0;
    m_methodCount = 0;
    m_passedCount = 0;
    m_failedCount = 0;
    m_skippedCount = 0;
    m_successPercentageFailed = 0;
    m_startTime= 0L;
    m_stopTime= 0L;
    
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
        
        m_counterPanel.reset();
        m_failureTraceComponent.clear();
        fProgressBar.reset(testCount);
        clearStatus();
        
        for(Enumeration e = m_tabsList.elements(); e.hasMoreElements();) {
          TestRunTab v = (TestRunTab) e.nextElement();
          v.aboutToStart();
        }
      }
    });
  }

  public void setFocus() {
    if(m_activeRunTab != null) {
      m_activeRunTab.setFocus();
    }
  }

  public void createPartControl(Composite parent) {
    m_parentComposite = parent;
    addResizeListener(parent);

    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    parent.setLayout(gridLayout);

    configureToolBar();

    m_counterComposite = createProgressCountPanel(parent);
    m_counterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                                                 | GridData.HORIZONTAL_ALIGN_FILL));

    SashForm sashForm = createSashForm(parent);
    sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

    TestNGPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);

    getViewSite().getPage().addPartListener(fPartListener);

    if(m_stateMemento != null) {
      restoreLayoutState(m_stateMemento);
    }
    m_stateMemento = null;
  }

  private void addResizeListener(Composite parent) {
    parent.addControlListener(new ControlListener() {
      public void controlMoved(ControlEvent e) {
      }

      public void controlResized(ControlEvent e) {
        computeOrientation();
      }
    });
  }

  void computeOrientation() {
    if(fOrientation != VIEW_ORIENTATION_AUTOMATIC) {
      fCurrentOrientation = fOrientation;
      setOrientation(fCurrentOrientation);
    }
    else {
      Point size = m_parentComposite.getSize();
      if((size.x != 0) && (size.y != 0)) {
        if(size.x > size.y) {
          setOrientation(VIEW_ORIENTATION_HORIZONTAL);
        }
        else {
          setOrientation(VIEW_ORIENTATION_VERTICAL);
        }
      }
    }
  }

  public void saveState(IMemento memento) {
    if(m_sashForm == null) {
      // part has not been created
      if(m_stateMemento != null) { //Keep the old state;
        memento.putMemento(m_stateMemento);
      }

      return;
    }

    int activePage = m_tabFolder.getSelectionIndex();
    memento.putInteger(TAG_PAGE, activePage);

    int[] weigths = m_sashForm.getWeights();
    int   ratio = (weigths[0] * 1000) / (weigths[0] + weigths[1]);
    memento.putInteger(TAG_RATIO, ratio);
    memento.putInteger(TAG_ORIENTATION, fOrientation);
  }

  private void configureToolBar() {
    IActionBars     actionBars = getViewSite().getActionBars();
    IToolBarManager toolBar = actionBars.getToolBarManager();
    IMenuManager    viewMenu = actionBars.getMenuManager();

    fToggleOrientationActions = new ToggleOrientationAction[] {
        new ToggleOrientationAction(this, VIEW_ORIENTATION_VERTICAL),
        new ToggleOrientationAction(this, VIEW_ORIENTATION_HORIZONTAL),
        new ToggleOrientationAction(this, VIEW_ORIENTATION_AUTOMATIC)
    };
    fNextAction = new ShowNextFailureAction(this);
    fPrevAction = new ShowPreviousFailureAction(this);
    m_rerunAction= new RerunAction();
    m_rerunFailedAction= new RerunFailedAction();
    m_openReportAction= new OpenReportAction();
    
    
    fNextAction.setEnabled(false);
    fPrevAction.setEnabled(false);
    m_rerunAction.setEnabled(false);
    m_rerunFailedAction.setEnabled(false);
    m_openReportAction.setEnabled(false);
    
    actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), fNextAction);
    actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), fPrevAction);

    toolBar.add(fNextAction);
    toolBar.add(fPrevAction);
    toolBar.add(new Separator());
    toolBar.add(m_rerunAction);
    toolBar.add(m_rerunFailedAction);
    toolBar.add(new Separator());
    toolBar.add(m_openReportAction);
    
    for(int i = 0; i < fToggleOrientationActions.length; ++i) {
      viewMenu.add(fToggleOrientationActions[i]);
    }

    actionBars.updateActionBars();
  }

  private IStatusLineManager getStatusLine() {

    // we want to show messages globally hence we
    // have to go through the active part
    IViewSite      site = getViewSite();
    IWorkbenchPage page = site.getPage();
    IWorkbenchPart activePart = page.getActivePart();

    if(activePart instanceof IViewPart) {

      IViewPart activeViewPart = (IViewPart) activePart;
      IViewSite activeViewSite = activeViewPart.getViewSite();

      return activeViewSite.getActionBars().getStatusLineManager();
    }

    if(activePart instanceof IEditorPart) {
      IEditorPart activeEditorPart = (IEditorPart) activePart;
      IEditorActionBarContributor contributor = activeEditorPart.getEditorSite()
                                                                .getActionBarContributor();
      if(contributor instanceof EditorActionBarContributor) {
        return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
      }
    }

    // no active part
    return getViewSite().getActionBars().getStatusLineManager();
  }

  protected Composite createProgressCountPanel(Composite parent) {
    Display display= parent.getDisplay();
    fFailureColor= new Color(display, 159, 63, 63);
    fOKColor= new Color(display, 95, 191, 95);
    
    Composite  composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
//    layout.numColumns = 1;
    composite.setLayout(layout);
    setCounterColumns(layout);

    fProgressBar = new JUnitProgressBar(composite);
    fProgressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                                            | GridData.HORIZONTAL_ALIGN_FILL));

//    m_progressBar= new ProgressBar(composite, SWT.SMOOTH);
//    m_progressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
//        | GridData.HORIZONTAL_ALIGN_FILL));
//    m_progressBar.addPaintListener(new ProgressBarTextPainter(this));
//    m_progressBar.setForeground(fOKColor);

    m_counterPanel = new CounterPanel(composite);
    m_counterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                                             | GridData.HORIZONTAL_ALIGN_FILL));


    return composite;
  }

  /*private static class ProgressBarTextPainter implements PaintListener {
    ProgressBar m_bar;
    Color m_fontColor;
    TestRunnerViewPart parentComponent;
    
    public ProgressBarTextPainter(TestRunnerViewPart parent) {
      m_bar= parent.m_progressBar;
      parentComponent= parent;
      m_fontColor= m_bar.getDisplay().getSystemColor(SWT.COLOR_BLACK);
    }
    
    public void paintControl(PaintEvent e) {
      // string to draw. 
      String string = "Tests: " + parentComponent.m_testCount + "/" + parentComponent.m_testsTotalCount
        + "  Methods: " + parentComponent.m_methodCount + "/" + parentComponent.m_methodTotalCount;
      Point point = m_bar.getSize();
      e.gc.setForeground(m_fontColor);
      FontMetrics fontMetrics = e.gc.getFontMetrics();
      int stringWidth = fontMetrics.getAverageCharWidth() * string.length();
      int stringHeight = fontMetrics.getHeight();
      e.gc.drawString(string, (point.x-stringWidth)/2 , (point.y-stringHeight)/2, true);
    }
  }*/
  
  public void handleTestSelected(final RunInfo testInfo) {
    postSyncRunnable(new Runnable() {
      public void run() {
        if(!isDisposed()) {
          m_failureTraceComponent.showFailure(testInfo);
        }
      }
    });
  }

  public IJavaProject getLaunchedProject() {
    return m_workingProject;
  }

  public ILaunch getLastLaunch() {
    return m_LastLaunch;
  }

  private boolean isDisposed() {
    return m_isDisposed || m_counterPanel.isDisposed();
  }

  private Display getDisplay() {
    return getViewSite().getShell().getDisplay();
  }

  public boolean isCreated() {
    return m_counterPanel != null;
  }

  public void warnOfContentChange() {

    IWorkbenchSiteProgressService service = getProgressService();
    if(service != null) {
      service.warnOfContentChange();
    }
  }

  public boolean lastLaunchIsKeptAlive() {
    return false;
  }

  private void setOrientation(int orientation) {
    if((m_sashForm == null) || m_sashForm.isDisposed()) {
      return;
    }

    boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
    m_sashForm.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
    for(int i = 0; i < fToggleOrientationActions.length; ++i) {
      fToggleOrientationActions[i].setChecked(fOrientation
                                              == fToggleOrientationActions[i].getOrientation());
    }
    fCurrentOrientation = orientation;

    GridLayout layout = (GridLayout) m_counterComposite.getLayout();
//    layout.numColumns = 1;
    setCounterColumns(layout);

    try {
      m_parentComposite.layout();
    }
    catch(Throwable cause) {
      cause.printStackTrace();
    }
  }

  private void setCounterColumns(GridLayout layout) {
    if(fCurrentOrientation == VIEW_ORIENTATION_HORIZONTAL) {
      layout.numColumns = 2;
    }
    else {
      layout.numColumns = 1;
    }
  }
  
  private class ToggleOrientationAction extends Action {

    private final int fActionOrientation;

    public ToggleOrientationAction(TestRunnerViewPart v, int orientation) {
      super("", AS_RADIO_BUTTON); //$NON-NLS-1$
      if(orientation == TestRunnerViewPart.VIEW_ORIENTATION_HORIZONTAL) {
        setText(ResourceUtil.getString("TestRunnerViewPart.toggle.horizontal.label")); //$NON-NLS-1$
        setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/th_horizontal.gif")); //$NON-NLS-1$
      }
      else if(orientation == TestRunnerViewPart.VIEW_ORIENTATION_VERTICAL) {
        setText(ResourceUtil.getString("TestRunnerViewPart.toggle.vertical.label")); //$NON-NLS-1$
        setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/th_vertical.gif")); //$NON-NLS-1$
      }
      else if(orientation == TestRunnerViewPart.VIEW_ORIENTATION_AUTOMATIC) {
        setText(ResourceUtil.getString("TestRunnerViewPart.toggle.automatic.label")); //$NON-NLS-1$
        setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/th_automatic.gif")); //$NON-NLS-1$
      }
      fActionOrientation = orientation;
    }

    public int getOrientation() {
      return fActionOrientation;
    }

    public void run() {
      if(isChecked()) {
        fOrientation = fActionOrientation;
        computeOrientation();
      }
    }
  }

  private class TreeEntryQueueDrainer implements Runnable {
    public void run() {
      while(true) {

        RunInfo treeEntry;
        synchronized(m_treeEntriesQueue) {
          if(m_treeEntriesQueue.isEmpty() || isDisposed()) {
            fQueueDrainRequestOutstanding = false;

            return;
          }
          treeEntry = (RunInfo) m_treeEntriesQueue.remove(0);
        }
        for(Enumeration e = m_tabsList.elements(); e.hasMoreElements();) {

          TestRunTab v = (TestRunTab) e.nextElement();
          v.newTreeEntry(treeEntry);
        }
      }
    }
  }

  /**
   * Background job running in UI thread for updating components info. 
   */
  class UpdateUIJob extends UIJob {
    private volatile boolean fRunning = true;

    public UpdateUIJob(String name) {
      super(name);
      setSystem(true);
    }

    public IStatus runInUIThread(IProgressMonitor monitor) {
      if(!isDisposed()) {
//        doShowStatus();
        refreshCounters();
//        m_progressBar.redraw();
      }
      schedule(REFRESH_INTERVAL);

      return Status.OK_STATUS;
    }

    public void stop() {
      fRunning = false;
    }

    public boolean shouldSchedule() {
      return fRunning;
    }
  }

  class IsRunningJob extends Job {
    public IsRunningJob(String name) {
      super(name);
      setSystem(true);
    }

    public IStatus run(IProgressMonitor monitor) {
      // wait until the test run terminates
      m_runLock.acquire();

      return Status.OK_STATUS;
    }

    public boolean belongsTo(Object family) {
      return family == TestRunnerViewPart.FAMILY_RUN;
    }
  }


  private static void ppp(final Object message) {
//    System.out.println("[TestRunnerViewPart]:- " + message);
  }

  /**
   * @see IWorkbenchPart#getTitleImage()
   */
  public Image getTitleImage() {
    return m_viewIcon;
  }
  
  /**
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event) {
  }

  private void postNewTreeEntry(RunInfo runInfo) {
    synchronized(m_treeEntriesQueue) {
      m_treeEntriesQueue.add(runInfo);
      if(!fQueueDrainRequestOutstanding) {
        fQueueDrainRequestOutstanding = true;
        if(!isDisposed()) {
          getDisplay().asyncExec(new TreeEntryQueueDrainer());
        }
      }
    }
  }
  
  private void postTestResult(final RunInfo runInfo, final int progressStep) {
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
//        for(int i = 0; i < m_tabsList.size(); i++) {
//          ((TestRunTab) m_tabsList.elementAt(i)).newTreeEntry(runInfo);
//        }

        fProgressBar.step(progressStep);
//        updateProgressBar(m_progressBar.getSelection() + 1, (progressStep == 0));

        for(int i = 0; i < m_tabsList.size(); i++) {
          ((TestRunTab) m_tabsList.elementAt(i)).updateTestResult(runInfo);
        }
      }
    });
  }
  
  private void postTestStarted(final RunInfo runInfo) {
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
        for(int i = 0; i < m_tabsList.size(); i++) {
          ((TestRunTab) m_tabsList.elementAt(i)).newTreeEntry(runInfo);
        }
      }
    });
  }
  
  /*private void updateProgressBar(int value, boolean success) {
    if(!success || hasErrors()) {
      m_progressBar.setForeground(fFailureColor);
      m_progressBar.setBackground(fFailureColor);
    }
    m_progressBar.setSelection(value);
  }*/
  
  ///~ [CURRENT WORK] ~///
  private IPartListener2 fPartListener = new IPartListener2() {
    public void partActivated(IWorkbenchPartReference ref) {
    }

    public void partBroughtToTop(IWorkbenchPartReference ref) {
    }

    public void partInputChanged(IWorkbenchPartReference ref) {
    }

    public void partClosed(IWorkbenchPartReference ref) {
    }

    public void partDeactivated(IWorkbenchPartReference ref) {
    }

    public void partOpened(IWorkbenchPartReference ref) {
    }

    public void partVisible(IWorkbenchPartReference ref) {
      if(getSite().getId().equals(ref.getId())) {
        m_partIsVisible = true;
      }
    }

    public void partHidden(IWorkbenchPartReference ref) {
      if(getSite().getId().equals(ref.getId())) {
        m_partIsVisible = false;
      }
    }
  };
  
  private class RerunAction extends Action {
    public RerunAction() {
      setText(ResourceUtil.getString("TestRunnerViewPart.rerunaction.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("TestRunnerViewPart.rerunaction.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/relaunch.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
    }
      
    public void run() {
      if(null != m_LastLaunch) {
        DebugUITools.launch(m_LastLaunch.getLaunchConfiguration(), m_LastLaunch.getLaunchMode());
      }
    }
  }
  
  private class OpenReportAction extends Action {
    public OpenReportAction() {
      setText(ResourceUtil.getString("TestRunnerViewPart.openreport.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("TestRunnerViewPart.openreport.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/report.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/report.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/report.gif")); //$NON-NLS-1$
    }

    private void openEditor(IFile file) {
      final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if(window == null) {
        return;
      }

      final IWorkbenchPage page = window.getActivePage();
      if(page == null) {
        return;
      }
      try {
        IDE.openEditor(page, file);
      } 
      catch(final PartInitException e) {
        TestNGPlugin.log(e);
      }
    }
    
    public void run() {
      Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
      IJavaProject javaProject= m_workingProject != null ? m_workingProject : JDTUtil.getJavaProjectContext();
      if(null == javaProject) {
        return;
      }
      TestNGPlugin plugin= TestNGPlugin.getDefault();
      IPath filePath= new Path(plugin.getOutputDirectoryPath(javaProject).toOSString() + "/index.html");
      boolean isAbsolute= plugin.isAbsolutePath(javaProject.getElementName());
      
      IProgressMonitor progressMonitor= new NullProgressMonitor();
      if(isAbsolute) {
        IFile file = javaProject.getProject().getFile("temp-testng-index.html");
        try {
          file.createLink(filePath, IResource.NONE, progressMonitor);
          if(null == file) return;
          try {
            openEditor(file);
          }
          finally {
            file.delete(true, progressMonitor);
          }
        }
        catch(CoreException cex) {
          ; // TODO: is there any other option?
        }
      }
      else {
        IFile file= (IFile) workspace.newResource(filePath, IResource.FILE);
        if(null == file) return;
        try {
          file.refreshLocal(IResource.DEPTH_ZERO, progressMonitor);
          openEditor(file);
        }
        catch(CoreException cex) {
          ; // nothing I can do about it
        }
      }
    }
  }
  
  private class RerunFailedAction extends Action {
    public RerunFailedAction() {
      setText(ResourceUtil.getString("TestRunnerViewPart.rerunfailedsaction.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("TestRunnerViewPart.rerunfailedsaction.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/relaunchf.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunchf.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunchf.gif")); //$NON-NLS-1$
    }
    
    public void run() {
      if(null != m_LastLaunch && hasErrors()) {
        LaunchUtil.launchFailedSuiteConfiguration(m_workingProject, m_LastLaunch.getLaunchMode());
      }
    }    
  }

  /// ~ ITestNGRemoteEventListener
  public void onInitialization(GenericMessage genericMessage) {
    final int suiteCount = Integer.parseInt(genericMessage.getProperty("suiteCount")); //$NON-NLS-1$
    final int testCount = Integer.parseInt(genericMessage.getProperty("testCount")); //$NON-NLS-1$
    reset(suiteCount, testCount);
    stopUpdateJobs();
    m_updateUIJob= new UpdateUIJob("Update TestNG"); //$NON-NLS-1$ 
    m_isRunningJob = new IsRunningJob("TestNG run wrapper job"); //$NON-NLS-1$
    m_runLock = Platform.getJobManager().newLock();
    // acquire lock while a test run is running the lock is released when the test run terminates
    // the wrapper job will wait on this lock.
    m_runLock.acquire();
    getProgressService().schedule(m_isRunningJob);
    m_updateUIJob.schedule(REFRESH_INTERVAL);
    m_startTime= System.currentTimeMillis();
  }

  public void onStart(SuiteMessage suiteMessage) {
    RunInfo ri= new RunInfo(suiteMessage.getSuiteName());
    ri.m_methodCount= suiteMessage.getTestMethodCount();

    postNewTreeEntry(ri);
  }

  public void onFinish(SuiteMessage suiteMessage) {
    m_suiteCount++;
    final String entryId = new RunInfo(suiteMessage.getSuiteName()).getId();
    
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
        for(int i = 0; i < m_tabsList.size(); i++) {
          ((TestRunTab) m_tabsList.elementAt(i)).updateEntry(entryId);
        }
      }
    });
    
    if(m_suitesTotalCount == m_suiteCount) {
      fNextAction.setEnabled(hasErrors());
      fPrevAction.setEnabled(hasErrors());
      m_rerunFailedAction.setEnabled(hasErrors());
      m_hasFailures= true;
      postShowTestResultsView();
      stopTest();
      m_stopTime= System.currentTimeMillis();
      postSyncRunnable(new Runnable() {
        public void run() {
          if(isDisposed()) {
            return;
          }
          refreshCounters();
//          m_progressBar.redraw();
        }
      });
      
    }
  }

  public void onStart(TestMessage tm) {
    RunInfo ri= new RunInfo(tm.getSuiteName(), tm.getTestName());
    ri.m_methodCount= tm.getTestMethodCount();
    m_methodTotalCount += tm.getTestMethodCount();
    
    postNewTreeEntry(ri);
    
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
        
        int newMaxBar = (m_methodTotalCount * m_testsTotalCount) / (m_testCount + 1);
        fProgressBar.setMaximum(newMaxBar, m_methodTotalCount);
//        m_progressBar.setMaximum(newMaxBar);
//        System.out.println("se maresteeee");
      }
    });
  }

  public void onFinish(TestMessage tm) {
    m_testCount++;
    
    // HINT: fix the total number of methods
    if(m_methodCount != m_methodTotalCount) {
        m_methodTotalCount= m_methodCount; // trust the methodCount
    }
    
    final String entryId = new RunInfo(tm.getSuiteName(), tm.getTestName()).getId();
    
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
        for(int i = 0; i < m_tabsList.size(); i++) {
          ((TestRunTab) m_tabsList.elementAt(i)).updateEntry(entryId);
        }
        
        fProgressBar.stepTests();
      }
    });
  }

  private RunInfo createRunInfo(TestResultMessage trm, String stackTrace, int type) {
    return new RunInfo(trm.getSuiteName(),
                       trm.getName(),
                       trm.getTestClass(),
                       trm.getMethod(),
                       trm.getParameters(),
                       trm.getParameterTypes(),
                       stackTrace,
                       type);
                       
  }
  
  public void onTestSuccess(TestResultMessage trm) {
    m_passedCount++;
    m_methodCount++;
    
    postTestResult(createRunInfo(trm, null, ITestResult.SUCCESS), 0 /*no error*/);
  }

  public void onTestFailure(TestResultMessage trm) {
    m_failedCount++;
    m_methodCount++;
//    System.out.println("[INFO:onTestFailure]:" + trm.getMessageAsString());
    postTestResult(createRunInfo(trm, trm.getStackTrace(), ITestResult.FAILURE), 1 /*error*/);
  }

  public void onTestSkipped(TestResultMessage trm) {
    m_skippedCount++;
    m_methodCount++;
//    System.out.println("[INFO:onTestSkipped]:" + trm.getMessageAsString());
    postTestResult(createRunInfo(trm, null, ITestResult.SKIP), 1 /*error*/
    );
  }

  public void onTestFailedButWithinSuccessPercentage(TestResultMessage trm) {
    m_successPercentageFailed++;
    m_methodCount++;
    
    postTestResult(createRunInfo(trm, trm.getStackTrace(), ITestResult.SUCCESS_PERCENTAGE_FAILURE),
                   1 /*error*/
    );
  }

  /**
   * FIXME: currently not used; it should be use to mark the currently running
   * tests.
   */
  public void onTestStart(TestResultMessage trm) {
//    System.out.println("[INFO:onTestStart]:" + trm.getMessageAsString());
    postTestStarted(createRunInfo(trm, null, ITestResult.SUCCESS));
  }
}
