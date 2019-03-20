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

import java.awt.image.DataBuffer;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeature;

import net.refractions.chyf.datatools.readers.ChyfDataSource;
import net.refractions.chyf.datatools.readers.GeoTiffDemReader;

/**
 * Computes slope/aspect/elevation stats for all DEM points inside
 * a tile, assigning the results to the catchment that intersects the
 * center point of the DEM grid cell.
 * 
 * @author Emily
 *
 */
public class SEATileProcessor  {
	
	private GeoTiffDemReader elevationData;
	private ChyfDataSource datasource;

	public SEATileProcessor(GeoTiffDemReader reader, ChyfDataSource datasource) {
		this.elevationData = reader;
		this.datasource = datasource;
	}
	
	
	public SEAResult doWork(Tile t) {
		try {
			//get features that overlap elevation data
			SimpleFeatureReader sfreader = datasource.getECatchments(t.getEnvelope());
			List<Geometry> sf = new ArrayList<>();
			while(sfreader.hasNext()) {
				SimpleFeature ff = sfreader.next();
				Geometry geom = ((Geometry)ff.getDefaultGeometry());			
				Geometry rgeom = ReprojectionUtils.reproject(geom, sfreader.getFeatureType().getCoordinateReferenceSystem(), elevationData.getCrs());
				rgeom.setUserData(ff.getID());
				sf.add(rgeom);
			}
			if (sf.isEmpty()) return new SEAResult();
			
			STRtree index = new STRtree();
			for (Geometry g : sf) {
				PreparedGeometry pg = PreparedGeometryFactory.prepare(g);
				index.insert(pg.getGeometry().getEnvelopeInternal(), pg);
			}
			index.build();
			
			//expand by 1 on each side so we have the data necessary to compute slope
			ReferencedEnvelope dataEnv = t.getEnvelope();
			dataEnv = new ReferencedEnvelope(dataEnv.getMinX() - elevationData.getXCellSize(), dataEnv.getMaxX() + elevationData.getXCellSize(), dataEnv.getMinY() - elevationData.getYCellSize(), dataEnv.getMaxY() + elevationData.getYCellSize(), dataEnv.getCoordinateReferenceSystem());
			
			GridCoverage2D dem = elevationData.getData(dataEnv);
			
			
			double xmin = dem.getEnvelope().getMinimum(0);
			double ymin = dem.getEnvelope().getMinimum(1);
			
			DataBuffer dataBuffer = null;
			SEAResult results = new SEAResult();
			
			for (int x = 1 ; x < dem.getRenderedImage().getWidth() - 1; x ++) {
				for (int y = 1 ; y < dem.getRenderedImage().getHeight() - 1; y ++) {	
					double xCenter = xmin + x * elevationData.getXCellSize() + (elevationData.getXCellSize() / 2.0);
					double yCenter = ymin + y * elevationData.getYCellSize() + (elevationData.getYCellSize() / 2.0);
					
					Envelope searchEnv = new Envelope(new Coordinate(xCenter, yCenter));
					Point pnt = (new GeometryFactory()).createPoint(new Coordinate(xCenter, yCenter));
					
					List<?> items = index.query(searchEnv);
					List<PreparedGeometry> geoms = new ArrayList<>();
					for (Object item : items) {
						PreparedGeometry pg = (PreparedGeometry) item;
						if (pg.contains(pnt)) geoms.add(pg);
					}
					if (geoms.isEmpty()) continue;
					
					if (dataBuffer == null) {
						dataBuffer = dem.getRenderedImage().getData().getDataBuffer();
					}

					double v1 = dataBuffer.getElemDouble(x-1 +   (dem.getRenderedImage().getHeight() - (y+1) - 1) * dem.getRenderedImage().getWidth());
					double v2 = dataBuffer.getElemDouble(x +     (dem.getRenderedImage().getHeight() - (y+1) - 1) * dem.getRenderedImage().getWidth());
					double v3 = dataBuffer.getElemDouble(x + 1 + (dem.getRenderedImage().getHeight() - (y+1) - 1) * dem.getRenderedImage().getWidth());
					
					double v4 = dataBuffer.getElemDouble(x-1 +   (dem.getRenderedImage().getHeight() - y - 1) * dem.getRenderedImage().getWidth());
					double v5 = dataBuffer.getElemDouble(x +     (dem.getRenderedImage().getHeight() - y - 1) * dem.getRenderedImage().getWidth());
					double v6 = dataBuffer.getElemDouble(x + 1 + (dem.getRenderedImage().getHeight() - y - 1) * dem.getRenderedImage().getWidth());
					
					double v7 = dataBuffer.getElemDouble(x-1 +   (dem.getRenderedImage().getHeight() - (y-1) - 1) * dem.getRenderedImage().getWidth());
					double v8 = dataBuffer.getElemDouble(x +     (dem.getRenderedImage().getHeight() - (y-1) - 1) * dem.getRenderedImage().getWidth());
					double v9 = dataBuffer.getElemDouble(x + 1 + (dem.getRenderedImage().getHeight() - (y-1) - 1) * dem.getRenderedImage().getWidth());
					
					double[][] edata = new double[][] {
						{v1, v4, v7},
						{v2, v5, v8},
						{v3, v6, v9},
					};
					
					double[] slopeaspect = computeSlopeAspect(edata);
					if (slopeaspect[0] == -9999) {
						if (!isNoData(v5)) {
							for (PreparedGeometry g : geoms) results.addElevationValue((String)g.getGeometry().getUserData(),  v5);
						}
					}else {
						for (PreparedGeometry g : geoms) {
							results.addSlopeAspectElevationValue((String)g.getGeometry().getUserData(), slopeaspect[0], slopeaspect[1], v5);
						}
					}
				}
			}
			return results;
		}catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private boolean isNoData(double v) {
		for (int k = 0; k < elevationData.getNoData().length; k ++) {
			if (v == elevationData.getNoData()[k]) return true;
		}
		return false;
	}
	
	//
	//Horn slope algorithm
	// slope = arctan(sqrt(fx2 + fy2))
	// 987
	// 654
	// 321
	//
	//fx = (z3-z1+2(z6-z4)+z9-z7)/8g
	//fy=(z7-z1+2(z8-z2)+z9-z3)/8g   
	/**
	 * Computes slope using the Horn Algorithm and returns the results
	 * in degrees.
	 * 
	 * http://people.csail.mit.edu/bkph/papers/Hill-Shading.pdf
	 * 
	 * 
	 * @param elevation double array of elevation data
	 * <pre>
	 * [a][b][c]
	 * [d][e][f]
	 * [g][h][i]
	 * 
	 * elevation[0][0] = a
	 * elevation[1][0] = b
	 * elevation[2][0] = c
	 * 
	 * elevation[0][1] = d
	 * elevation[1][1] = e
	 * elevation[2][1] = f
	 * 
	 * elevation[0][2] = g
	 * elevation[1][2] = h
	 * elevation[2][2] = i
	 * 
	 * </pre>
	 * @return double array where the first value is slope and the second is aspect. 
	 * If any of the input cell data is no data then -9999 is returned.
	 * 
	 */
	//http://desktop.arcgis.com/en/arcmap/10.3/tools/spatial-analyst-toolbox/how-slope-works.htm
	//http://desktop.arcgis.com/en/arcmap/10.3/tools/spatial-analyst-toolbox/how-aspect-works.htm
	
	public double[] computeSlopeAspect(double[][] elevation) {
		for (int i = 0; i <  elevation.length; i ++) {
			for (int j = 0; j < elevation.length; j ++) {
				if (isNoData(elevation[i][j])) return new double[] {-9999};
			}
		}

		double dx = (elevation[2][0] + 2*elevation[2][1] + elevation[2][2]) - (elevation[0][0] + 2*elevation[0][1] + elevation[0][2]);
		double dy = (elevation[0][2] + 2*elevation[1][2] + elevation[2][2]) - (elevation[0][0] + 2*elevation[1][0] + elevation[2][0]); 
				
		double fx = dx / (8.0 * elevationData.getXCellSize()) ;
		double fy = dy / (8.0 * elevationData.getYCellSize()) ;
		
		double slope = Math.atan( Math.sqrt( fx * fx + fy * fy) );
		//convert to degress
		slope = slope * 180 / Math.PI;

		double aspect = Math.atan2((dy / 8.0), (-dx / 8.0)) * (180 / Math.PI);
//		//rotate north
		if (aspect < 0) {
			aspect = 90.0 - aspect;
		}else if (aspect > 90) {
			aspect = 360.0 - aspect + 90.0;
		}else {
			aspect = 90.0 - aspect;
		}
		return new double[] {slope, aspect};
	}
	
}
