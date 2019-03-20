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

import java.nio.file.Path;

import org.geotools.data.FeatureReader;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import net.refractions.chyf.hygraph.HyGraphBuilder;

/**
 * Reads a geopackage input dataset.  
 * 
 * @author Emily
 *
 */
public class ChyfGeoPackageReader extends ChyfDataReader{

	public static final String FLOWPATH_LAYER = "Flowpath";
	public static final String CATCHMENT_LAYER = "ElementaryCatchment";
	public static final String WATERBODY_LAYER = "Waterbody";
	public static final String BOUNDARY_LAYER = "Working_limit";
	
	private Path geopackageFile ;
	public ChyfGeoPackageReader(Path geopackageFile) {
		this.geopackageFile = geopackageFile;
	}
	
	public void read(HyGraphBuilder builder) throws Exception {
			
		GeoPackage reader = new GeoPackage(geopackageFile.toFile());
		
	    logger.info("Reading waterbodies");
	    FeatureEntry items = reader.feature(WATERBODY_LAYER);
		if (items == null) throw new Exception("No waterbody layer found in geopackage file");
		try(FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = reader.reader(items,  null,  null)){
			super.readWaterbody(featureReader, builder);
		}
	    
		logger.info("Reading catchments");
		items = reader.feature(CATCHMENT_LAYER);
		if (items == null) throw new Exception("No Catchment layer found in geopackage file");
		try(FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = reader.reader(items,  null,  null)){
			super.readCatchment(featureReader, builder);
		}
		
		logger.info("Reading flowpaths");
		items = reader.feature(FLOWPATH_LAYER);
		if (items == null) throw new Exception("No Flowpath layer found in geopackage file");
		try(FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = reader.reader(items,  null,  null)){
			super.readFlowpath(featureReader, builder);
		}
		
		logger.info("Reading boundary");
		items = reader.feature(BOUNDARY_LAYER);
		if (items == null) throw new Exception("No boundary layer found in geopackage file");
		try(FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = reader.reader(items,  null,  null)){
			super.readBoundary(featureReader, builder);
		}
		
	}
}
