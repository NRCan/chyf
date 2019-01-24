package net.refractions.chyf.pourpoint;

import java.util.HashSet;
import java.util.Set;

import net.refractions.chyf.hygraph.Nexus;


/**
 * Node class used when calculating the PRT pourpoint tree
 * @author Emily
 *
 */
public class PrtTreeNode {
	
	private Set<PrtTreeNode> innodes = new HashSet<>();
	private PrtTreeNode outnode = null;
	
	private Set<Pourpoint> pourpoints = new HashSet<>();
	private Set<Pourpoint> allpourpoints = new HashSet<>();
	
	private String id;
	private int order = -1;
	
	private Nexus nexus;
	
	public PrtTreeNode(Nexus nexus) {
		this.nexus = nexus;
	}
	
	public Nexus getNexus() {
		return this.nexus;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}
	
	public Set<PrtTreeNode> getInNodes(){
		return this.innodes;
	}
	
	public Set<Pourpoint> getPourpoints(boolean isroot){
		if (isroot) return this.pourpoints;
		return this.allpourpoints;
	}
	public boolean containsPourpoint(Pourpoint p, boolean isroot) {
		if (isroot) return this.pourpoints.contains(p);
		return this.allpourpoints.contains(p);
	}
	public void addPourpoint(Pourpoint p, boolean isroot) {
		allpourpoints.add(p);
		if (isroot) this.pourpoints.add(p);
	}
	
	public void addInNode(PrtTreeNode n) {
		innodes.add(n);
	}
	public void removeInNode(PrtTreeNode n) {
		innodes.remove(n);
	}
	
	public int getOrder() {
		return this.order;
	}
	
	public void updateOrder(int order) {
		if (this.order < order) this.order = order;
	}
	
	public PrtTreeNode getOutNode() {
		return this.outnode;
	}
	
	public void setOutNode(PrtTreeNode out, boolean candiff) {
		if (candiff == false && this.outnode != null && this.outnode != out) throw new IllegalStateException("PRT Tree cannot have multiple outputs for a node");
		this.outnode = out;
	}
}
