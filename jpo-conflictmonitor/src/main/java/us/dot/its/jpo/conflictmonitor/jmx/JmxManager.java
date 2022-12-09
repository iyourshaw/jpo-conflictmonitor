package us.dot.its.jpo.conflictmonitor.jmx;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.lang3.exception.ExceptionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;




public class JmxManager {
	
	Logger log = LoggerFactory.getLogger(JmxManager.class);
	
	private final MBeanServer server;
	
	public JmxManager() {
		this.server = ManagementFactory.getPlatformMBeanServer();
	}
	
	
	
	
	public BeanList listAllBeans() {
		var beanList = new BeanList();
		try {
			Set<ObjectName> objectNames = server.queryNames(null, null);
			for (ObjectName name : objectNames) {
				var beanInfo = server.getMBeanInfo(name);
				var beanDesc = beanInfo.getDescription();
				var bean = new BaseBean(server, name, beanDesc, null);
				beanList.add(bean);
			}
		} catch (Exception e) {
			var sb = new StringBuilder();
            sb.append(String.format("%s: %s%n%s", e.getClass().toString(), e.getMessage(), ExceptionUtils.getStackTrace(e)));
			beanList.setError(sb.toString());
		}
		return beanList;
	}
	
	/**
	 * @param domain - If present find only beans with this domain in their name
	 * @param keyFilter - If present, find only beans containing this string in the key property string.
	 * @return BeanList
	 */
	public BeanList listBeans(String domain, String keyFilter, String classNameFilter, Set<String> attributes) {
		var beanList = new BeanList();
		try {
			Set<ObjectName> objectNames = server.queryNames(null, null);
			for (ObjectName name : objectNames) {
				var beanInfo = server.getMBeanInfo(name);
				if (classNameFilter == null || beanInfo.getClassName().contains(classNameFilter)) {
					var beanDesc = beanInfo.getDescription();
					if ((domain == null || name.getDomain().contains(domain))
							&& (keyFilter == null || name.getKeyPropertyList().size() == 0
							|| name.getCanonicalKeyPropertyListString().contains(keyFilter))) {
						var bean = new BaseBean(server, name, beanDesc, attributes);
						beanList.add(bean);
					}
				}
			}

		} catch (Exception e) {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("%s: %s%n%s", e.getClass().toString(), e.getMessage(), ExceptionUtils.getStackTrace(e)));
			beanList.setError(sb.toString());
		}
		return beanList;
	}
	
	
	public BaseBean getBean(String name, String[] attributes) throws Exception {
		var objectName = new ObjectName(name);

		var attributeSet = (attributes != null && attributes.length > 0) ? Sets.newHashSet(attributes) : null;
		return getBean(objectName, attributeSet);
	}
	
	public BaseBean getBean(ObjectName name, Set<String> attributes) throws Exception {
		MBeanInfo beanInfo = server.getMBeanInfo(name);
		String beanDesc = beanInfo.getDescription();
		var bean = new BaseBean(server, name, beanDesc, attributes);
		return bean;
	}

	public BaseBean getBean(String name) throws Exception {
		var objectName = new ObjectName(name);
		MBeanInfo beanInfo = server.getMBeanInfo(objectName);
		var bean = new BaseBean(server, objectName, beanInfo.getDescription(), null);
		return bean;
	}
	
	public BaseBean getBasicPoolAttributes(ObjectName name) throws Exception {
		return getBean(name, Sets.newHashSet("Size", "Active", "Idle"));
	}
	
	
}
