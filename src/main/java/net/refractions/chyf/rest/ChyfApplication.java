/**
 * Copyright 2008-2015, Province of British Columbia
 * All rights reserved.
 */
package net.refractions.chyf.rest;

import javax.annotation.PreDestroy;

import net.refractions.chyf.ChyfDatastore;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChyfApplication {
	private final String DATA_DIR = "C:\\apps\\chyf\\";
	private ChyfDatastore chyfDatastore;
	private GeotoolsGeometryReprojector reprojector = new GeotoolsGeometryReprojector();
	
	public ChyfApplication() {
		chyfDatastore = new ChyfDatastore(DATA_DIR);
	}
	
	@Bean
	public ChyfDatastore chyfDatastore() {
		return chyfDatastore;
	}
	
	@PreDestroy
	public void preDestroy() {
		//chyfDatastore.close();
	}
	
}
