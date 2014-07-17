package mpi.aidalight.service.web;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class AidalightApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(RequestProcessor.class);
		return classes;
	}

}