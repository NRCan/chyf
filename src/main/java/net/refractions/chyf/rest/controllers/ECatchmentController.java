package net.refractions.chyf.rest.controllers;

import java.util.List;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.indexing.BboxIntersectsFilter;
import net.refractions.chyf.indexing.ECatchmentContainsPointFilter;
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
@RequestMapping("/ecatchment")
@CrossOrigin
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
	public ApiResponse getECatchmentsNear(ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}
				
		ApiResponse resp = new ApiResponse(hyGraph.findECatchments(params.getPoint(), params.getMaxFeatures(), null, null));
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/within", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentsWithin(ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getBbox() == null) {
			String errMsg = "The bbox parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		ApiResponse resp = new ApiResponse(hyGraph.findECatchments(
				ChyfDatastore.GEOMETRY_FACTORY.createPoint(params.getBbox().centre()), 
				params.getMaxFeatures(),
				params.getMaxDistance(), //Math.min((int)Math.round(params.getBbox().maxExtent()), params.getMaxDistance() == null ? Integer.MAX_VALUE : params.getMaxDistance()), 
				new BboxIntersectsFilter<ECatchment>(params.getBbox())));
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/containing", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentsContaining(ReverseGeocodeParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();
		
		if(params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		List<ECatchment> eCatchments = hyGraph.findECatchments(params.getPoint(), 1, null, 
				new ECatchmentContainsPointFilter(params.getPoint()));
		ECatchment containing = null;
		if(eCatchments.size() > 0) {
			containing = eCatchments.get(0);
		}
		ApiResponse resp = new ApiResponse(containing);
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/indexNode/{id}", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getECatchmentIndexNodeById(@PathVariable("id") int id,
			SharedParameters params, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		
		ApiResponse resp = new ApiResponse(hyGraph.getECatchmentIndexNode(id));
		resp.setParams(params);
		return resp;
	}

}
