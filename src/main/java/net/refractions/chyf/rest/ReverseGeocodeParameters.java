package net.refractions.chyf.rest;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.util.GeomUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;

public class ReverseGeocodeParameters extends SharedParameters {
	
	private double[] point;
	private Point pointPoint;
	private double[] bbox;
	private Envelope bboxEnvelope;
	private Integer maxDistance;
	
	public Point getPoint() {
		return pointPoint;
	}
	
	public void setPoint(double[] point) {
		this.point = point;
	}
	
	public void setPointGeom(Point point) {
		this.pointPoint = point;
	}
	
	public Envelope getBbox() {
		return bboxEnvelope;
	}
	
	public void setBbox(double[] bbox) {
		this.bbox = bbox;
	}
	
	public Integer getMaxDistance() {
		return maxDistance;
	}
	
	public void setMaxDistance(Integer maxDistance) {
		this.maxDistance = maxDistance;
	}
	
	private static Geometry precisionReducer(Geometry geometry) {
		PrecisionModel pm = new PrecisionModel(1000);
		return GeometryPrecisionReducer.reduce(geometry, pm);
	}
	
	public void resolveAndValidate() {
		super.resolveAndValidate();
		// convert any double[] into geometries in input SRS projection
		// then reproject any geometries to the internal projection
		// note the incoming geomtries may either come in as double[]
		// or as geometry objects in the inputSRS projection
		// so we may not need to create them from double[] but still need to reproject
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), getSrs());
		if(point != null && point.length != 0) {
			if(point.length == 2) {
				pointPoint = gf.createPoint(new Coordinate(point[0], point[1]));
			} else {
				throw new IllegalArgumentException(
						"Parameter \"point\" must be in the format \"x,y\".");
			}
		}
		if(pointPoint != null) {
			pointPoint = (Point) precisionReducer(GeotoolsGeometryReprojector.reproject(pointPoint, ChyfDatastore.BASE_SRS));
		}
		Polygon bboxPolygon = null;
		if(bbox != null && bbox.length != 0) {
			if(bbox.length == 4) {
				bboxPolygon = GeomUtil.buildBbox(bbox, gf);
			} else {
				throw new IllegalArgumentException(
						"Parameter \"bbox\" must be in the format \"minx,miny,maxx,maxy\".");
			}
		}
		if(bboxPolygon != null) {
			bboxPolygon = (Polygon) precisionReducer(GeotoolsGeometryReprojector.reproject(bboxPolygon, ChyfDatastore.BASE_SRS));
			bboxEnvelope = bboxPolygon.getEnvelopeInternal();
			// if their is a bbox, override the maxdistance with the radius of the bbox
			maxDistance = (int)Math.round(GeomUtil.getRadius(bboxEnvelope));
		}		
	}

}
