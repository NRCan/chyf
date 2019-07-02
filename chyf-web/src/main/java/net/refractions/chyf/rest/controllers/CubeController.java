package net.refractions.chyf.rest.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import net.refractions.chyf.hygraph.Coverage;
import net.refractions.util.JsonConverter;

/*
 * Faire les requêtes GET et POST vers le Cube
 */
public class CubeController {
	
	private String urlCUbeLandCover;
	
	public CubeController() {
		//v/rifier si c'est collection= (eCatchman)  ou feauture = (si on utilise drainageArea)
		this.urlCUbeLandCover = "http://dy0a51yzxbhxw.cloudfront.net/datacube/temporal/landsat/landcover/12?collection=";
	}
	
	
	//Avoir les informations pour le landCover
	public ArrayList<Coverage> getLandCover(String urlDrainageAreaUpstream) throws IOException
	{			
		//Encoder l'URL
		String urlCurrentEncoding = URLEncoder.encode(urlDrainageAreaUpstream, StandardCharsets.UTF_8.toString());
		
		String urlCubeComplet = urlCUbeLandCover + urlCurrentEncoding;
				
		JsonConverter jConverter = new JsonConverter();
		
		ArrayList<Coverage> coverageList = jConverter.convertJsonToCover(getCubeLandCover(urlCubeComplet));
		
		return coverageList;		
	}
	
	//Fait la requête get au cube pour avoir le landCover
	private String getCubeLandCover(String urlComplet) throws IOException
	{
		StringBuilder result = new StringBuilder();
	      URL url = new URL(urlComplet);
	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	      conn.setRequestMethod("GET");
	      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	      String line;
	      while ((line = rd.readLine()) != null) {
	         result.append(line);
	      }
	      rd.close();
	      
	      System.out.println(result.toString());
	      
	      return result.toString();
		
	}
}
