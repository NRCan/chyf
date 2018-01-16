package net.refractions.chyf.hygraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.refractions.chyf.indexing.SpatiallyIndexable;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ECatchment implements SpatiallyIndexable {
	private final int id;
	private final double area;
	private final Polygon polygon;
	private List<EFlowpath> flowpaths;
	//private CatchmentType type;
	
	public ECatchment(int id, Polygon polygon) {
		this.id = id;
		this.polygon = polygon;
		this.area = polygon.getArea();
		this.flowpaths = new ArrayList<EFlowpath>(1);
	}
	
	public int getId() {
		return id;
	}

	public double getArea() {
		return area;
	}

	public Polygon getPolygon() {
		return polygon;
	}

	@Override
	public Envelope getEnvelope() {
		return polygon.getEnvelopeInternal();
	}
	
	@Override
	public double distance(Point p) {
		return polygon.distance(p);
	}

	public void addFlowpath(EFlowpath flowpath) {
		flowpaths.add(flowpath);
	}

	public List<EFlowpath> getFlowpaths() {
		return Collections.unmodifiableList(flowpaths);
	}	
	
}
