package net.refractions.chyf.rest;

import java.io.File;

import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.HyGraph;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChyfApplication {
	
	private ChyfDatastore chyfDatastore;
	
    @Autowired
	public ChyfApplication(ServletContext servletContext) {
    	String dataStore = servletContext.getInitParameter("chyfDataStore");
		String dir = null;
		
    	if (dataStore.equals("filestore")) {
    		String[] dirs = {
	                 servletContext.getInitParameter("chyfDataDir"), 
	                 "C:\\projects\\chyf-pilot\\data\\",
	                 "/data/chyf/"
			};

			for(String d : dirs) {
				if(new File(d).isDirectory()) {
					dir = d;
					break;
				}
			}

			chyfDatastore = new ChyfDatastore(dir);
    		
    	} else if (dataStore.equals("database")) {
    		
    		chyfDatastore = new ChyfDatastore();
    		
    	}
    	
	}
	
	@Bean
	public HyGraph getHyGraph() {
		return chyfDatastore.getHyGraph();
	}
	
	@PreDestroy
	public void preDestroy() {
		//chyfDatastore.close();
	}
	
}
