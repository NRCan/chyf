package net.refractions.chyf.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointException;

public class PourpointParameters extends SharedParameters {

	public String points;
	public String output;
	
	private List<Pourpoint> inputPoints;
	private Set<PourpointEngine.OutputType> outputTypes;
	
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
				
				inputPoints.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(pnt, ChyfDatastore.BASE_SRS),code,id));
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
