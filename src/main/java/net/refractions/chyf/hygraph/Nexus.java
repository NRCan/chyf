package net.refractions.chyf.hygraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.refractions.chyf.enumTypes.NexusType;
import net.refractions.chyf.indexing.SpatiallyIndexable;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

public class Nexus implements SpatiallyIndexable{
	private ArrayList<EFlowpath> upFlows = new ArrayList<EFlowpath>();
	private ArrayList<EFlowpath> downFlows = new ArrayList<EFlowpath>();
	private Point point;
	private NexusType type = NexusType.UNKNOWN;

	public Nexus(Point point) {
		this.point = point;
	}

	public void addUpFlow(EFlowpath edge) {
		upFlows.add(edge);
	}

	public void addDownFlow(EFlowpath edge) {
		downFlows.add(edge);
	}

	public List<EFlowpath> getUpFlows(){
		return Collections.unmodifiableList(upFlows);
	}

	public List<EFlowpath> getDownFlows() {
		return Collections.unmodifiableList(downFlows);
	}

	public Point getPoint() {
		return point;
	}
	
	public NexusType getType() {
		return type;
	}

	@Override
	public Envelope getEnvelope() {
		return point.getEnvelopeInternal();
	}

	@Override
	public double distance(Point p) {
		return point.distance(p);
	}

}
