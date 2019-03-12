/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
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
