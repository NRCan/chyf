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
package net.refractions.chyf.pourpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.pourpoint.PourpointEngine.OutputType;

/**
 * Class containing all the requested outputs
 * from the pourpoint engine.
 * 
 * @author Emily
 *
 */
public class PourpointOutput {

	private List<Pourpoint> points;
	
	private Set<PourpointEngine.OutputType> outputs;
	
	//catchments
	private HashMap<Pourpoint, DrainageArea> catchments;
	private Integer[][] ccr;
	//sub catchment rel.
	private Integer[][] scr;
	private HashMap<Pourpoint, DrainageArea> subcatchments;
	
	//partitioned catchments
	private List<DrainageArea> partitionedcatchments;
	private Integer[][] pcr;
	
	private Double[][] minPpDistance;
	private Double[][] maxPpDistance;
	private Double[][] primaryPpDistance;
	
	private Set<DrainageArea> interiorCatchments;
	private String prt = "";
	
	public PourpointOutput(PourpointEngine engine) {
		this.points = engine.getPoints();
		this.scr = engine.getSubCatchmentRelationship();
		this.pcr = engine.getPartitionedCatchmentRelationship();
		
		if (engine.getSortedPartitionedCatchments() != null) {
			partitionedcatchments = new ArrayList<>();
			for (UniqueSubCatchment c : engine.getSortedPartitionedCatchments()) {
				DrainageArea da = c.getDrainageArea(engine.getGraph());
				da.setId(c.getId());
				partitionedcatchments.add(da);
			}
		}
		this.ccr = engine.getCatchmentContainment();
		this.interiorCatchments = engine.getInteriorCatchments();
		
		minPpDistance = engine.getProjectedPourpointMinDistanceMatrix();
		maxPpDistance = engine.getProjectedPourpointMaxDistanceMatrix();
		this.primaryPpDistance = engine.getProjectedPourpointPrimaryDistanceMatrix();
		this.outputs = engine.getAvailableOutputs();
		this.prt = engine.getPointRelationshipTree();
		
		if (engine.getAvailableOutputs().contains(OutputType.CATCHMENTS)) {
			catchments = new HashMap<>();
			for (Pourpoint p : points) {
				catchments.put(p, p.getCatchmentDrainageArea(engine.getGraph(), engine.getRemoveHoles()));
			}
		}
		if (engine.getAvailableOutputs().contains(OutputType.SUBCATCHMENTS)) {
			subcatchments = new HashMap<>();
			for (Pourpoint p : points) {
				subcatchments.put(p, engine.getGraph().buildDrainageArea(p.getUniqueCatchments(), false));
			}
		}
		
	}

	public String getPointRelationshipTree() {
		return this.prt;
	}
	
	public Set<DrainageArea> getInteriorCatchments(){
		return this.interiorCatchments;
	}
	
	public Set<PourpointEngine.OutputType> getAvailableOutputs(){
		return this.outputs;
	}
	
	public List<Pourpoint> getPoints() {
		return points;
	}

	public Double[][] getProjectedPourpointMinDistanceMatrix(){
		return minPpDistance;
	}
	
	public Double[][] getProjectedPourpointMaxDistanceMatrix(){
		return maxPpDistance;
	}
	
	public Double[][] getProjectedPourpointPrimaryDistanceMatrix(){
		return primaryPpDistance;
	}
	
	public Integer[][] getCatchmentContainment(){
		return this.ccr;
	}
	
	public Integer[][] getSubCatchmentRelationship() {
		return scr;
	}

	public Integer[][] getPartitionedCatchmentRelationship() {
		return pcr;
	}

	public List<DrainageArea> getPartitionedCatchments() {
		return partitionedcatchments;
	}
	
	public DrainageArea getCatchment(Pourpoint point) {
		return catchments.get(point);
	}
	
	public DrainageArea getSubcatchment(Pourpoint point) {
		return subcatchments.get(point);
	}


}
