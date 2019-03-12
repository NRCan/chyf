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
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

public abstract class ConverterHelper {
	
	public static enum DrainageAreaField{
		AREA("area", Double.class);
		
		String fieldName;
		Class<?> type;
		
		DrainageAreaField(String fieldName, Class<?> type){
			this.fieldName = fieldName;
			this.type = type;
		}
		
		public Object getValue(DrainageArea item) {
			if (this == AREA) return item.getArea() / 10000;
			return "";
		}
	};

	
	public static enum NexusField{
		TYPE("type", String.class);
		
		String fieldName;
		Class<?> type;
		
		NexusField(String fieldName, Class<?> type){
			this.fieldName = fieldName;
			this.type = type;
		}
		
		public Object getValue(Nexus item) {
			if (this == TYPE) return item.getType().toString();
			return "";
		}
	};

	public static enum ECatchmentField{
		NAME("name", String.class),
		TYPE("type", String.class),
		SUBTYPE("subtype", String.class),
		RANK("rank", String.class),
		STRAHLER("strahleror", Integer.class),
		HORTON("hortonor", Integer.class),
		HACK("hackor", Integer.class),
		AREA("area", Double.class);
		
	
		String fieldName;
		Class<?> type;
		
		ECatchmentField(String fieldName, Class<?> type){
			this.fieldName = fieldName;
			this.type = type;
		}
		
		public Object getValue(ECatchment item) {
			switch(this) {
			case AREA: return item.getArea() / 10000;
			case HACK: return item.getHackOrder();
			case HORTON: return item.getHortonOrder();
			case NAME: return item.getName();
			case RANK: return item.getRank().rankName;
			case STRAHLER: return item.getStrahlerOrder();
			case SUBTYPE: return item.getType().getSubType();
			case TYPE: return item.getType().toString();
			}
			return "";
		}
	}

	public static enum EFlowpathField{
		NAME("name", String.class),
		NAMEID("nameid", String.class),
		TYPE("type", String.class),
		RANK("rank", String.class),
		STRAHLER("strahleror", Integer.class),
		HORTON("hortonor", Integer.class),
		HACK("hackor", Integer.class),
		LENGTH("length", Double.class);
		
		String fieldName;
		Class<?> type;
		
		EFlowpathField(String fieldName, Class<?> type){
			this.fieldName = fieldName;
			this.type = type;
		}
		
		public Object getValue(EFlowpath item) {
			switch(this) {
			case LENGTH: return item.getLength();
			case HACK: return item.getHackOrder();
			case HORTON: return item.getHortonOrder();
			case NAME: return item.getName();
			case NAMEID: return item.getNameId() == null ? null : item.getName();
			case RANK: return item.getRank().rankName;
			case STRAHLER: return item.getStrahlerOrder();
			case TYPE: return item.getType().toString();
			}
			return "";
		}
	}
	
	
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
	
	protected void nfield(String fieldName, Object type) throws IOException {
		if(type instanceof String) field(fieldName, (String)type);
		if(type instanceof Long) field(fieldName, (Long)type);
		if(type instanceof Integer) field(fieldName, (Integer)type);
		if(type instanceof Double) field(fieldName, (Double)type);
	}

	protected void nexus(Nexus nexus, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(nexus.getPoint(), ChyfDatastore.BASE_CRS, GeotoolsGeometryReprojector.srsCodeToCRS(response.getSrs())), nexus.getId(), responseMetadata);
		for (NexusField f : NexusField.values()) {
			nfield(f.fieldName, f.getValue(nexus));
		}
		featureFooter();
	}
	protected void eFlowpath(EFlowpath eFlowpath, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		featureHeader(filterCoords(GeotoolsGeometryReprojector.reproject(eFlowpath.getLineString(), ChyfDatastore.BASE_CRS, GeotoolsGeometryReprojector.srsCodeToCRS(response.getSrs())), response.getScale()), eFlowpath.getId(), responseMetadata);
		for (EFlowpathField f : EFlowpathField.values()) {
			nfield(f.fieldName, f.getValue(eFlowpath));
		}
		featureFooter();
	}

	protected void eCatchment(ECatchment eCatchment, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(eCatchment.getPolygon(), ChyfDatastore.BASE_CRS, GeotoolsGeometryReprojector.srsCodeToCRS(response.getSrs())), eCatchment.getId(), responseMetadata);
		for (ECatchmentField f : ECatchmentField.values()) {
			nfield(f.fieldName, f.getValue(eCatchment));
		}
		//add stats
		for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
			double v =  s.getValue(eCatchment);
			if (!Double.isNaN(v)) nfield(s.getFieldName().toLowerCase(), v);
		}
		featureFooter();
	}

	protected void drainageArea(DrainageArea drainageArea, ApiResponse response, ApiResponse responseMetadata) throws IOException {
		featureHeader(GeotoolsGeometryReprojector.reproject(drainageArea.getGeometry(), ChyfDatastore.BASE_CRS, GeotoolsGeometryReprojector.srsCodeToCRS(response.getSrs())), 1, responseMetadata);
		for (DrainageAreaField f : DrainageAreaField.values()) {
			nfield(f.fieldName, f.getValue(drainageArea));
		}
		
		if (drainageArea.hasStats()) {
			for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
				Double v = drainageArea.getStat(s);
				if (v != null) nfield(s.getFieldName().toLowerCase(), v);
			}
		}
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
		featureHeader(GeotoolsGeometryReprojector.reproject(polygon, ChyfDatastore.BASE_CRS, GeotoolsGeometryReprojector.srsCodeToCRS(response.getSrs())), null, responseMetadata);
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
		}else if (data instanceof PourpointOutput) {
			(new PourpointJsonConverter(out)).convertResponse(response);
		} else {
			dataObject(data, response, response);
		}
		responseFooter(response);
	}
	
	protected void dataObject(Object data, ApiResponse response, ApiResponse responseMetadata) throws IOException {
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
