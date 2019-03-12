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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.hygraph.Nexus;
import net.refractions.chyf.indexing.SpatiallyIndexable;
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

/**
 * Converts API Response to a geopackage output.  Collections
 * are combined into individual layers by type.
 * 
 * @author Emily
 *
 */
public class GeoPackageHelper {

	private GeoPackage geopkg;
	
	private OutputStream outStream; 
	
	public GeoPackageHelper(OutputStream out) {
		this.outStream = out;
	}

	
	public void convertResponse(ApiResponse response)  throws IOException {
		Path tempFile = Files.createTempFile("geopackage", ".gpkg");
		geopkg = new GeoPackage(tempFile.toFile());
		
		try {
		Object data = response.getData();
		if (data instanceof PourpointOutput) {
			(new PourpointGeoPackageConverter(geopkg)).convertResponse(response);
		}else {
			convertResponseInternal(response);
		}
		}finally {
			geopkg.close();
		}
		//write and delete temporary file
		Files.copy(tempFile, outStream);
		Files.delete(tempFile);
	}
	
	private void convertResponseInternal(ApiResponse response) throws IOException{
		Object data = response.getData();
			
		if(data == null) {
			data = Collections.emptySet();
		}else if (!(data instanceof Iterable<?>)) {
			data = Collections.singleton(data);
		}
			
		//features must be collected together by type 
		//Layers include: Nexus, EFlowpath, ECatchment, DraingeArea, Other??
		HashMap<Class<?>, List<Object>> collections = new HashMap<>();
				
		for (Object o : ((Iterable<?>)data)) {
			Class<?> root = null;
			if (o instanceof ECatchment) root = ECatchment.class;
			else if (o instanceof Nexus) root = Nexus.class;
			else if (o instanceof EFlowpath) root = EFlowpath.class;
			else if (o instanceof DrainageArea)  root = DrainageArea.class;
			else if (o instanceof SpatiallyIndexable) root = SpatiallyIndexable.class;
			else continue;
							
			List<Object> objs = collections.get(root);
			if (objs == null) {
				objs = new ArrayList<>();
				collections.put(root, objs);
			}
			objs.add(o);
		}
				
		for (Entry<Class<?>, List<Object>> collection : collections.entrySet()) {
			Class<?> type = collection.getKey();
			if (type == Nexus.class) {
				writeNexus(collection.getValue(), response);
			}else if (type == EFlowpath.class) {
				writeEFlowpath(collection.getValue(), response);
			}else if (type == ECatchment.class) {
				writeECatchment(collection.getValue(), response);
			}else if (type == DrainageArea.class) {
				writeDrainageArea(collection.getValue(), response);
			}else if (type == SpatiallyIndexable.class) {
				writeSpatialIndex(collection.getValue(), response);
			}
		}
	}
	
	private void writeEFlowpath(List<Object> items, ApiResponse response) throws IOException {
		SimpleFeatureType featureType = getFeatureType(EFlowpath.class, response);
		ListFeatureCollection features = new ListFeatureCollection(featureType);
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		
		for (Object x : items) {
			EFlowpath flowpath = (EFlowpath)x;
			
			for(ConverterHelper.EFlowpathField field : ConverterHelper.EFlowpathField.values()) {
				featureBuilder.set(field.fieldName, field.getValue(flowpath));
			}
			
			featureBuilder.set("geometry", reproject(flowpath.getLineString(), response.getSrs()));
			features.add(featureBuilder.buildFeature(String.valueOf(flowpath.getId())));
		}
			
		FeatureEntry entry = new FeatureEntry();
		entry.setDataType(org.geotools.geopkg.Entry.DataType.Feature);
		entry.setGeometryColumn("geometry");
		entry.setGeometryType(Geometries.LINESTRING);
		entry.setSrid(response.getSrs());
		entry.setDescription("Collection of Flowpaths");
		entry.setTableName("flowpath");
		geopkg.add(entry, features);
		
	}
	
	private void writeDrainageArea(List<Object> items, ApiResponse response) throws IOException {
		SimpleFeatureType featureType = getFeatureType(DrainageArea.class, response);
		ListFeatureCollection features = new ListFeatureCollection(featureType);
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		
		int fid = 1;
		for (Object x : items) {
			DrainageArea area = (DrainageArea)x;
			
			for(ConverterHelper.DrainageAreaField field : ConverterHelper.DrainageAreaField.values()) {
				featureBuilder.set(field.fieldName, field.getValue(area));
			}
			
			//stats
			if (area.hasStats()) {
				for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
					Double v = area.getStat(s);
					if (v != null) featureBuilder.set(s.getFieldName().toLowerCase(), v);
				}
			}
			
			featureBuilder.set("geometry", reproject(area.getGeometry(), response.getSrs()));
			features.add(featureBuilder.buildFeature(String.valueOf(fid++)));
		}
			
		FeatureEntry entry = new FeatureEntry();
		entry.setDataType(org.geotools.geopkg.Entry.DataType.Feature);
		entry.setGeometryColumn("geometry");
		entry.setGeometryType(Geometries.GEOMETRY);
		entry.setSrid(response.getSrs());
		entry.setDescription("Drainage area");
		entry.setTableName("drainagearea");
		
