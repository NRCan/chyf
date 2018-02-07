/**
 * Copyright 2008-2015, Province of British Columbia
 * All rights reserved.
 */
package net.refractions.chyf.rest;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	
	private static final String CSP_POLICY = "script-src * 'unsafe-eval' 'unsafe-inline'";
	
	@Override
	protected void configure(HttpSecurity httpSec) throws Exception {
		httpSec.csrf().disable()
				.headers()
				.contentTypeOptions().and()
				.xssProtection().and()
				.cacheControl().and()
				.httpStrictTransportSecurity().and()
				.frameOptions().and()
				.addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy", CSP_POLICY))
				.and().antMatcher("/**");
	}
}
