package us.dot.its.jpo.conflictmonitor.jmx;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseBean {
 
	
	
	
	private final MBeanServer beanServer;
	
	private final ObjectName name;
	private final String className;
	private final String description;
	private final String error;
	
	public BaseBean(MBeanServer beanServer, ObjectName name, String description, Set<String> attributes) throws Exception {
		this.beanServer = beanServer;
		this.name = name;
		this.description = description;
		MBeanInfo beanInfo = beanServer.getMBeanInfo(name);
		this.className = beanInfo.getClassName();
		this.addAttributes(beanInfo, attributes);
		this.addNotifications(beanInfo);
		this.error = null;
	}

	public BaseBean(String error) {
		this.error = error;
		this.name = null;
		this.beanServer = null;
		this.className = null;
		this.description = null;
	}
	
	
	private final Map<String, Object> allAttributes = new TreeMap<String, Object>();
	
	private final Map<String, String[]> notifications = new TreeMap<String, String[]>();

	public String getError() {
		return error;
	}

	public String getDomain() {
		return name != null ? name.getDomain() : null;
	}

	public String getName() {
		return name != null ? name.getCanonicalName() : null;
	}
	
	
	public ObjectName getObjectName() {
		return name;
	}
	
	public String getClassName() {
		return className;
	}

	public String getDescription() {
		return description;
	}

	public String getContext() {
		if (name != null && name.getKeyPropertyList().containsKey("context")) {
			return name.getKeyProperty("context").replace("/", "");
		}
		return null;
	}

	public String getShortName() {
		if (name != null && name.getKeyPropertyList().containsKey("name")) {
			return name.getKeyProperty("name").replace("\"", "");
		}
		return null;
	}
	

	public Map<String, Object> getAllAttributes() {
		return allAttributes;
	}

	public Object getAttribute(String key) {
		if (allAttributes.containsKey(key)) {
			return allAttributes.get(key);
		} else {
			return null;
		}
	}
	

	
	private void addAttributes(MBeanInfo beanInfo, Set<String> attributes) throws Exception {
		for (MBeanAttributeInfo attrib : beanInfo.getAttributes()) {
			if (attributes == null || attributes.size() == 0 || attributes.contains(attrib.getName())) {
			    addAttribute(allAttributes, attrib);
			}
		}
	}
	
	private void addNotifications(MBeanInfo beanInfo) throws Exception {
		for (MBeanNotificationInfo notif : beanInfo.getNotifications()) {
			String[] notifTypes = notif.getNotifTypes();
			//String notifStr = String.join(" ", notifTypes);
			notifications.put(notif.getName(), notifTypes);
		}
	}
	
	
	
	private void addAttribute(Map<String, Object> attributes, MBeanAttributeInfo attrib) {
		try {
			Object attribValue = beanServer.getAttribute(name, attrib.getName());
			if (attribValue != null) {
				if (attribValue instanceof String || attribValue instanceof Number) {
					attributes.put(attrib.getName(), attribValue);
				} else {
					var jsonMapper = new ObjectMapper();
					var json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(attribValue);
					attributes.put(attrib.getName(), json);
				}
			}
		} catch (Exception ae) {
			attributes.put(attrib.getName(), ae.getMessage());
		}
	}
}