		geopkg.add(entry, features);
	}
	
	private void writeECatchment(List<Object> items, ApiResponse response) throws IOException {
		SimpleFeatureType featureType = getFeatureType(ECatchment.class, response);
		ListFeatureCollection features = new ListFeatureCollection(featureType);
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		
		for (Object x : items) {
			ECatchment catchment = (ECatchment)x;
			for(ConverterHelper.ECatchmentField field : ConverterHelper.ECatchmentField.values()) {
				featureBuilder.set(field.fieldName, field.getValue(catchment));
			}
			//add stats
			for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
				double v =  s.getValue(catchment);
				if (!Double.isNaN(v)) featureBuilder.set(s.getFieldName().toLowerCase(), v);
			}
			featureBuilder.set("geometry", reproject(catchment.getPolygon(), response.getSrs()));
			features.add(featureBuilder.buildFeature(String.valueOf(catchment.getId())));
		}
			
		FeatureEntry entry = new FeatureEntry();
		entry.setDataType(org.geotools.geopkg.Entry.DataType.Feature);
		entry.setGeometryColumn("geometry");
		entry.setGeometryType(Geometries.POLYGON);
		entry.setSrid(response.getSrs());
		entry.setDescription("Collection of Catchments");
		entry.setTableName("catchment");
		
		geopkg.add(entry, features);
	}
	
	private void writeNexus(List<Object> items, ApiResponse response) throws IOException {
		SimpleFeatureType featureType = getFeatureType(Nexus.class, response);
		ListFeatureCollection features = new ListFeatureCollection(featureType);
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		
		for (Object x : items) {
			Nexus nexus = (Nexus)x;
			for(ConverterHelper.NexusField field : ConverterHelper.NexusField.values()) {
				featureBuilder.set(field.fieldName, field.getValue(nexus));
			}	
			featureBuilder.set("geometry",reproject(nexus.getPoint(), response.getSrs()));
			features.add(featureBuilder.buildFeature(String.valueOf(nexus.getId())));
		}
		
		FeatureEntry entry = new FeatureEntry();
		entry.setDataType(org.geotools.geopkg.Entry.DataType.Feature);
		entry.setGeometryColumn("geometry");
		entry.setGeometryType(Geometries.POINT);
		entry.setSrid(response.getSrs());
		entry.setDescription("Collection of Nexus Points");
		entry.setTableName("nexus");
		geopkg.add(entry, features);
		
	}
	
	private void writeSpatialIndex(List<Object> items, ApiResponse response) throws IOException {
		SimpleFeatureType featureType = getFeatureType(SpatiallyIndexable.class, response);
		ListFeatureCollection features = new ListFeatureCollection(featureType);
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		
		int fid = 1;
		for (Object x : items) {
			SpatiallyIndexable spatiallyIndexable = (SpatiallyIndexable)x;
			Envelope e = spatiallyIndexable.getEnvelope();
			Coordinate[] coords = {
					new Coordinate(e.getMinX(), e.getMinY()), 
					new Coordinate(e.getMinX(), e.getMaxY()), 
					new Coordinate(e.getMaxX(), e.getMaxY()), 
					new Coordinate(e.getMaxX(), e.getMinY()),
					new Coordinate(e.getMinX(), e.getMinY())
			};
			Polygon polygon = ChyfDatastore.GEOMETRY_FACTORY.createPolygon(coords);
			
			featureBuilder.set("geometry", reproject(polygon, response.getSrs()));
			features.add(featureBuilder.buildFeature(String.valueOf(fid++)));
		}
			
		FeatureEntry entry = new FeatureEntry();
		entry.setDataType(org.geotools.geopkg.Entry.DataType.Feature);
		entry.setGeometryColumn("geometry");
		entry.setGeometryType(Geometries.POLYGON);
		entry.setSrid(response.getSrs());
		entry.setDescription("Collection of spatially indexable features");
		entry.setTableName("spatiallyindexable");
		
		geopkg.add(entry, features);
	}
	
	private SimpleFeatureType getFeatureType(Class<?> type, ApiResponse response) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setSRS("EPSG:" + String.valueOf(response.getSrs()));
		
		if (type == Nexus.class) {
			builder.setName("Nexus");
			for(ConverterHelper.NexusField field : ConverterHelper.NexusField.values()) {
				builder.add(field.fieldName, field.type);
			}
			builder.add("geometry", Point.class);
		}else if (type == EFlowpath.class) {
			builder.setName("Flowpath");
			for(ConverterHelper.EFlowpathField field : ConverterHelper.EFlowpathField.values()) {
				builder.add(field.fieldName, field.type);
			}
			builder.add("geometry", LineString.class);
		}else if (type == ECatchment.class) {
			builder.setName("Catchment");
			for(ConverterHelper.ECatchmentField field : ConverterHelper.ECatchmentField.values()) {
				builder.add(field.fieldName, field.type);
			}
			//add stats
			for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
				builder.add(s.getFieldName().toLowerCase(), Double.class);
			}
			builder.add("geometry", Polygon.class);
		}else if (type == DrainageArea.class) {
			builder.setName("Drainge Area");
			for(ConverterHelper.DrainageAreaField field : ConverterHelper.DrainageAreaField.values()) {
				builder.add(field.fieldName, field.type);
			}
			//add stats
			for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
				builder.add(s.getFieldName().toLowerCase(), Double.class);
			}
			builder.add("geometry", Geometry.class);
		}else if (type == SpatiallyIndexable.class) {
			builder.setName("Spatially Indexable");
			builder.add("geometry", Polygon.class);
		}
		return builder.buildFeatureType();
	}
	
	private <T extends Geometry> T reproject(T geom, int tosrs) {
		return (T)GeotoolsGeometryReprojector.reproject(geom, ChyfDatastore.BASE_CRS, GeotoolsGeometryReprojector.srsCodeToCRS(tosrs));
	}

}
