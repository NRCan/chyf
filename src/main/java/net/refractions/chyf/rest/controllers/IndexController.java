package net.refractions.chyf.rest.controllers;

import net.refractions.chyf.hygraph.HyGraph;
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
@RequestMapping("/index")
@CrossOrigin
public class IndexController {

	@Autowired
	private HyGraph hyGraph;

	@RequestMapping(value = "/catchment/{id}", method = {RequestMethod.GET,RequestMethod.POST})
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
