package net.refractions.chyf.pourpoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.HyGraph;

public class UniqueSubCatchment {

	//immediate upstream poutpoint
	private Set<Pourpoint> upPoints = new HashSet<>();
	//elementary catchments
	private Set<ECatchment> catchments = new HashSet<>();
	//main pouroint
	private Pourpoint point;
	//downstream subcatchments
	private Set<UniqueSubCatchment> immediateDownstream = new HashSet<>();
	private Set<UniqueSubCatchment> upstream = new HashSet<>();
	
	private String id = null;
//	private boolean removeHoles;
	
	public UniqueSubCatchment(Pourpoint point, boolean removeHoles) {
		this.point = point;
//		this.removeHoles = removeHoles;
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
		upPoints.addAll(pc.upPoints);
	}
	
	public void addUpstreamPourpoint(Pourpoint up) {
		upPoints.add(up);
		id = null;
	}
	
	public DrainageArea getDrainageArea() {
		return HyGraph.buildDrainageArea(catchments, false);
	}
}
