package mpi.aidalight.service.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AidalightServiceContext implements ServletContextListener{
	
	private AidalightManager aidalightManager;
	
	private static final Logger logger_ = LoggerFactory.getLogger(AidalightServiceContext.class);

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		logger_.info("Destroying the context..........");
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		logger_.info("Starting disambiguation service......");
		
		aidalightManager = new AidalightManager();
		aidalightManager.init();

	}

}