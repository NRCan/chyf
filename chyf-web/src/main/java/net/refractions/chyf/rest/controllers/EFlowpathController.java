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
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.enumTypes.Rank;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.indexing.BboxIntersectsFilter;
import net.refractions.chyf.indexing.Filter;
import net.refractions.chyf.indexing.PredicateFilter;
import net.refractions.chyf.rest.FilterParameters;
import net.refractions.chyf.rest.PredicateParameter;
import net.refractions.chyf.rest.HygraphParameters;
import net.refractions.chyf.rest.SharedParameters;
import net.refractions.chyf.rest.exceptions.InvalidParameterException;
import net.refractions.chyf.rest.messageconverters.ApiResponse;
import net.refractions.util.StopWatch;
import net.refractions.util.UuidUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eflowpath")
@CrossOrigin(allowCredentials="true",allowedHeaders="Autorization")
public class EFlowpathController {
	
	@Autowired
	private HyGraph hyGraph;
	
	@RequestMapping(value = "/{id}", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathById(@PathVariable("id") int id,
			SharedParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		
		StopWatch sw = new StopWatch();
		sw.start();		
		ApiResponse resp = new ApiResponse(hyGraph.getEFlowpath(id));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/near", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsNear(HygraphParameters params, BindingResult bindingResult) {
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
		ApiResponse resp = new ApiResponse(hyGraph.findEFlowpaths(params.getPoint(), params.getMaxFeatures(), params.getMaxDistance(), null));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/within", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsWithin(HygraphParameters params, BindingResult bindingResult) {
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
		ApiResponse resp = new ApiResponse(hyGraph.findEFlowpaths(
				ChyfDatastore.GEOMETRY_FACTORY.createPoint(params.getBbox().centre()), 
				params.getMaxFeatures(),
				params.getMaxDistance(), 
				new BboxIntersectsFilter<EFlowpath>(params.getBbox())));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/flowsFrom", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathFlowsFrom(HygraphParameters params, BindingResult bindingResult) {
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
		ApiResponse resp = new ApiResponse(hyGraph.getEFlowpath(params.getPoint()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/{id}/upstream", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsUpstream(@PathVariable("id") int id,
			HygraphParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getUpstreamEFlowpaths(hyGraph.getEFlowpath(id), params.getMaxFeatures()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/{id}/downstream", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsDownstream(@PathVariable("id") int id,
			HygraphParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getDownstreamEFlowpaths(hyGraph.getEFlowpath(id), params.getMaxFeatures()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/upstreamOf", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsUpstreamOf(HygraphParameters params, BindingResult bindingResult) {
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
		ApiResponse resp = new ApiResponse(hyGraph.getUpstreamEFlowpaths(hyGraph.getEFlowpath(params.getPoint()), params.getMaxFeatures()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/downstreamOf", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsDownstreamOf(HygraphParameters params, BindingResult bindingResult) {
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
		ApiResponse resp = new ApiResponse(hyGraph.getDownstreamEFlowpaths(hyGraph.getEFlowpath(params.getPoint()), params.getMaxFeatures()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/filter", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsByFilter(FilterParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		
		StopWatch sw = new StopWatch();
		sw.start();
		Filter<EFlowpath> filter;
		switch(params.getProperty().toLowerCase()) {
			case "name": filter = new PredicateFilter<EFlowpath>(EFlowpath::getName, PredicateParameter.convert(params.getPredicate()).get(), params.getValue()); break;
			case "nameid": filter = new PredicateFilter<EFlowpath>(EFlowpath::getNameId, PredicateParameter.convert(params.getPredicate()).get(), UuidUtil.UuidFromString(params.getValue())); break;
			case "type": filter = new PredicateFilter<EFlowpath>(EFlowpath::getType, PredicateParameter.convert(params.getPredicate()).get(), FlowpathType.convert(params.getValue())); break;
			case "rank": filter = new PredicateFilter<EFlowpath>(EFlowpath::getRank, PredicateParameter.convert(params.getPredicate()).get(), Rank.convert(params.getValue())); break;
			case "strahleror": filter = new PredicateFilter<EFlowpath>(EFlowpath::getStrahlerOrder, PredicateParameter.convert(params.getPredicate()).get(), Integer.parseInt(params.getValue())); break;
			case "hortonor": filter = new PredicateFilter<EFlowpath>(EFlowpath::getHortonOrder, PredicateParameter.convert(params.getPredicate()).get(), Integer.parseInt(params.getValue())); break;
			case "hackor": filter = new PredicateFilter<EFlowpath>(EFlowpath::getHackOrder, PredicateParameter.convert(params.getPredicate()).get(), Integer.parseInt(params.getValue())); break;
			case "length": filter = new PredicateFilter<EFlowpath>(EFlowpath::getLength, PredicateParameter.convert(params.getPredicate()).get(), Double.parseDouble(params.getValue())); break;
			default:
				String errMsg = "The property parameter must be one of (name, nameId, type, rank, strahleror, hortonor, hackor, length).";
				throw new IllegalArgumentException(errMsg);
		}
		
		ApiResponse resp = new ApiResponse(hyGraph.getEFlowpaths(filter));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

}





