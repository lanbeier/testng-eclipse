package org.testng.eclipse.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jdt.core.IJavaProject;
import org.testng.reporters.XMLStringBuffer;

public class ClassMethodsSuite extends CustomSuite {
	protected Collection/* <String> */m_classNames;
	protected Map/* <String, Collection<String> */m_classMethods;
	protected boolean m_useMethods;

	public ClassMethodsSuite(final IJavaProject project,
			final Collection classNames, final Map classMethods,
			final Map parameters, final String annotationType,
			final int logLevel) {
		super(project, null, parameters, annotationType, logLevel);
		m_classNames = classNames;
		if (m_classMethods == null)
			m_useMethods = false;
	}

	public ClassMethodsSuite(final IJavaProject project,
			final String className, String method, final Map parameters,
			final String annotationType, final int logLevel) {
		super(project, null, parameters, annotationType, logLevel);
		m_classNames = new ArrayList();
		m_classNames.add(className);
		m_useMethods = false;
		if (method != null) {
			m_classMethods = new HashMap();
			List methods = new ArrayList();
			methods.add(method);
			m_classMethods.put(className, method);
			m_useMethods = true;
		}
	}

	protected String getTestName() {
		return m_classNames.size() == 1 ? (String) m_classNames.iterator()
				.next() : "classes";
	}

	protected void classesElement(XMLStringBuffer suiteBuffer) {
		if (m_useMethods) {
			generateClassesWithMethodsElement(suiteBuffer);
		} else {
			generateDefaultClassesElement(suiteBuffer, m_classNames);
		}
	}

	protected void generateClassesWithMethodsElement(XMLStringBuffer suiteBuffer) {
		suiteBuffer.push("classes");

		for (Iterator it = m_classMethods.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			String className = (String) entry.getKey();
			Properties classAttrs = new Properties();
			classAttrs.setProperty("name", className);

			List methodNames = (List) entry.getValue();
			if (null == methodNames) {
				suiteBuffer.addEmptyElement("class", classAttrs);
			} else {
				suiteBuffer.push("class", classAttrs);
				suiteBuffer.push("methods");

				for (Iterator itNames = methodNames.iterator(); itNames
						.hasNext();) {
					Properties methodAttrs = new Properties();
					methodAttrs.setProperty("name", (String) itNames.next());
					suiteBuffer.addEmptyElement("include", methodAttrs);
				}

				suiteBuffer.pop("methods");
				suiteBuffer.pop("class");
			}
		}

		suiteBuffer.pop("classes");
	}
}
