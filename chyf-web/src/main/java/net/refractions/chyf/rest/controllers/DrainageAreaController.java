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

import net.refractions.chyf.hygraph.Coverage;
import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.HyGraph;

import net.refractions.chyf.rest.HygraphParameters;
import net.refractions.chyf.rest.exceptions.InvalidParameterException;
import net.refractions.chyf.rest.messageconverters.ApiResponse;
import net.refractions.util.StopWatch;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.net.HttpURLConnection;
import java.net.URL;


//encoder l'URL
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import net.refractions.chyf.rest.controllers.CubeController;


@RestController
@RequestMapping("/drainageArea")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "Autorization")
public class DrainageAreaController {
	

	@Autowired
	private HyGraph hyGraph;
	
	//permet de faire les requetes get ou post vers le cube
	private CubeController cubeController = new CubeController();


	@RequestMapping(value = "/upstreamOf/ecatchment/{id}", method = { RequestMethod.GET, RequestMethod.POST })
	public ApiResponse getDrainageAreaUpstreamOfCatchment(@PathVariable("id") int id, HygraphParameters params,
			BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();

		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(
				hyGraph.getUpstreamDrainageArea(hyGraph.getECatchment(id), params.getRemoveHoles()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/downstreamOf/ecatchment/{id}", method = { RequestMethod.GET, RequestMethod.POST })
	public ApiResponse getDrainageAreaDownstreamOfCatchment(@PathVariable("id") int id, HygraphParameters params,
			BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();

		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(
				hyGraph.getDownstreamDrainageArea(hyGraph.getECatchment(id), params.getRemoveHoles()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/upstreamOf/eflowpath/{id}", method = { RequestMethod.GET, RequestMethod.POST })
	public ApiResponse getDrainageAreaUpstreamOfFlowpath(@PathVariable("id") int id, HygraphParameters params,
			BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();

		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(
				hyGraph.getUpstreamDrainageArea(hyGraph.getEFlowpath(id).getCatchment(), params.getRemoveHoles()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/downstreamOf/eflowpath/{id}", method = { RequestMethod.GET, RequestMethod.POST })
	public ApiResponse getDrainageAreaDownstreamOfFlowpath(@PathVariable("id") int id, HygraphParameters params,
			BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();

		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(
				hyGraph.getDownstreamDrainageArea(hyGraph.getEFlowpath(id).getCatchment(), params.getRemoveHoles()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

	@RequestMapping(value = "/upstreamOf", method = { RequestMethod.GET, RequestMethod.POST })
	public ApiResponse getDrainageAreaUpstreamOfLocation(HygraphParameters params, BindingResult bindingResult,HttpServletRequest request) throws IOException {
		
		if (bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();

		if (params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		
		//URL courrant *******Only use in developpement  ********************
		/*String currentURL = request.getRequestURL().toString() + "?" + request.getQueryString();
		ArrayList<Coverage> coverageList = cubeController.getLandCover(currentURL);*/
		
		
		//URL test en local *******Use when you run in local ********************
		/*String urlTestLocal = "http://chyf.ca/chyf/ecatchment/upstreamOf.json?point=-73.2667922973633,45.14354689687145";		
		ArrayList<Coverage> coverageList = cubeController.getLandCover(urlTestLocal);*/
		
		
		StopWatch sw = new StopWatch();
		sw.start();
		
		//******If you want to call the cube******
		/*ApiResponse resp = new ApiResponse(
			hyGraph.getUpstreamDrainageAreaWithCoverage(hyGraph.getECatchment(params.getPoint()), params.getRemoveHoles(),coverageList));*/
		
		//**********If you don't whant to call the cube*****
		ApiResponse resp = new ApiResponse(
				hyGraph.getUpstreamDrainageArea(hyGraph.getECatchment(params.getPoint()), params.getRemoveHoles()));
		
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
	
		
		return resp;
	}
	
	@RequestMapping(value = "/downstreamOf", method = { RequestMethod.GET, RequestMethod.POST })
	public ApiResponse getDrainageAreaDownstreamOfLocation(HygraphParameters params, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			throw new InvalidParameterException(bindingResult);
		}
		params.resolveAndValidate();

		if (params.getPoint() == null) {
			String errMsg = "The point parameter must be provided.";
			throw new IllegalArgumentException(errMsg);
		}

		StopWatch sw = new StopWatch();
		sw.start();
		ApiResponse resp = new ApiResponse(
				hyGraph.getDownstreamDrainageArea(hyGraph.getECatchment(params.getPoint()), params.getRemoveHoles()));
		sw.stop();
		resp.setExecutionTime(sw.getElapsedTime());
		resp.setParams(params);
		return resp;
	}

}
