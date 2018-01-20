package net.refractions.chyf.rest;

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
		chyfDatastore = new ChyfDatastore(servletContext.getInitParameter("chyfDataDir"));
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
