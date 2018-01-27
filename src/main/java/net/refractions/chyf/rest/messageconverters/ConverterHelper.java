package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.hygraph.Nexus;
import net.refractions.chyf.indexing.SpatiallyIndexable;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public abstract class ConverterHelper {
	static final DecimalFormat DEGREE_FORMAT = new DecimalFormat("###.#####");
	static final DecimalFormat METRE_FORMAT = new DecimalFormat("###.##");
	
	protected Writer out;
	
	public ConverterHelper(Writer out) {
		this.out = out;
	}

	protected abstract void responseHeader(ApiResponse response) throws IOException;
	protected abstract void responseFooter(ApiResponse response) throws IOException;
	protected abstract void featureCollectionHeader() throws IOException;
	protected abstract void featureCollectionFooter() throws IOException;
	protected abstract void featureHeader(Geometry g, Integer id) throws IOException;
	protected abstract void featureFooter() throws IOException;
	protected abstract void objectHeader() throws IOException;
	protected abstract void objectFooter() throws IOException;
	protected abstract void listHeader() throws IOException;
	protected abstract void listFooter() throws IOException;
	protected abstract void nestedFieldHeader(String fieldName) throws IOException;
	protected abstract void nestedFieldFooter() throws IOException;
	
	protected abstract void field(String fieldName, long fieldValue) throws IOException;
	protected abstract void field(String fieldName, double fieldValue) throws IOException;
	protected abstract void field(String fieldName, String fieldValue) throws IOException;
	protected abstract void nullData() throws IOException;
	
	protected void nexus(Nexus nexus, ApiResponse response) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(nexus.getPoint(), response.getSrs()), null);
		field("type", nexus.getType().toString());
		featureFooter();
	}
	protected void eFlowpath(EFlowpath eFlowpath, ApiResponse response) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(eFlowpath.getLineString(), response.getSrs()), eFlowpath.getId());
		field("name", eFlowpath.getName());
		field("type", eFlowpath.getType().toString());
		field("rank", eFlowpath.getRank());
		field("strahleror", eFlowpath.getStrahlerOrder());
		field("hortonor", eFlowpath.getHortonOrder());
		field("hackor", eFlowpath.getHackOrder());
		field("length", eFlowpath.getLength());
		featureFooter();
	}

	protected void eCatchment(ECatchment eCatchment, ApiResponse response) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(eCatchment.getPolygon(), response.getSrs()), eCatchment.getId());
		field("type", eCatchment.getType().toString());
		field("subtype", eCatchment.getType().getSubType());
		field("rank", eCatchment.getRank());
		field("area", eCatchment.getArea()/10000);		
		featureFooter();
	}

	protected void drainageArea(DrainageArea drainageArea, ApiResponse response) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(drainageArea.getGeometry(), response.getSrs()), 1);
		field("area", drainageArea.getArea()/10000);		
		featureFooter();
	}

	protected void spatiallyIndexable(SpatiallyIndexable spatiallyIndexable, ApiResponse response) throws IOException {
		Envelope e = spatiallyIndexable.getEnvelope();
		Coordinate[] coords = {
				new Coordinate(e.getMinX(), e.getMinY()), 
				new Coordinate(e.getMinX(), e.getMaxY()), 
				new Coordinate(e.getMaxX(), e.getMaxY()), 
				new Coordinate(e.getMaxX(), e.getMinY()),
				new Coordinate(e.getMinX(), e.getMinY())
		};
		Polygon polygon = ChyfDatastore.GEOMETRY_FACTORY.createPolygon(coords);
		featureHeader(GeotoolsGeometryReprojector.reproject(polygon, response.getSrs()), null);
		featureFooter();
	}

	public void convertResponse(ApiResponse response) 
			throws IOException {
		responseHeader(response);
		field("executionTime", response.getExecutionTime());
		Object data = response.getData();
		if(data instanceof Iterable<?>) {
			nestedFieldHeader("data");
			featureCollectionHeader();
			for(Object o : ((Iterable<?>)data)) {
				dataObject(o, response);
			}
			featureCollectionFooter();
			nestedFieldFooter();
		} else {
			nestedFieldHeader("data");
			dataObject(data,response);
			nestedFieldFooter();
		}
		responseFooter(response);
	}
	
	private void dataObject(Object data, ApiResponse response) throws IOException {
		if(data instanceof Nexus) {
			nexus((Nexus)data, response);
		} else if(data instanceof EFlowpath) {
			eFlowpath((EFlowpath)data, response);
		} else if(data instanceof ECatchment) {
			eCatchment((ECatchment)data, response);
		} else if(data instanceof DrainageArea) {
			drainageArea((DrainageArea)data, response);
		} else if(data instanceof SpatiallyIndexable) {
			spatiallyIndexable((SpatiallyIndexable)data, response);
		} else {
			nullData();
		}
	}

	protected static String formatOrdinate(double ord) {
		if(ord <= 180 && ord >= -180) {
			return DEGREE_FORMAT.format(ord);
		}
		return METRE_FORMAT.format(ord);
	}

	protected LineString filterCoords(LineString l, Double scale) {
		if(scale == null) {
			return l;
		}
		CoordinateSequence cs = l.getCoordinateSequence();
		Coordinate[] coords = new Coordinate[cs.size()];
		
		//TODO loop over coords, copying only coords at least <scale> away from the last copied coordinate
		// always copy start and end
		// alter the scale filter value by the (sin|cosin?) of angle of the line between the points
		
		return l.getFactory().createLineString(coords);
	}
}
