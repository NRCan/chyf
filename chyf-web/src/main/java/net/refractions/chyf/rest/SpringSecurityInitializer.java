/**
 * Copyright 2008-2015, Province of British Columbia
 * All rights reserved.
 */
package net.refractions.chyf.rest;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

public class SpringSecurityInitializer extends AbstractSecurityWebApplicationInitializer {
	public SpringSecurityInitializer() {
		super(WebSecurityConfig.class);
	}
}
