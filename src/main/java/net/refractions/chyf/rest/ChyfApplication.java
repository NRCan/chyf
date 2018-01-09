/**
 * Copyright 2008-2015, Province of British Columbia
 * All rights reserved.
 */
package net.refractions.chyf.rest;

import javax.annotation.PreDestroy;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hydrograph.HyGraph;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChyfApplication {
	private static final String DATA_DIR = "C:\\apps\\chyf\\";
	private ChyfDatastore chyfDatastore;
	
	public ChyfApplication() {
		chyfDatastore = new ChyfDatastore(DATA_DIR);
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
