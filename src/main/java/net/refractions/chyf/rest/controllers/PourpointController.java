package net.refractions.chyf.rest.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.rest.PourpointParameters;
import net.refractions.chyf.rest.exceptions.InvalidParameterException;
import net.refractions.chyf.rest.messageconverters.ApiResponse;

@RestController
@RequestMapping("/pourpoint")
@CrossOrigin(allowCredentials="true",allowedHeaders="Autorization")
public class PourpointController {

	
	@Autowired
	private HyGraph hyGraph;
	
	@RequestMapping(value = "/compute", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse processPourpoint(PourpointParameters params,
			BindingResult bindingResult) {
		
		if(bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
//		PourpointParameters params = new PourpointParameters();
//		params.setOutput(output);
//		params.setPoints(points);
		
		params.resolveAndValidate();
		
		PourpointEngine engine = new PourpointEngine(params.getPourpoints(), hyGraph, params.getRemoveHoles());
		PourpointOutput pout = engine.compute(params.getOutputTypes());
		
		ApiResponse resp = new ApiResponse(pout);
		resp.setParams(params);
		
		return resp;
	}
}
