package org.testng.eclipse.launch;

import org.testng.eclipse.TestNGPlugin;

/**
 * Constants used to pass information from the launch manager to the launcher.
 * 
 * @author cbeust
 */
public abstract class TestNGLaunchConfigurationConstants {
	public static final String JDK15_COMPLIANCE = "JDK";
	public static final String JDK14_COMPLIANCE = "javadoc";

	public static final String ID_TESTNG_APPLICATION = "org.testng.eclipse.launchconfig"; //$NON-NLS-1$

	private static String make(String s) {
		return TestNGPlugin.PLUGIN_ID + "." + s;
	}

	/**
	 * Root directory of the tests. If this property is set, TestNG will run all
	 * the tests contained in this directory.
	 */
	public static final String DIRECTORY_TEST_LIST = make("DIRECTORY_TEST_LIST"); //$NON-NLS-1$

	/**
	 * List of classes
	 */
	public static final String CLASS_TEST_LIST = make("CLASS_TEST_LIST"); //$NON-NLS-1$

	/**
	 * List of methods. This is replaced by {@link #ALL_METHODS_LIST}.
	 */
	public static final String METHOD_TEST_LIST = make("METHOD_TEST_LIST"); //$NON-NLS-1$

	public static final String ALL_METHODS_LIST = make("ALL_CLASS_METHODS"); //$NON-NLS-1$

	/**
	 * List of packages
	 */
	public static final String PACKAGE_TEST_LIST = make("PACKAGE_TEST_LIST"); //$NON-NLS-1$

	/**
	 * List of sources
	 */
	public static final String SOURCE_TEST_LIST = make("SOURCE_TEST_LIST"); //$NON-NLS-1$

	/**
	 * List of groups
	 */
	public static final String GROUP_LIST = make("GROUP_LIST"); //$NON-NLS-1$

	public static final String GROUP_CLASS_LIST = make("GROUP_LIST_CLASS");

	public static final int DEFAULT_LOG_LEVEL = 2;

	/**
	 * List of suites
	 */
	public static final String SUITE_TEST_LIST = make("SUITE_TEST_LIST"); //$NON-NLS-1$

	/**
	 * Port of the launcher
	 */
	public static final String PORT = make("PORT"); //$NON-NLS-1$

	public static final String TESTNG_RUN_NAME_ATTR = make("SUBNAME"); //$NON-NLS-1$

	public static final String TEMP_SUITE_LIST = make("TEMP_SUITE_LIST"); //$NON-NLS-1$

	public static final String TYPE = make("TYPE"); //$NON-NLS-1$ 

	public static final String VM_ENABLEASSERTION_OPTION = "-ea";

	// What kind of run we are doing
	// This would be a nice place for an enum when jdk1.5 or later can be
	// required.
	public static final int SINGLE = 1;
	public static final int SUITE = 2;
	public static final int CONTAINER = 3;
	public static final String PARAMS = make("PARAMETERS");

	public static final String ATTR_NO_DISPLAY = TestNGPlugin.PLUGIN_ID
			+ ".NO_DISPLAY"; //$NON-NLS-1$

	public static final String ATTR_PORT = TestNGPlugin.PLUGIN_ID + ".PORT"; //$NON-NLS-1$

	/**
	 * The test method, or "" iff running the whole test type.
	 */
	public static final String ATTR_TEST_METHOD_NAME = TestNGPlugin.PLUGIN_ID
			+ ".TESTNAME"; //$NON-NLS-1$

	public static final String ATTR_KEEPRUNNING = TestNGPlugin.PLUGIN_ID
			+ ".KEEPRUNNING_ATTR"; //$NON-NLS-1$
	/**
	 * The launch container, or "" iff running a single test type.
	 */
	public static final String ATTR_TEST_CONTAINER = TestNGPlugin.PLUGIN_ID
			+ ".CONTAINER"; //$NON-NLS-1$

	public static final String ATTR_FAILURES_NAMES = TestNGPlugin.PLUGIN_ID
			+ ".FAILURENAMES"; //$NON-NLS-1$
	
	public static final String ATTR_ENABLED_GROUPS = TestNGPlugin.PLUGIN_ID + ".ENABLEDGROUPS";

	public static final String ATTR_DISABLED_GROUPS = TestNGPlugin.PLUGIN_ID + ".DISABLEDGROUPS";
	public static final String MODE_RUN_QUIETLY_MODE = "runQuietly";
	

}
