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
package net.refractions.chyf.datatools.readers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.EmptyFeatureReader;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.datatools.processor.ReprojectionUtils;

/**
 * Reads a shapefile input dataset
 * @author Emily
 *
 */
public class ChyfShapeDataSource implements ChyfDataSource {
	
	static final Logger logger = LoggerFactory.getLogger(ChyfDataSource.class.getCanonicalName());
	
	public static final String FLOWPATH_FILE = "Flowpath.shp";
	public static final String CATCHMENT_FILE = "Catchment.shp";
	public static final String WATERBODY_FILE = "Waterbody.shp";
	public static final String BOUNDARY_FILE = "Working_limit.shp";
	
	private static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	
	private Path directory;
	
	private SimpleFeatureSource waterbodies;
	private SimpleFeatureSource catchments;
	private SimpleFeatureSource flowpaths;
	
	public ChyfShapeDataSource(Path shapeDir) throws IOException {
		this.directory = shapeDir;
		read();
	}
	
	private void read() throws IOException{
		logger.info("Reading waterbodies shapefile");
	    DataStore ds = getShapeFileDataStore(directory, WATERBODY_FILE);
	    this.waterbodies = ds.getFeatureSource(ds.getTypeNames()[0]);
	  		
	    logger.info("Reading catchments shapefile");
		ds = getShapeFileDataStore(directory, CATCHMENT_FILE);
		this.catchments = ds.getFeatureSource(ds.getTypeNames()[0]);
		    
	    logger.info("Reading flowpaths shapefile");
		ds = getShapeFileDataStore(directory, FLOWPATH_FILE);
		this.flowpaths = ds.getFeatureSource(ds.getTypeNames()[0]);
	}
	
	
	private DataStore getShapeFileDataStore(Path dir, String fileName) throws IOException {
		File file = dir.resolve(fileName).toFile();
	    Map<String, Object> map = new HashMap<String, Object>();
		map.put("url", file.toURI().toURL());
		map.put("charset", Charset.forName("ISO-8859-1"));
	    return DataStoreFinder.getDataStore(map);
	}
	
	
	public ReferencedEnvelope getCatchmentBounds() throws IOException{
		return catchments.getBounds();
	}
	
	public SimpleFeatureReader getECatchments(ReferencedEnvelope bounds) throws IOException{
		return query(bounds, catchments);
	}
	
	public SimpleFeatureReader getWaterbodies(ReferencedEnvelope bounds) throws IOException{
		return query(bounds, waterbodies);
	}
	
	public SimpleFeatureReader getFlowpaths(ReferencedEnvelope bounds) throws IOException{
		return query(bounds, flowpaths);
	}
	
	private SimpleFeatureReader query(ReferencedEnvelope bounds, SimpleFeatureSource source) throws IOException {
		if (bounds == null) {
			return DataUtilities.simple(DataUtilities.reader(source.getFeatures()));
		}
		
		bounds = ReprojectionUtils.reproject(bounds, source.getSchema().getCoordinateReferenceSystem());
		String geom = source.getSchema().getGeometryDescriptor().getLocalName();
		Filter filter1 = ff.bbox(ff.property(geom), bounds);
		Filter filter2 = ff.intersects(ff.property(geom), ff.literal(JTS.toGeometry(bounds)));
        Filter filter = ff.and(filter1, filter2);
        if (source.getFeatures(filter).size() == 0) {
        	return DataUtilities.simple(new EmptyFeatureReader<SimpleFeatureType, SimpleFeature>(source.getSchema()));
        }else {
        	return DataUtilities.simple(DataUtilities.reader(source.getFeatures(filter)));
        }
	}
	
	@Override
	public void close() throws Exception {
	
	}
}
