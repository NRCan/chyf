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
package net.refractions.chyf.hygraph;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.google.gson.stream.JsonReader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.indexing.SpatiallyIndexable;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

/**
 * Tests the multi-dimensional downstream test service.
 * @author Emily
 *
 */
public class MultiDimensionalDownstreamTest {
	
	/**
	 * Test data and expected results for multidimensional
	 * downstream service. This is a json file which contains an
	 * array of test data.  Each array contains an object that
	 * contains the point and results which is a array of
	 * well-known-text representation of expected geometries
	 */
	public static final String DOWNSTREAMELEDRAINAGE_RESULTS = BasicTestSuite.RESULTS_DIR + "/multidimensionaldownstream_results.json";
	public static final String UPSTREAMELEDRAINAGE_RESULTS = BasicTestSuite.RESULTS_DIR + "/multidimensionalupstream_results.json";

	
	@Rule
	public TestRule rule = BasicTestSuite.SETUP_RULE;
	
	@Test
	public void test_MultiDimensionalDownstreamCatchment() throws ParseException {
		try {
			validateCatchments(DOWNSTREAMELEDRAINAGE_RESULTS, 
					point-> BasicTestSuite.DATASTORE.getHyGraph().getDownstreamMultiDimensional(BasicTestSuite.DATASTORE.getHyGraph().getECatchment(point), ChyfDatastore.MAX_RESULTS));
		}catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail(ex.getMessage());
		}
	}
	
	@Test
	public void test_MultiDimensionalUpstreamCatchment() throws ParseException {
		try {
			validateCatchments(UPSTREAMELEDRAINAGE_RESULTS, 
					point-> BasicTestSuite.DATASTORE.getHyGraph().getUpstreamMultiDimensional(BasicTestSuite.DATASTORE.getHyGraph().getECatchment(point), ChyfDatastore.MAX_RESULTS));
		}catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail(ex.getMessage());
		}
	}
	
	private void validateCatchments(String dataFile, Function<Point, Collection<SpatiallyIndexable>> function) throws Exception{
		
		HashMap<Coordinate, List<String>> testData = new HashMap<>();
		try(JsonReader reader = new JsonReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(dataFile)))){
			reader.beginArray();
			while(reader.hasNext()) {
				reader.beginObject();
				Coordinate point = null;
				List<String> geoms = new ArrayList<>();
				
				while(reader.hasNext()) {
					
					String name = reader.nextName();
					if (name.equals("point")) {
						reader.beginArray();
						double x = reader.nextDouble();
						double y = reader.nextDouble();
						reader.endArray();
						point = new Coordinate(x,y);
					}else if (name.equals("results")) {
						reader.beginArray();
						while(reader.hasNext()) {
							geoms.add(reader.nextString());
						}
						reader.endArray();
					}
				}
				testData.put(point, geoms);
				reader.endObject();
			}
		}
		
		WKTReader reader = new WKTReader(BasicTestSuite.GF);
		for (Entry<Coordinate, List<String>> result : testData.entrySet()) {
			Point pnt = GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(result.getKey()),  BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);
			
			Collection<SpatiallyIndexable> paths = function.apply(pnt);
			
			Assert.assertEquals("Multi dimensional service at (" + result.getKey().x + ", " +result.getKey().y + ") returned incorrect number of geometries", result.getValue().size(), paths.size());
			
			for (String ls : result.getValue()) {
				Geometry g = reader.read(ls);
				Geometry projection = GeotoolsGeometryReprojector.reproject(g,  BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);
				projection = GeometryPrecisionReducer.reduce(projection, ChyfDatastore.PRECISION_MODEL);
				//find the same linestring in the actual results
				SpatiallyIndexable found = null;
				for (SpatiallyIndexable p : paths) {
					Geometry totest = null;
					if (p instanceof EFlowpath) totest = ((EFlowpath) p).getLineString();
					if (p instanceof ECatchment) totest = ((ECatchment) p).getPolygon();
							
					if (totest.equalsExact(projection, 0.00001)) {
						found = p;
						break;
					}
				}
				Assert.assertNotNull("Multi dimensional service at (" + result.getKey().x + ", " +result.getKey().y + ") does not match excepted results", found);
				if (found != null) paths.remove(found);
			}
		}
	}
	 
}
