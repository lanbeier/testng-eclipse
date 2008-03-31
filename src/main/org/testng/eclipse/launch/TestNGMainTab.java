package org.testng.eclipse.launch;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.ProjectChooserDialog;
import org.testng.eclipse.ui.util.TestSelectionDialog;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.TestSearchEngine;

/**
 * TestNG specific launcher tab.
 *
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 * @author cedric
 */
public class TestNGMainTab extends AbstractLaunchConfigurationTab implements
		ILaunchConfigurationTab {
	private static ImageRegistry m_imageRegistry = null;
	private static final String UNKNOWN_CONSTANT = "Unknown TestNGLaunchConfigurationConstants: ";

	private Text m_projectText;
	private IJavaProject m_selectedProject;

	// Single test class
	private TestngTestSelector classSelector;

	//method
	TestngTestSelector methodSelector;

	// Group
	private GroupSelector groupSelector;

	// Suite
	private TestngTestSelector suiteSelector;

	// Package
	private TestngTestSelector packageSelector;

	private int m_typeOfTestRun = -1;

	// Runtime group
	private Combo m_complianceLevelCombo;
	private Combo m_logLevelCombo;

	private List/*<TestngTestSelector>*/testngTestSelectors = new ArrayList/*<TestngTestSelector>*/();
	private Map/*<String, List<String>>*/m_classMethods;

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(layout);
		setControl(comp);

		createProjectSelectionGroup(comp);

		Group group = createGroup(comp, "TestNGMainTab.label.run"); //$NON-NLS-1$

		createSelectors(group);
		createRuntimeGroup(comp);
	}

	private void createSelectors(Composite comp) {

		// classSelector
		TestngTestSelector.ButtonHandler handler = new TestngTestSelector.ButtonHandler() {
			public void handleButton() {
				handleSearchButtonSelected(TestNGLaunchConfigurationConstants.CLASS);
			};
		};
		classSelector = new TestngTestSelector(this, handler,
				TestNGLaunchConfigurationConstants.CLASS, comp,
				"TestNGMainTab.label.test") {
			public void initializeFrom(ILaunchConfiguration configuration) {
				List testClassNames = ConfigurationHelper
						.getClasses(configuration);
				setText(Utils.listToString(testClassNames));
			}
		};
		testngTestSelectors.add(classSelector);

		// methodSelector
		handler = new TestngTestSelector.ButtonHandler() {
			public void handleButton() {
				handleSearchButtonSelected(TestNGLaunchConfigurationConstants.METHOD);
			};
		};

		methodSelector = new TestngTestSelector(this, handler,
				TestNGLaunchConfigurationConstants.METHOD, comp,
				"TestNGMainTab.label.method") {
			public void initializeFrom(ILaunchConfiguration configuration) {
				List names = ConfigurationHelper.getMethods(configuration);
				setText(Utils.listToString(names));
			}
		};
		testngTestSelectors.add(methodSelector);

		// groupSelector
		groupSelector = new GroupSelector(this, comp);
		testngTestSelectors.add(groupSelector);

		//packageSelector
		handler = new TestngTestSelector.ButtonHandler() {
			public void handleButton() {
				handleSearchButtonSelected(TestNGLaunchConfigurationConstants.PACKAGE);
			};
		};
		packageSelector = new TestngTestSelector(this, handler,
				TestNGLaunchConfigurationConstants.PACKAGE, comp,
				"TestNGMainTab.label.package") {
			public void initializeFrom(ILaunchConfiguration configuration) {
				List names = ConfigurationHelper.getPackages(configuration);
				setText(Utils.listToString(names));
			}
		};
		testngTestSelectors.add(packageSelector);

		// suiteSelector
		handler = new TestngTestSelector.ButtonHandler() {
			public void handleButton() {
				handleSearchButtonSelected(TestNGLaunchConfigurationConstants.SUITE);
			};
		};

		class SuiteSelector extends TestngTestSelector {

			private Button m_suiteBrowseButton;

			SuiteSelector(TestNGMainTab callback, ButtonHandler handler,
					Composite comp) {
				super(callback, handler,
						TestNGLaunchConfigurationConstants.SUITE, comp,
						"TestNGMainTab.label.suiteTest");

				Composite fill = new Composite(comp, SWT.NONE);
				GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				gd.horizontalSpan = 2;
				gd.verticalIndent = 0;
				gd.heightHint = 1;
				fill.setLayoutData(gd);

				//
				// Search button
				//
				m_suiteBrowseButton = new Button(comp, SWT.PUSH);
				m_suiteBrowseButton.setText(ResourceUtil
						.getString("TestNGMainTab.label.browsefs")); //$NON-NLS-1$

				TestngTestSelector.ButtonHandler buttonHandler = new TestngTestSelector.ButtonHandler() {
					public void handleButton() {
						FileDialog fileDialog = new FileDialog(
								m_suiteBrowseButton.getShell(), SWT.OPEN);
						setText(fileDialog.open());
					}
				};
				ButtonAdapter adapter = new ButtonAdapter(getTestngType(),
						buttonHandler);

				m_suiteBrowseButton.addSelectionListener(adapter);
				gd = new GridData();
				gd.verticalIndent = 0;
				m_suiteBrowseButton.setLayoutData(gd);

			}

			public void initializeFrom(ILaunchConfiguration configuration) {
				List suites = ConfigurationHelper.getSuites(configuration);
				setText(Utils.listToString(suites));
			}
		}

		suiteSelector = new SuiteSelector(this, handler, comp);
		testngTestSelectors.add(suiteSelector);

	}

	/**
	 * @see ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		if (null == m_selectedProject) {
			m_selectedProject = JDTUtil.getJavaProjectContext();
		}
		ConfigurationHelper.createBasicConfiguration(m_selectedProject, config);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		updateProjectFromConfig(configuration);

		dettachModificationListeners();
		for (Iterator it = testngTestSelectors.iterator(); it.hasNext();) {
			TestngTestSelector sel = (TestngTestSelector) it.next();
			sel.initializeFrom(configuration);
		}

		int logLevel = ConfigurationHelper.getLogLevel(configuration);
		m_logLevelCombo.select(logLevel);

		updateComplianceLevel(configuration);

		int type = ConfigurationHelper.getType(configuration);
		setType(type);
		if (TestNGLaunchConfigurationConstants.METHOD == type) {
			m_classMethods = ConfigurationHelper.getClassMethods(configuration);
		}

		attachModificationListeners();
	}

	private void dettachModificationListeners() {
		for (Iterator it = testngTestSelectors.iterator(); it.hasNext();) {
			TestngTestSelector sel = (TestngTestSelector) it.next();
			sel.detachModificationListener();
		}
	}

	private void attachModificationListeners() {
		for (Iterator it = testngTestSelectors.iterator(); it.hasNext();) {
			TestngTestSelector sel = (TestngTestSelector) it.next();
			sel.attachModificationListener();
		}
	}

	protected void updateProjectFromConfig(ILaunchConfiguration configuration) {
		String projectName = ConfigurationHelper.getProjectName(configuration);
		if (null != projectName) {
			m_selectedProject = JDTUtil.getJavaProject(projectName);
			m_projectText.setText(projectName);
		}
	}

	protected void updateComplianceLevel(ILaunchConfiguration configuration) {
		final String complianceLevel = ConfigurationHelper
				.getComplianceLevel(configuration);

		String[] options = m_complianceLevelCombo.getItems();
		for (int i = 0; i < options.length; i++) {
			if (options[i].equals(complianceLevel)) {
				m_complianceLevelCombo.select(i);
			}
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		ConfigurationHelper.updateLaunchConfiguration(configuration,
				new ConfigurationHelper.LaunchInfo(m_projectText.getText(),
						m_typeOfTestRun, Utils.stringToList(classSelector
								.getText().trim()),
						Utils.stringToList(packageSelector.getText().trim()),
						m_classMethods, groupSelector.getGroupMap(),
						suiteSelector.getText(), m_complianceLevelCombo
								.getText(), m_logLevelCombo.getText()));
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration launchConfig) {
		boolean result = getErrorMessage() == null;

		return result;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return ResourceUtil.getString("TestNGMainTab.tab.label"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return getTestNGImage();
	}

	/**
	 * Method to retreive TestNG icon Image object.
	 * <p>
	 * Code adopted from {@link org.eclipse.jdt.internal.debug.ui.JavaDebugImages}
	 * and {@link org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin} classes.
	 * 
	 * @return
	 */
	public static Image getTestNGImage() {
		final String key = "icon";
		if (m_imageRegistry == null) {
			Display display = Display.getCurrent();
			if (display == null) {
				display = Display.getDefault();
			}
			m_imageRegistry = new ImageRegistry(display);
			m_imageRegistry.put(key, TestNGPlugin
					.getImageDescriptor("main16/testng.gif"));
		}
		return m_imageRegistry.get(key);
	}

	public void validatePage() {
		setErrorMessage(null);
		setMessage(null);

		if (null == m_selectedProject) {
			setErrorMessage(ResourceUtil
					.getString("TestNGMainTab.error.projectnotdefined")); // $NON-NLS-1$

			return;
		}

		if (!m_selectedProject.getProject().exists()) {
			setErrorMessage(ResourceUtil
					.getFormattedString(
							"TestNGMainTab.error.projectnotexists", m_projectText.getText())); //$NON-NLS-1$

			return;
		} else if (!m_selectedProject.getProject().isOpen()) {
			setErrorMessage(ResourceUtil
					.getFormattedString(
							"TestNGMainTab.error.projectnotopen", m_projectText.getText())); //$NON-NLS-1$

			return;
		}

		if (getType() > -1) {
			switch (getType()) {
			case TestNGLaunchConfigurationConstants.CLASS:
				if (classSelector.getText().trim().length() < 1) {
					setErrorMessage(ResourceUtil
							.getString("TestNGMainTab.error.testclassnotdefined")); //$NON-NLS-1$
				}
				break;
			case TestNGLaunchConfigurationConstants.SUITE:
				if (suiteSelector.getText().trim().length() < 1) {
					setErrorMessage(ResourceUtil
							.getString("TestNGMainTab.error.suitenotdefined")); //$NON-NLS-1$
				}
				break;
			case TestNGLaunchConfigurationConstants.METHOD:
				if (methodSelector.getText().trim().length() < 1) {
					setErrorMessage(ResourceUtil
							.getString("TestNGMainTab.error.methodnotdefined")); //$NON-NLS-1$
				}
				break;	
			case TestNGLaunchConfigurationConstants.GROUP:
				if (groupSelector.getText().trim().length() < 1) {
					setErrorMessage(ResourceUtil
							.getString("TestNGMainTab.error.groupnotdefined")); //$NON-NLS-1$
				}
				break;
			case TestNGLaunchConfigurationConstants.PACKAGE:
				if (packageSelector.getText().trim().length() < 1) {
					setErrorMessage(ResourceUtil
							.getString("TestNGMainTab.error.packagenotdefined")); //$NON-NLS-1$
				}
				break;
			default:
				throw new IllegalArgumentException(UNKNOWN_CONSTANT + getType());

			}
		}

	}

	/**
	 * Package access for callbacks.
	 * @param testngType - one of TestNGLaunchConfigurationConstants
	 */
	void handleSearchButtonSelected(int testngType) {
		Object[] types = new Object[0];
		SelectionDialog dialog = null;

		try {
			switch (testngType) {
			case TestNGLaunchConfigurationConstants.CLASS:
				types = TestSearchEngine
						.findTests(getLaunchConfigurationDialog(),
								new Object[] { m_selectedProject },
								Filters.SINGLE_TEST);
				dialog = TestSelectionDialog.createTestTypeSelectionDialog(
						getShell(), m_selectedProject, types,
						Filters.SINGLE_TEST);
				break;
			case TestNGLaunchConfigurationConstants.METHOD:
				
				types = TestSearchEngine.findMethods(
						getLaunchConfigurationDialog(),
						new Object[] { m_selectedProject }, 
						classSelector.getText());
				dialog = TestSelectionDialog.createMethodSelectionDialog(
						getShell(), m_selectedProject, types);
				break;
			case TestNGLaunchConfigurationConstants.SUITE:
				types = TestSearchEngine.findSuites(
						getLaunchConfigurationDialog(),
						new Object[] { m_selectedProject });
				dialog = TestSelectionDialog.createSuiteSelectionDialog(
						getShell(), m_selectedProject, types);
				break;
			case TestNGLaunchConfigurationConstants.PACKAGE:
				types = TestSearchEngine.findPackages(
						getLaunchConfigurationDialog(),
						new Object[] { m_selectedProject });
				dialog = TestSelectionDialog.createPackageSelectionDialog(
						getShell(), m_selectedProject, types);
				break;
			default:
				throw new IllegalArgumentException(UNKNOWN_CONSTANT
						+ testngType);
			}
		} catch (InterruptedException e) {
			TestNGPlugin.log(e);
		} catch (InvocationTargetException e) {
			TestNGPlugin.log(e.getTargetException());
		}
		dialog.setBlockOnOpen(true);
		dialog.setTitle(ResourceUtil
				.getString("TestNGMainTab.testdialog.title")); //$NON-NLS-1$
		if (dialog.open() == Window.CANCEL) {
			return;
		}

		Object[] results = dialog.getResult();
		if ((results == null) || (results.length < 1)) {
			return;
		}
		Object type = results[0];

		if (type != null) {
			switch (testngType) {
			case TestNGLaunchConfigurationConstants.CLASS:
				classSelector.setText((((IType) type).getFullyQualifiedName())
						.trim());
				m_selectedProject = ((IType) type).getJavaProject();
				break;
			case TestNGLaunchConfigurationConstants.METHOD:
				String fullName = ((String)type);
				int index = fullName.lastIndexOf('.');
				String className = fullName.substring(0, index);
				String methodName = fullName.substring(index + 1);
				classSelector.setText(className);
				methodSelector.setText(methodName);
				m_classMethods = new HashMap();
				List methods = new ArrayList();
				methods.add(methodName);
				m_classMethods.put(className, methods); 
				break;
			case TestNGLaunchConfigurationConstants.SUITE:
				IFile file = (IFile) type;
				suiteSelector.setText(file.getProjectRelativePath()
						.toOSString().trim());
				break;
			case TestNGLaunchConfigurationConstants.PACKAGE:
				packageSelector.setText((String) type);
				break;
			default:
				throw new IllegalArgumentException(UNKNOWN_CONSTANT
						+ testngType);
			}
		}

		updateDialog();
	}

	private void handleProjectTextModified() {
		String projectName = m_projectText.getText().trim();
		m_selectedProject = JDTUtil.getJavaProject(projectName);

		updateDialog();
	}

	private void handleProjectButtonSelected() {
		IJavaProject project = ProjectChooserDialog
				.getSelectedProject(getShell());

		if (project == null) {
			return;
		}

		m_selectedProject = project;
		m_projectText.setText(project.getElementName());

		updateDialog();
	}

	private void createRuntimeGroup(Composite parent) {
		//
		// Compliance
		//
		Group group = createGroup(parent, "TestNGMainTab.runtime.type"); //$NON-NLS-1$

		{
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			Label label = new Label(group, SWT.LEFT);
			label.setLayoutData(gd);
			label.setText(ResourceUtil
					.getString("TestNGMainTab.testng.compliance")); // $NON-NLS-1$

			m_complianceLevelCombo = new Combo(group, SWT.DROP_DOWN
					| SWT.READ_ONLY);
			m_complianceLevelCombo
					.add(TestNGLaunchConfigurationConstants.JDK15_COMPLIANCE);
			m_complianceLevelCombo
					.add(TestNGLaunchConfigurationConstants.JDK14_COMPLIANCE);
			m_complianceLevelCombo.select(0);
			GridData gd2 = new GridData(GridData.HORIZONTAL_ALIGN_END
					| GridData.GRAB_HORIZONTAL);
			gd2.widthHint = 70; // HINT: originally minimumWidth (widthHint is supported in older API version)
			m_complianceLevelCombo.setLayoutData(gd2);
			m_complianceLevelCombo.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent evt) {
					updateLaunchConfigurationDialog();
				}
			});
		}

		//
		// Log level
		//
		{
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			Label label = new Label(group, SWT.LEFT);
			label.setLayoutData(gd);
			label.setText(ResourceUtil
					.getString("TestNGMainTab.testng.loglevel")); // $NON-NLS-1$

			m_logLevelCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END
					| GridData.GRAB_HORIZONTAL);
			gd.widthHint = 70;
			m_logLevelCombo.setLayoutData(gd);
			for (int i = 0; i < 11; i++) {
				m_logLevelCombo.add("" + i);
			}
			m_logLevelCombo.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent evt) {
					updateLaunchConfigurationDialog();
				}
			});
		}
	}

	private void createProjectSelectionGroup(Composite comp) {
		Group projectGroup = createGroup(comp, "TestNGMainTab.label.project"); //$NON-NLS-1$

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		m_projectText = new Text(projectGroup, SWT.SINGLE | SWT.BORDER);
		m_projectText.setLayoutData(gd);
		m_projectText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				handleProjectTextModified();
			}
		});
		Button projectSearchButton = new Button(projectGroup, SWT.PUSH);
		projectSearchButton.setText(ResourceUtil
				.getString("TestNGMainTab.label.browse")); //$NON-NLS-1$
		projectSearchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleProjectButtonSelected();
			}
		});
		SWTUtil.setButtonGridData(projectSearchButton);
	}

	private Group createGroup(Composite parent, String groupTitleKey) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
		group.setText(ResourceUtil.getString(groupTitleKey));

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		group.setLayoutData(gd);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);

		return group;
	}

	// package access for callbacks
	void setEnabledRadios(boolean state) {
		for (Iterator it = testngTestSelectors.iterator(); it.hasNext();) {
			TestngTestSelector sel = (TestngTestSelector) it.next();
			sel.enableRadio(state);
		}
	}

	// package not private, for callback access
	void setType(int type) {
		if (type != m_typeOfTestRun) {
			//      ppp("SET TYPE TO " + type + " (WAS " + m_typeOfTestRun + ")");
			m_typeOfTestRun = type;
			//////m_classMethods = null; // we reset it here, because the user has changed settings on front page
			for (Iterator it = testngTestSelectors.iterator(); it.hasNext();) {
				TestngTestSelector sel = (TestngTestSelector) it.next();
				boolean select = (type == sel.getTestngType());
				sel.setRadioSelected(select);
				TestNGPlugin.bold(sel.getRadioButton(), select);
			}
		}
		updateDialog();
	}

	private int getType() {
		return m_typeOfTestRun;
	}

	public void updateDialog() {
		validatePage();
		updateLaunchConfigurationDialog();
	}

	public static void ppp(String s) {
		System.out.println("[TestNGMainTab] " + s);
	}

	public IJavaProject getSelectedProject() {
		return m_selectedProject;
	}

	protected Shell getShell() {
		return super.getShell();
	}

	protected ILaunchConfigurationDialog getLaunchConfigurationDialog() {
		return super.getLaunchConfigurationDialog();
	}

}