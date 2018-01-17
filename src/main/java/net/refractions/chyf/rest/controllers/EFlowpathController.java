package net.refractions.chyf.rest.controllers;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.indexing.BboxIntersectsFilter;
import net.refractions.chyf.rest.ReverseGeocodeParameters;
import net.refractions.chyf.rest.SharedParameters;
import net.refractions.chyf.rest.exceptions.InvalidParameterException;
import net.refractions.chyf.rest.messageconverters.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eflowpath")
@CrossOrigin
public class EFlowpathController {
	
	@Autowired
	private HyGraph hyGraph;
	
	@RequestMapping(value = "/{id}", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathById(@PathVariable("id") int id,
			SharedParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		
		ApiResponse resp = new ApiResponse(hyGraph.getEFlowpath(id));
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/near", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsNear(ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}
		
		
		ApiResponse resp = new ApiResponse(hyGraph.findEFlowpaths(params.getPoint(), params.getMaxFeatures(), null, null));
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/within", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsWithin(ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getBbox() == null) {
			String errMsg = "The bbox parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		ApiResponse resp = new ApiResponse(hyGraph.findEFlowpaths(
				ChyfDatastore.GEOMETRY_FACTORY.createPoint(params.getBbox().centre()), 
				params.getMaxFeatures(),
				params.getMaxDistance(), 
				new BboxIntersectsFilter<EFlowpath>(params.getBbox())));
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/flowsFrom", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathFlowsFrom(ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		ApiResponse resp = new ApiResponse(hyGraph.getEFlowpath(params.getPoint()));
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/upstreamOf", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpathsUpstreamOf(ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		ApiResponse resp = new ApiResponse(hyGraph.getUpstreamEFlowpaths(params.getPoint(), params.getMaxFeatures()));
		resp.setParams(params);
		return resp;
	}


}





