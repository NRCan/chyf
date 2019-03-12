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
package net.refractions.chyf.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointException;

public class PourpointParameters extends SharedParameters {

	public String points;
	public String output;
	
	private List<Pourpoint> inputPoints;
	private Set<PourpointEngine.OutputType> outputTypes;
	private boolean includeStats = false;

	public PourpointParameters() {
		
	}
	
	public String getPoints() {
		return this.points;
	}
	public void setPoints(String points) {
		this.points = points;
	}
	
	public String getOutput() {
		return this.output;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	
	public boolean getIncludeStats() {
		return includeStats;
	}

	public void setIncludeStats(boolean includeStats) {
		this.includeStats = includeStats;
	}

	public void resolveAndValidate() {
		inputPoints = new ArrayList<>();
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), getSrs());
		
		try {
			String[] parts = points.split(",");
			for (int i = 0; i < parts.length; i +=4) {
				String id = parts[i + 0];
				String x = parts[i + 1];
				String y = parts[i + 2];
				int code = Integer.valueOf(parts[i + 3]);
				
				Double dx = Double.parseDouble(x);
				Double dy = Double.parseDouble(y);
				Point pnt = gf.createPoint(new Coordinate(dx, dy));
				
				inputPoints.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(pnt, GeotoolsGeometryReprojector.srsCodeToCRS(getSrs()), ChyfDatastore.BASE_CRS),code,id));
			}
		}catch (PourpointException pe) {
			throw pe;
		}catch (Throwable t) {
			throw new IllegalArgumentException(
					"Parameter \"points\" must be in the format \"id,x,y,c,id,x,y,c...\". " + t.getMessage());
		}
		
		outputTypes = new HashSet<>();
		try {
			String[] parts = output.split(",");
			for (int i = 0; i < parts.length; i ++) {
				String key = parts[i];
				outputTypes.add(PourpointEngine.OutputType.parse(key));
			}
		}catch (Throwable t) {
			throw new IllegalArgumentException(
					"Parameter \"output\" must be in the format \"type,type,type\". " + t.getMessage());
		}
		
	}
	
	public List<Pourpoint> getPourpoints(){
		return this.inputPoints;
	}
	
	public Set<PourpointEngine.OutputType> getOutputTypes(){
		return this.outputTypes;
	}
}
