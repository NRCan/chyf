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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.HyGraph;

/**
 * Used for computing paritioned catchments
 * @author Emily
 *
 */
public class UniqueSubCatchment {

	//these are the most upstream pourpoints that 
	//this are flows into (may be multiple in the case)
	//of secondary flows that flow into different pourpoints
	private Set<Pourpoint> points;
	
	//elementary catchments
	private Set<ECatchment> catchments = new HashSet<>();

	//downstream subcatchments
	private Set<UniqueSubCatchment> immediateDownstream = new HashSet<>();
	private Set<UniqueSubCatchment> upstream = new HashSet<>();
	
	private String id = null;
	
	public UniqueSubCatchment(Pourpoint point) {
		this.points = new HashSet<>();
		points.add(point);
	}
	
	public void addPoint(Pourpoint point) {
		this.points.add(point);
	}
	
	/**
	 * Sets the subcatchment id; should be unique
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * 
	 * @return the subcatchment id 
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * true of false if down is downstream of current catchments
	 * @param down
	 * @return
	 */
	public boolean isUpstream(UniqueSubCatchment down) {
		return upstream.contains(down);
	}
	
	public void addDownstreamCatchment(UniqueSubCatchment catchment) {
		if (this == catchment) return;
		immediateDownstream.add(catchment);
	}
	
	public void computeUpstreamCatchments() {
		//add to everything else downstream
		ArrayDeque<UniqueSubCatchment> toprocess = new ArrayDeque<>();
		toprocess.addAll(immediateDownstream);
		Set<UniqueSubCatchment> processed = new HashSet<>();
		while(!toprocess.isEmpty()) {
			UniqueSubCatchment dd = toprocess.removeFirst();
			processed.add(dd);
			dd.upstream.add(this);
			for (UniqueSubCatchment d : dd.immediateDownstream) {
				if (!processed.contains(d)) toprocess.add(d);
			}
		}
	}
	
	public Set<ECatchment> getCatchments(){
		return this.catchments;
		
	}
	
	public void addCatchment(ECatchment c){
		catchments.add(c);
	}

	/**
	 * Merges all values from the provided catchment with the
	 * current catchment.
	 * @param pc
	 */
	public void mergeCatchment(UniqueSubCatchment pc){
		catchments.addAll(pc.getCatchments());
	}

	public DrainageArea getDrainageArea(HyGraph graph) {
		return graph.buildDrainageArea(catchments, false);
	}
}
