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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.rest.HygraphParameters;
import net.refractions.chyf.rest.exceptions.InvalidParameterException;
import net.refractions.chyf.rest.messageconverters.ApiResponse;
import net.refractions.util.StopWatch;

@RestController
@RequestMapping("/waterbodyFlow")
@CrossOrigin(allowCredentials="true",allowedHeaders="Autorization")
public class WaterbodyFlowController {

	@Autowired
	private HyGraph hyGraph;
	
	
	@RequestMapping(value = "/multiDimensionalDownstreamOf", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getMultiDimensionalDownstreamOfLocation(HygraphParameters params, BindingResult bindingResult) {
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
		ApiResponse resp = new ApiResponse(hyGraph.getDownstreamMultiDimensional(hyGraph.getECatchment(params.getPoint()), params.getMaxFeatures() ));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}
	
	
	@RequestMapping(value = "/multiDimensionalUpstreamOf", method = {RequestMethod.GET,RequestMethod.POST})
	public ApiResponse getMultiDimensionalUpstreamOfLocation(HygraphParameters params, BindingResult bindingResult) {
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
		ApiResponse resp = new ApiResponse(hyGraph.getUpstreamMultiDimensional(hyGraph.getECatchment(params.getPoint()), params.getMaxFeatures() ));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

}
