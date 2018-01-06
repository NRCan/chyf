package net.refractions.chyf.rest.controllers;


import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.rest.SharedParameters;
import net.refractions.chyf.rest.messageconverters.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eflowpath")
public class EFlowpathController {
	
	@Autowired
	private ChyfDatastore chyfDatastore;
	
	@RequestMapping(value = "/{id}", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getEFlowpath(@PathVariable("id") int id,
			SharedParameters params, BindingResult bindingResult) {
		
		ApiResponse resp = new ApiResponse(chyfDatastore.getEdge(id));
		resp.setParams(params);
		return resp;
	}
	
}
