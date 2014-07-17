package mpi.aidalight.service.web;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mpi.aidalight.DataStore;
import mpi.aidalight.entitycoherence.NED;
import mpi.util.ClassPathLoader;

/**
 * This class is responsible for initializing data required for disambiguation
 * @author chalitha
 *
 */
public class AidalightManager {
	
	private static final Logger logger_ = LoggerFactory.getLogger(AidalightManager.class);
	
	private static String LSH_PROP_PATH = "lsh.properties";
	
	/**
	 * Initialize resources
	 */
	public void init(){
		boolean uselsh = Boolean.parseBoolean(this.getUselsh());
		if(uselsh){
			NED.partlyMatchingMentionEntitySearch = true;
		}
		else{
			NED.partlyMatchingMentionEntitySearch = false;
		}
		
		DataStore.init();

	}
	
	/**
	 * read lsh properties file to determine whether to use
	 * least sensitive hashing or not
	 * @return
	 */
	public String getUselsh(){
		String uselsh = null;
		try {
			Properties lshprop = ClassPathLoader.getPropertiesFromClasspath(LSH_PROP_PATH);
			uselsh = lshprop.getProperty("uselsh");
		} catch (IOException e) {
			logger_.error("could not find properties file for lsh");
		}
		return uselsh;
	}

}
