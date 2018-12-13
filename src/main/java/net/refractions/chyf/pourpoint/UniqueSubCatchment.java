package net.refractions.chyf.pourpoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.vividsolutions.jts.geom.Geometry;

import net.refractions.chyf.hygraph.ECatchment;

public class UniqueSubCatchment {

	//immediate upstream poutpoint
	private Set<Pourpoint> upPoints = new HashSet<>();
	//elementary catchments
	private Set<ECatchment> catchments = new HashSet<>();
	//main pouroint
	private Pourpoint point;
	//downstream subcatchments
	private Set<UniqueSubCatchment> downstream = new HashSet<>();
	
	private String id = null;
	
	public UniqueSubCatchment(Pourpoint point) {
		this.point = point;
	}
	
	/**
	 * 
	 * @return the subcatchment id which is generated for the pourpoint id and the upstream pourpoint ids
	 */
	public String getId() {
		if (id == null) updateId();
		return id;
	}
	
	private void updateId() {
		StringBuilder sb = new StringBuilder();
		sb.append(point.getId());
		if (!upPoints.isEmpty()) {
			List<Pourpoint> sortedPoints = new ArrayList<>(upPoints);
			sortedPoints.sort((a,b)->(a.getId().compareTo(b.getId())));
			sb.append("_");
			StringJoiner joiner = new StringJoiner("_");
			sortedPoints.forEach(p->joiner.add(p.getId()));
			sb.append(joiner.toString());
		}
		id = sb.toString();
	}
	
	/**
	 * true of false if down is downstream of current catchments
	 * @param down
	 * @return
	 */
	public boolean isDownstream(UniqueSubCatchment down) {
		return downstream.contains(down);
	}
	
	public void addDownstreamCatchment(UniqueSubCatchment catchment) {
		downstream.add(catchment);
		
		ArrayDeque<UniqueSubCatchment> toprocess = new ArrayDeque<>();
		toprocess.add(catchment);
		while(!toprocess.isEmpty()) {
			UniqueSubCatchment dd = toprocess.removeFirst();
			downstream.add(dd);
			toprocess.addAll(dd.downstream);
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
		upPoints.addAll(pc.upPoints);
	}
	
	public void addUpstreamPourpoint(Pourpoint up) {
		upPoints.add(up);
		id = null;
	}
	
	public Geometry getGeometry() {
		return PourpointOutput.aggregateAreas(catchments);
	}
}