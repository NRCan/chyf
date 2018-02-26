package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Arrays;

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
import com.vividsolutions.jts.geom.Polygon;

public abstract class ConverterHelper {
	static final DecimalFormat DEGREE_FORMAT = new DecimalFormat("###.#####");
	static final DecimalFormat METRE_FORMAT = new DecimalFormat("###.##");
	
	protected Writer out;
	
	public ConverterHelper(Writer out) {
		this.out = out;
	}

	protected abstract void responseHeader(ApiResponse response) throws IOException;
	protected abstract void responseFooter(ApiResponse response) throws IOException;
	protected abstract void featureCollectionHeader(ApiResponse response) throws IOException;
	protected abstract void featureCollectionFooter() throws IOException;
	protected abstract void featureHeader(Geometry g, Integer id, ApiResponse response) throws IOException;
	protected abstract void featureFooter() throws IOException;
	protected abstract void objectHeader() throws IOException;
	protected abstract void objectFooter() throws IOException;
	protected abstract void listHeader() throws IOException;
	protected abstract void listFooter() throws IOException;
	protected abstract void nestedFieldHeader(String fieldName) throws IOException;
	protected abstract void nestedFieldFooter() throws IOException;
	
	protected abstract void field(String fieldName, boolean fieldValue) throws IOException;
	protected abstract void field(String fieldName, Long fieldValue) throws IOException;
	protected abstract void field(String fieldName, double fieldValue) throws IOException;
	protected abstract void field(String fieldName, String fieldValue) throws IOException;
	protected abstract void nullData() throws IOException;

	protected void field(String fieldName, Integer fieldValue) throws IOException {
		if(fieldValue == null) {
			field(fieldName, (Long)null);
		} else {
			field(fieldName, Long.valueOf(fieldValue));
		}
	}

	protected void nexus(Nexus nexus, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(nexus.getPoint(), response.getSrs()), nexus.getId(), responseMetadata);
		field("type", nexus.getType().toString());
		featureFooter();
	}
	protected void eFlowpath(EFlowpath eFlowpath, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		featureHeader(filterCoords(GeotoolsGeometryReprojector.reproject(eFlowpath.getLineString(), response.getSrs()), response.getScale()), eFlowpath.getId(), responseMetadata);
		field("name", eFlowpath.getName());
		field("nameid", eFlowpath.getNameId() == null ? null : eFlowpath.getNameId().toString());
		field("type", eFlowpath.getType().toString());
		field("rank", eFlowpath.getRank());
		field("certainty", eFlowpath.getCertainty());
		field("strahleror", eFlowpath.getStrahlerOrder());
		field("hortonor", eFlowpath.getHortonOrder());
		field("hackor", eFlowpath.getHackOrder());
		field("length", eFlowpath.getLength());
		featureFooter();
	}

	protected void eCatchment(ECatchment eCatchment, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(eCatchment.getPolygon(), response.getSrs()), eCatchment.getId(), responseMetadata);
		field("name", eCatchment.getName());
		field("type", eCatchment.getType().toString());
		field("subtype", eCatchment.getType().getSubType());
		field("rank", eCatchment.getRank());
		field("isTerminal", eCatchment.isTerminal());
		field("strahleror", eCatchment.getStrahlerOrder());
		field("hortonor", eCatchment.getHortonOrder());
		field("hackor", eCatchment.getHackOrder());
		field("area", eCatchment.getArea()/10000);		
		featureFooter();
	}

	protected void drainageArea(DrainageArea drainageArea, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(drainageArea.getGeometry(), response.getSrs()), 1, responseMetadata);
		field("area", drainageArea.getArea()/10000);		
		featureFooter();
	}

	protected void spatiallyIndexable(SpatiallyIndexable spatiallyIndexable, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		Envelope e = spatiallyIndexable.getEnvelope();
		Coordinate[] coords = {
				new Coordinate(e.getMinX(), e.getMinY()), 
				new Coordinate(e.getMinX(), e.getMaxY()), 
				new Coordinate(e.getMaxX(), e.getMaxY()), 
				new Coordinate(e.getMaxX(), e.getMinY()),
				new Coordinate(e.getMinX(), e.getMinY())
		};
		Polygon polygon = ChyfDatastore.GEOMETRY_FACTORY.createPolygon(coords);
		featureHeader(GeotoolsGeometryReprojector.reproject(polygon, response.getSrs()), null, responseMetadata);
		featureFooter();
	}

	protected void responseMetadata(ApiResponse response) throws IOException {
		if(response != null) {
			nestedFieldHeader("responseMetadata");
			objectHeader();
			field("executionTime", response.getExecutionTime());
			objectFooter();
			nestedFieldFooter();
		}
	}

	public void convertResponse(ApiResponse response) 
			throws IOException {
		responseHeader(response);
		Object data = response.getData();
		if(data == null) {
			featureCollectionHeader(response);
			featureCollectionFooter();			
		} else if(data instanceof Iterable<?>) {
			featureCollectionHeader(response);
			for(Object o : ((Iterable<?>)data)) {
				dataObject(o, response, null);
			}
			featureCollectionFooter();
		} else {
			dataObject(data, response, response);
		}
		responseFooter(response);
	}
	
	private void dataObject(Object data, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		if(data instanceof Nexus) {
			nexus((Nexus)data, response, responseMetadata);
		} else if(data instanceof EFlowpath) {
			eFlowpath((EFlowpath)data, response, responseMetadata);
		} else if(data instanceof ECatchment) {
			eCatchment((ECatchment)data, response, responseMetadata);
		} else if(data instanceof DrainageArea) {
			drainageArea((DrainageArea)data, response, responseMetadata);
		} else if(data instanceof SpatiallyIndexable) {
			spatiallyIndexable((SpatiallyIndexable)data, response, responseMetadata);
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
		
		// loop over coords, copying only coords at least <scale> away from the last copied coordinate
		// always copy start and end
		int c = 0;
		int nextCoord = 0;
		coords[nextCoord++] = cs.getCoordinate(c++);
		while(c < cs.size() - 1) {
			// TODO alter the scale filter value by the (sin|cosin?) of angle of the line between the points
			if(coords[nextCoord-1].distance(cs.getCoordinate(c)) > scale) {
				coords[nextCoord++] = cs.getCoordinate(c);
			}
			c++;
		}
		coords[nextCoord++] = cs.getCoordinate(c++); 
		coords = Arrays.copyOf(coords, nextCoord);
		return l.getFactory().createLineString(coords);
	}
}
