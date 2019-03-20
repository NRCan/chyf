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
package net.refractions.chyf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import net.refractions.chyf.hygraph.HyGraphBuilder;

/**
 * Reads a shapefile input dataset
 * @author Emily
 *
 */
public class ChyfShapeDataReader extends ChyfDataReader {
	
	public static final String FLOWPATH_FILE = "Flowpath.shp";
	public static final String CATCHMENT_FILE = "Catchment.shp";
	public static final String WATERBODY_FILE = "Waterbody.shp";
	
	public static final String BOUNDARY_FILE = "Working_limit.shp";
	
	private String directory;
	
	public ChyfShapeDataReader(String shapeDir) {
		this.directory = shapeDir;
	}
	
	public void read(HyGraphBuilder builder) throws Exception {
		String shapeDir = this.directory;
		
	    Query query = new Query();
	    
	    logger.info("Reading waterbodies shapefile");
	    DataStore waterbodyDataStore = getShapeFileDataStore(shapeDir + WATERBODY_FILE);
	    query.setTypeName(waterbodyDataStore.getTypeNames()[0]);
	    try(FeatureReader<SimpleFeatureType, SimpleFeature> reader = waterbodyDataStore.getFeatureReader(query, null)){
	    	super.readWaterbody(reader, builder);
	    }
	    waterbodyDataStore.dispose();
		
	    logger.info("Reading catchments shapefile");
		DataStore catchmentDataStore = getShapeFileDataStore(shapeDir + CATCHMENT_FILE);
		query.setTypeName(catchmentDataStore.getTypeNames()[0]);
		try(FeatureReader<SimpleFeatureType, SimpleFeature> reader = catchmentDataStore.getFeatureReader(query, null)){
	    	super.readCatchment(reader, builder);
	    }
	    catchmentDataStore.dispose();
	    
	    logger.info("Reading flowpaths shapefile");
		DataStore flowPathDataStore = getShapeFileDataStore(shapeDir + FLOWPATH_FILE);
		query.setTypeName(flowPathDataStore.getTypeNames()[0]);
		try(FeatureReader<SimpleFeatureType, SimpleFeature> reader = flowPathDataStore.getFeatureReader(query, null)){
	    	super.readFlowpath(reader, builder);
	    }
	    flowPathDataStore.dispose();
	    
	    logger.info("Reading boundary shapefile");
		DataStore boundaryDataStore = getShapeFileDataStore(shapeDir + BOUNDARY_FILE);
		query.setTypeName(boundaryDataStore.getTypeNames()[0]);
		try(FeatureReader<SimpleFeatureType, SimpleFeature> reader = boundaryDataStore.getFeatureReader(query, null)){
	    	super.readBoundary(reader, builder);
	    }
	    boundaryDataStore.dispose();
	}
	
	
	private DataStore getShapeFileDataStore(String fileName) throws IOException {
	    File file = new File(fileName);
	    Map<String, Object> map = new HashMap<String, Object>();
		map.put("url", file.toURI().toURL());
		map.put("charset", Charset.forName("ISO-8859-1"));
	    return DataStoreFinder.getDataStore(map);
	}
}
