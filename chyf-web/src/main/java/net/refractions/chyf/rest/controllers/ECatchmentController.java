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
package net.refractions.chyf.rest.controllers;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.indexing.BboxIntersectsFilter;
import net.refractions.chyf.rest.HygraphParameters;
import net.refractions.chyf.rest.SharedParameters;
import net.refractions.chyf.rest.exceptions.InvalidParameterException;
import net.refractions.chyf.rest.messageconverters.ApiResponse;
import net.refractions.util.StopWatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ecatchment")
@CrossOrigin(allowCredentials="true",allowedHeaders="Autorization")
public class ECatchmentController {

	@Autowired
	private HyGraph hyGraph;
	
	@RequestMapping(value = "/{id}", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentById(@PathVariable("id") int id,
			SharedParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		
		ApiResponse resp = new ApiResponse(hyGraph.getECatchment(id));
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/near", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentsNear(HygraphParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}				

		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.findECatchments(params.getPoint(), params.getMaxFeatures(), params.getMaxDistance(), null));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/within", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentsWithin(HygraphParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getBbox() == null) {
			String errMsg = "The bbox parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}
		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.findECatchments(
				ChyfDatastore.GEOMETRY_FACTORY.createPoint(params.getBbox().centre()), 
				params.getMaxFeatures(),
				params.getMaxDistance(), 
				new BboxIntersectsFilter<ECatchment>(params.getBbox())));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/containing", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentsContaining(HygraphParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		StopWatch sw = new StopWatch();
		sw.start();
		ECatchment containing = hyGraph.getECatchment(params.getPoint());
		ApiResponse resp = new ApiResponse(containing);
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/{id}/upstream", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentsUpstream(@PathVariable("id") int id, 
			HygraphParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getUpstreamECatchments(hyGraph.getECatchment(id), params.getMaxFeatures()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/{id}/downstream", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentsDownstream(@PathVariable("id") int id, 
			HygraphParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getDownstreamECatchments(hyGraph.getECatchment(id), params.getMaxFeatures()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/upstreamOf", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentsUpstreamOf(HygraphParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getUpstreamECatchments(hyGraph.getECatchment(params.getPoint()), params.getMaxFeatures()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/downstreamOf", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentsDownstreamOf(HygraphParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getDownstreamECatchments(hyGraph.getECatchment(params.getPoint()), params.getMaxFeatures()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

}
