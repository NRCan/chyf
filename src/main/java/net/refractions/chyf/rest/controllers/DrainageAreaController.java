package net.refractions.chyf.rest.controllers;

import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.rest.ReverseGeocodeParameters;
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
@RequestMapping("/drainageArea")
@CrossOrigin
public class DrainageAreaController {

	@Autowired
	private HyGraph hyGraph;
	
	@RequestMapping(value = "/upstreamOf/ecatchment/{id}", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getDrainageAreaUpstreamOfCatchment(@PathVariable("id") int id, 
			ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getUpstreamDrainageArea(hyGraph.getECatchment(id)));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/downstreamOf/ecatchment/{id}", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getDrainageAreaDownstreamOfCatchment(@PathVariable("id") int id, 
			ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getDownstreamDrainageArea(hyGraph.getECatchment(id)));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/upstreamOf/eflowpath/{id}", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getDrainageAreaUpstreamOfFlowpath(@PathVariable("id") int id, 
			ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getUpstreamDrainageArea(hyGraph.getEFlowpath(id).getCatchment()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/downstreamOf/eflowpath/{id}", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getDrainageAreaDownstreamOfFlowpath(@PathVariable("id") int id, 
			ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(hyGraph.getDownstreamDrainageArea(hyGraph.getEFlowpath(id).getCatchment()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/upstreamOf", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getDrainageAreaUpstreamOfLocation(ReverseGeocodeParameters params, BindingResult bindingResult) {
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
		ApiResponse resp = new ApiResponse(hyGraph.getUpstreamDrainageArea(hyGraph.getECatchment(params.getPoint())));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/downstreamOf", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getDrainageAreaDownstreamOfLocation(ReverseGeocodeParameters params, BindingResult bindingResult) {
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
		ApiResponse resp = new ApiResponse(hyGraph.getDownstreamDrainageArea(hyGraph.getECatchment(params.getPoint())));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

}
