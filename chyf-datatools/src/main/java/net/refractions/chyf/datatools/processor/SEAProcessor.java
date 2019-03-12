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
package net.refractions.chyf.datatools.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.datatools.readers.ChyfDataSource;
import net.refractions.chyf.datatools.readers.GeoTiffDemReader;

/**
 * Computes slope/aspect/elevation statistics for catchments
 * @author Emily
 *
 */
public class SEAProcessor {

	static final Logger logger = LoggerFactory.getLogger(ChyfDataSource.class.getCanonicalName());

	private int processingtilesize = 1024; 
	
	private ChyfDataSource data;
	private GeoTiffDemReader dem;
	
	private List<Tile> tiles;
	
	public SEAProcessor (ChyfDataSource data, GeoTiffDemReader dem) throws IOException {
		this.data = data;
		this.dem = dem;
		this.tiles = generateTiles();
	}
	
	//Feature id:
	//Generation of the identifier is dependent on the underlying data storage medium. 
	//Often this identifier is not persistent. Mediums such shapefiles and database tables
	//have "keys" built in which map naturally to persistent feature identifiers. But other
	//mediums do not have such keys and may have to generate feature identifiers "on-the-fly".
	//This means that client code being able to depend on this value as a persistent entity is
	//dependent on which storage medium or data source is being used.
	
	public SEAResult doWork(ProgressMonitor monitor) throws Exception {
		//process by tile
		List<SEAResult> results = new ArrayList<>();
		
		monitor.setTaskLength(tiles.size()+1);
		
		for (Tile tile : tiles) {
			results.add( (new SEATileProcessor(dem, data)).doWork(tile) );
			monitor.worked(1);
		}
		
		//merge all results
		SEAResult total = new SEAResult();
		results.forEach(r->total.merge(r));
			
		//compute values for catchments with no data
		//assigne the values to those of the catchment with the longest shared edge
		SimpleFeatureReader all = data.getECatchments(null);
		while(all.hasNext()) {
			SimpleFeature feature = all.next();
			if (!total.getStats().containsKey(feature.getID())) {
				//compute a value for this based on the longest shared edge
				logger.warn("Catchment " + feature.getID() + " has no SEA values.  Will assign values of the catchment with the longest shared edge");
				SEAResult.Statistics s = findLongestSharedEdgeWithStats(feature, total);
				total.getStats().put(feature.getID(), s);
			}
		}
		monitor.worked(1);
		return total;
	}
	
	private SEAResult.Statistics findLongestSharedEdgeWithStats(SimpleFeature currentFeature, SEAResult allData) throws Exception{
		ReferencedEnvelope toSearch = new ReferencedEnvelope(currentFeature.getBounds());
		Geometry g = (Geometry) currentFeature.getDefaultGeometry();
		try(SimpleFeatureReader near = data.getECatchments(toSearch)){
			SimpleFeature longestShared = null;
			double length = Double.MIN_VALUE;
			while(near.hasNext()) {
				SimpleFeature n = near.next();
				if (!allData.getStats().containsKey(n.getID())) continue;  //there are no stats for this polygons so don't test it
			
				Geometry gn = (Geometry)n.getDefaultGeometry();
				Geometry intersection = g.intersection(gn);
				if (intersection.getLength() > length) {
					length = intersection.getLength();
					longestShared = n;
				}
			}
			if (longestShared == null) {
				logger.warn("Unable to compute slope aspect elevation for feature " + currentFeature.getID() + ".  No DEM points and no surrounding polygons with values.");
				//log warning
				return null;
			}else {
				return allData.getStats().get(longestShared.getID()).clone();
			}
		}
	}
	
	private List<Tile> generateTiles() throws IOException {
		GeneralEnvelope genv = dem.getFileBounds();
		ReferencedEnvelope env = new ReferencedEnvelope(genv.getMinimum(0), genv.getMaximum(0), genv.getMinimum(1), genv.getMaximum(1), dem.getCrs());
		
		int numx = (int) Math.ceil( env.getWidth() / processingtilesize );
		int numy = (int) Math.ceil( env.getHeight() / processingtilesize );
		
		List<Tile> toProcess = new ArrayList<>();
		
		for (int x = 0; x < numx; x ++) {
			for (int y = 0; y < numy; y ++) {
				double x1 = x * processingtilesize + env.getMinX();
				double y1 = y * processingtilesize + env.getMinY();
				double x2 = (x+1) * processingtilesize + env.getMinX();
				double y2 = (y+1) * processingtilesize + env.getMinY();
				ReferencedEnvelope re = new ReferencedEnvelope(x1, x2, y1, y2, env.getCoordinateReferenceSystem());
				
				ReprojectionUtils.reproject(re, env.getCoordinateReferenceSystem());
				
				toProcess.add(new Tile(re));
			}
		}
		return toProcess;
	}
}
