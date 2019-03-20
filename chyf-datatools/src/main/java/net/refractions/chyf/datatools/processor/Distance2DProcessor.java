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

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.IntersectionMatrix;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import net.refractions.chyf.datatools.readers.ChyfDataSource;

/**
 * Processor for computing mean and maximum 2d distance to water per catchment.
 * 
 * @author Emily
 *
 */
public class Distance2DProcessor {

	private int cellSize = 1;
	
	private CoordinateReferenceSystem toWork;
	private GeometryFactory gf = new GeometryFactory();
	
	private Distance2DResult distanceToWater;
	private ChyfDataSource dataSource;
	
	public Distance2DProcessor(ChyfDataSource dataSource, CoordinateReferenceSystem crs) {
		this.toWork = crs;
		this.dataSource = dataSource;
	}
	
	public Distance2DResult getResults(){
		return this.distanceToWater;
	}
	
	/**
	 * Default cell size is 1.
	 * @param cellSize
	 */
	//I added this method to simplify the test case
	public void setCellSize(int cellSize) {
		this.cellSize = cellSize;
	}
	
	public void doWork(ProgressMonitor progressMonitor) throws Exception {
		distanceToWater = new Distance2DResult();
				
		//lets make a 1m grid out of this
		int total = 0;
		try(SimpleFeatureReader reader = dataSource.getECatchments(null)){
			while(reader.hasNext()) { reader.next(); total++;}
		}
		progressMonitor.setTaskLength(total);
		
		try(SimpleFeatureReader reader = dataSource.getECatchments(null)){
			while(reader.hasNext()) {
				
				progressMonitor.worked(1);
				
				SimpleFeature sf = reader.next();
				
				Geometry g = (Geometry) sf.getDefaultGeometry();
				Polygon p = null;
				if (g instanceof Polygon) {
					p = (Polygon)g;
				}else if (g instanceof MultiPolygon && (((MultiPolygon)g).getNumGeometries() == 1)) {
					p = (Polygon) ((MultiPolygon)g).getGeometryN(0);
				}else {
					throw new IllegalStateException("Geometry of type " + g.getClass().toString() + " is not supported for ecatchment.");
				}
				
				boolean process = true;
				List<LineString> waterEdges = new ArrayList<>();
				
				try(SimpleFeatureReader wbReader = dataSource.getWaterbodies( new ReferencedEnvelope(p.getEnvelopeInternal(), sf.getType().getCoordinateReferenceSystem()))){
					while(wbReader.hasNext()) {
						SimpleFeature sb = wbReader.next();
						Geometry wbGeom = (Geometry) sb.getDefaultGeometry();
					
						IntersectionMatrix matrix = p.relate(wbGeom);
						if(matrix.isEquals(3,3)) {
							//overlaps entirely - this is distance to water = 0
							distanceToWater.addResult(sf.getID(), 0.0, 0.0);
							process = false;
							break;
						}else if (matrix.matches("****1****")){
							//boundary intersection is line - get these lines as water edge
							Geometry intersection = p.intersection(wbGeom);
							for (int i = 0; i < intersection.getNumGeometries(); i ++) {
								if (intersection.getGeometryN(i) instanceof LineString) {
									LineString ls = (LineString)(intersection.getGeometryN(i));
									waterEdges.add(ReprojectionUtils.reproject(ls, sb.getType().getCoordinateReferenceSystem(), toWork));
								}
							}
						}
					}
				}
				if (!process) continue;
				
				//add flowpaths to list of edges
				try(SimpleFeatureReader fpReader = dataSource.getFlowpaths( new ReferencedEnvelope(p.getEnvelopeInternal(), sf.getType().getCoordinateReferenceSystem()))){
					while(fpReader.hasNext()) {
						SimpleFeature sb = fpReader.next();
						Geometry fpGeom = (Geometry) sb.getDefaultGeometry();
						if (p.relate(fpGeom, "1********")){
							for (int i = 0; i < fpGeom.getNumGeometries(); i ++) {
								if (fpGeom.getGeometryN(i) instanceof LineString) {
									LineString ls = (LineString)(fpGeom.getGeometryN(i));
									waterEdges.add(ReprojectionUtils.reproject(ls, sb.getType().getCoordinateReferenceSystem(), toWork));
								}
							}
						}
					}
				}
				
				if (waterEdges.isEmpty()) {
					System.out.println("ERROR: NO WATER EDGES IN CATCHMENT");
					continue;
				}
				
				//reproject
				p = ReprojectionUtils.reproject(p, sf.getType().getCoordinateReferenceSystem(), toWork);

				//need to get all water edges that bound or reside in the polygon
				double[] value = processFeature(p, waterEdges);
				distanceToWater.addResult(sf.getID(), value[0], value[1]);
			}
		}
		
		
	}
	private double[] processFeature(Polygon polygon, List<LineString> waterEdges) {
		Envelope env = polygon.getEnvelopeInternal();
		
		int size = cellSize;
		
		int startx = (int)Math.floor( env.getMinX() / size ) * size;
		int starty = (int)Math.floor( env.getMinY() / size ) * size;
		
		int endx = (int)Math.ceil( env.getMaxX() /size ) * size;
		int endy = (int)Math.ceil( env.getMaxY() /size) * size;
		
		PreparedPolygon pp = new PreparedPolygon(polygon);
		
		double distanceSum = 0;
		double maxDistance = Double.NaN;
		int count = 0;
		for (int x = startx; x <= endx; x += size) {
			for (int y = starty; y <= endy; y += size) {
				Point p = gf.createPoint(new Coordinate(x,y));
				if (pp.contains(p)) {
					double d = Double.MAX_VALUE;
					for (LineString ls : waterEdges) {
						double d1 = DistanceOp.distance(p, ls);
						if (d1 < d) d = d1;
					}
					if (d != Double.MAX_VALUE) {
						distanceSum += d;
						count ++;
						
						if (Double.isNaN(maxDistance)) {
							maxDistance = d;
						}else if (d > maxDistance) {
							maxDistance = d;
						}
					}
				}
			}
			
		}
		if (count == 0) return new double[] {Double.NaN, Double.NaN};
		return new double[] {distanceSum / count, maxDistance};
	}
}
