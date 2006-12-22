package org.testng.eclipse;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractSet;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.testng.eclipse.ui.TestRunnerViewPart;
import org.testng.eclipse.ui.preferences.PreferenceConstants;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.remote.RemoteTestNG;

/**
 * The plug-in runtime class for the TestNG plug-in.
 */
public class TestNGPlugin extends AbstractUIPlugin implements ILaunchListener {
  /**
   * The single instance of this plug-in runtime class.
   */
  private static TestNGPlugin m_pluginInstance = null;

  public static final String PLUGIN_ID = "org.testng.eclipse"; //$NON-NLS-1$
  public static final String TESTNG_HOME= "TESTNG_HOME"; //$NON-NLS-1$

  public static final String MAIN_RUNNER = RemoteTestNG.class.getName();

  public static final int LAUNCH_ERROR = 1001;

  private static URL m_fgIconBaseURL;
  
  private static boolean m_isStopped = false;

  /**
   * Use to track new launches. We need to do this
   * so that we only attach a TestRunner once to a launch.
   * Once a test runner is connected it is removed from the set.
   */
  private AbstractSet n_trackedLaunches = new HashSet(20);

  private BundleContext m_context;

  public TestNGPlugin() {
    m_pluginInstance = this;
    try {
      m_fgIconBaseURL = new URL(Platform.getBundle(PLUGIN_ID).getEntry("/"), "icons/full/"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    catch(MalformedURLException e) {
      ;
    }
  }

  public static TestNGPlugin getDefault() {
    return m_pluginInstance;
  }

  public static String getPluginId() {
    return PLUGIN_ID;
  }

  public static void log(Throwable e) {
    log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  /**
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    super.start(context);

    m_context= context;
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    launchManager.addLaunchListener(this);
    m_isStopped = false;
  }

  /**
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    try {
      // Dispose the font
      if (null != BOLD_FONT) {
        BOLD_FONT.dispose();
      }
      m_isStopped = true;
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      launchManager.removeLaunchListener(this);
    }
    finally {
      super.stop(context);
      m_context= null;
    }
  }

  public static boolean isStopped() {
    return m_isStopped;
  }
  
  /**
   * @see org.eclipse.debug.core.ILaunchListener#launchRemoved(org.eclipse.debug.core.ILaunch)
   */
  public void launchRemoved(final ILaunch launch) {
    ppp("launchRemoved:" + n_trackedLaunches);
    n_trackedLaunches.remove(launch);
    SWTUtil.getDisplay().asyncExec(new Runnable() {
        public void run() {

          TestRunnerViewPart testRunnerViewPart = findTestRunnerViewPartInActivePage();
          if((testRunnerViewPart != null)
             && testRunnerViewPart.isCreated()
             && launch.equals(testRunnerViewPart.getLastLaunch())) {

            testRunnerViewPart.reset();
          }
        }
      });
  }

  /**
   * @see org.eclipse.debug.core.ILaunchListener#launchAdded(org.eclipse.debug.core.ILaunch)
   */
  public void launchAdded(final ILaunch launch) {
    ppp("launchAdded:" + n_trackedLaunches);
    n_trackedLaunches.add(launch);
  }

  /**
   * @see org.eclipse.debug.core.ILaunchListener#launchChanged(org.eclipse.debug.core.ILaunch)
   */
  public void launchChanged(final ILaunch launch) {
    ppp("launchChanged:" + n_trackedLaunches);
    if(!n_trackedLaunches.contains(launch)) {
      return;
    }

    int port = -1;
    String projectName = "";
    String subName = "";
    
    try {
      // test whether the launch defines the JUnit attributes
      port = ConfigurationHelper.getPort(launch);
      projectName = ConfigurationHelper.getProjectName(launch);
      subName = ConfigurationHelper.getSubName(launch);
    }
    catch(Exception ex) {
      log(ex);
      return;
    }
    
    if(null == projectName) {
//      log(new Status(IStatus.WARNING, getPluginId(), IStatus.WARNING, 
//          "launch a project with NULL name. Ignoring", null));
      return;
    }

//    ppp("PORT   :" + port);
//    ppp("PROJECT:" + projectName);
//    ppp("SUBNAME:" + subName);
    
    final int   finalPort = port;
    final IJavaProject ijp = JDTUtil.getJavaProject(projectName);
    final String name = subName;
    
    n_trackedLaunches.remove(launch);

    SWTUtil.getDisplay().asyncExec(new Runnable() {
        public void run() {
          connectTestRunner(launch, ijp, name, finalPort);
        }
      });
  }

  public void connectTestRunner(ILaunch launch, 
                                IJavaProject launchedProject,
                                String subName,
                                int port) {
    TestRunnerViewPart testRunnerViewPart = showTestRunnerViewPartInActivePage(findTestRunnerViewPartInActivePage());
    if(testRunnerViewPart != null) {
      testRunnerViewPart.startTestRunListening(launchedProject, subName, port, launch);
    }
  }

  private TestRunnerViewPart showTestRunnerViewPartInActivePage(TestRunnerViewPart testRunner) {
    IWorkbenchPart activePart = null;
    IWorkbenchPage page = null;
    try {

      // TODO: have to force the creation of view part contents
      // otherwise the UI will not be updated
      if((testRunner != null) && testRunner.isCreated()) {
        return testRunner;
      }

      page = SWTUtil.getActivePage(getWorkbench());

      if(page == null) {
        return null;
      }

      activePart = page.getActivePart();

      //  show the result view if it isn't shown yet
      return (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME);
    }
    catch(PartInitException pie) {
      log(pie);

      return null;
    }
    finally {

      //restore focus stolen by the creation of the result view
      if((page != null) && (activePart != null)) {
        page.activate(activePart);
      }
    }
  }

  private TestRunnerViewPart findTestRunnerViewPartInActivePage() {

    IWorkbenchPage page = SWTUtil.getActivePage(getWorkbench());
    if(null == page) {
      return null;
    }

    return (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
  }

  public static URL makeIconFileURL(String name) throws MalformedURLException {
    if(m_fgIconBaseURL == null) {
      throw new MalformedURLException();
    }

    return new URL(m_fgIconBaseURL, name);
  }

  public static ImageDescriptor getImageDescriptor(String relativePath) {
    try {
      return ImageDescriptor.createFromURL(makeIconFileURL(relativePath));
    }
    catch(MalformedURLException e) {

      // should not happen
      return ImageDescriptor.getMissingImageDescriptor();
    }
  }

  private static void ppp(final Object message) {
//    System.out.println("[TestNG]:- " + message);
  }

  // Assume that all the controls that go through bold() have the same font, 
  // which is probably a safe assumption.  The BOLD font will be disposed
  // in the stop() method
  public static Font BOLD_FONT = null;
  public static Font REGULAR_FONT = null;

  /**
   * Make the passed label bold or not based on the boolean.  This method
   * is put here because it initializes BOLD_FONT, which is later disposed
   * in the stop method of this class
   */
  public static void bold(Control label, boolean bold) {
    if (null == BOLD_FONT) {
      REGULAR_FONT = label.getFont();
      FontData data = REGULAR_FONT.getFontData() [0];
      data.setStyle(data.getStyle() | SWT.BOLD);
      BOLD_FONT = new Font(label.getDisplay(), data);
    }
    
    label.setFont(bold ? BOLD_FONT : REGULAR_FONT);
  }
  
  public Bundle getBundle(String bundleName) {
    Bundle bundle= Platform.getBundle(bundleName);
    if (bundle != null)
        return bundle;
    
    // Accessing unresolved bundle
    ServiceReference serviceRef= m_context.getServiceReference(PackageAdmin.class.getName());
    PackageAdmin admin= (PackageAdmin) m_context.getService(serviceRef);
    Bundle[] bundles= admin.getBundles(bundleName, null);
    if (bundles != null && bundles.length > 0)
        return bundles[0];
    return null;
  }
  
  private boolean isEmtpy(String string) {
    return null == string || "".equals(string.trim());
  }
  
  public void storeOutputDir(String projectName, String outdir, boolean isAbsolute) {
    getPreferenceStore().setValue(projectName + ".outdir", outdir);
    getPreferenceStore().setValue(projectName + ".absolutepath", isAbsolute);
  }

  /**
   * @param name
   * @return
   */
  public String getReporters(String projectName) {
    return getPreferenceStore().getString(projectName + ".reporters");
  }

  /**
   * @param name
   * @param text
   */
  public void storeReporters(String projectName, String reporters) {
    getPreferenceStore().setValue(projectName + ".reporters", reporters);
  }

  /**
   * @param name
   * @return
   */
  public boolean getDisabledListeners(String projectName) {
    return getPreferenceStore().getBoolean(projectName + ".disabledListeners");
  }

  /**
   * @param name
   * @param selection
   */
  public void storeDisabledListeners(String projectName, boolean selection) {
    getPreferenceStore().setValue(projectName + ".disabledListeners", selection);
  }

  /**
   * @param name
   * @return
   */
  public boolean getUseProjectJar(String projectName) {
    return getPreferenceStore().getBoolean(projectName + ".useProjectJar");
  }

  /**
   * @param name
   * @param selection
   */
  public void storeUseProjectJar(String projectName, boolean selection) {
    getPreferenceStore().setValue(projectName + ".useProjectJar", selection);
  }

  /**
   * @param name
   * @return
   */
  public boolean isAbsolutePath(String projectName) {
    if(getPreferenceStore().contains(projectName + ".absolutepath")) {
      return getPreferenceStore().getBoolean(projectName + ".absolutepath");
    }
    else {
      return getPreferenceStore().getBoolean(PreferenceConstants.P_ABSOLUTEPATH);
    }
  }

  public IPath getOutputDirectoryPath(IJavaProject project) {
    final String projectName= project.getElementName();
    final String outdir= getOutputDir(projectName);
    boolean isAbsolute= isAbsolutePath(projectName);
    return new Path(isAbsolute ? outdir : project.getPath().toOSString() + "/" + outdir);
  }
  
  public String getOutputDir(String projectName) {
    String outdir= getPreferenceStore().getString(projectName + ".outdir");
    if(isEmtpy(outdir)) {
      outdir= getPreferenceStore().getString(PreferenceConstants.P_OUTPUT); 
    }
    
    return outdir;
  }
  
  /**
   * @return an <code>IPath</code> with the absolute path to the output directory
   */
  public IPath getAbsoluteOutputdirPath(IJavaProject project) {
    final String projectName= project.getElementName();
    final String outdir= getOutputDir(projectName);
    boolean isAbsolute= isAbsolutePath(projectName);
    
    return new Path(isAbsolute ? outdir : project.getProject().getLocation().toOSString() + "/" + outdir);
  }
}
