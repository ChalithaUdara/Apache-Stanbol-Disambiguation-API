package org.apache.stanbol.enhancer.engine.aidalight_disambiguation;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

public class MockComponentContext implements ComponentContext {

	protected final Dictionary<String, Object> properties;

	public MockComponentContext() {
		properties = new Hashtable<String, Object>();
	}
	
	public MockComponentContext(Dictionary<String, Object> properties) {
		this.properties = properties;
	}

	public Dictionary<String, Object> getProperties() {
		return properties;
	}

	public Object locateService(String name) {
		return null;
	}

	public Object locateService(String name, ServiceReference reference) {
		return null;
	}

	public Object[] locateServices(String name) {
		return null;
	}

	public BundleContext getBundleContext() {
		return null;
	}

	public Bundle getUsingBundle() {
		return null;
	}

	public ComponentInstance getComponentInstance() {
		return null;
	}

	public void enableComponent(String name) {

	}

	public void disableComponent(String name) {

	}

	public ServiceReference getServiceReference() {
		return null;
	}

}
