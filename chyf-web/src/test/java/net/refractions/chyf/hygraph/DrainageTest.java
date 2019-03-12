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
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import com.google.gson.stream.JsonReader;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

/**
 * For for testing upstream and downstream elementary drainages feature.
 * 
 * @author Emily
 *
 */
public class DrainageTest {

	/**
	 * Test data and expected results for upstream and downstream 
	 * results.  This is a json file which contains an
	 * array of test data.  Each array contains an object that
	 * contains the point and the wkt representation
	 * of the drainage area.
	 */
	public static final String UPSTREAMEDRAINAGE_HOLE_RESULTS = BasicTestSuite.RESULTS_DIR + "/upstreamdrainage_holes_results.json";
	public static final String UPSTREAMEDRAINAGE_NOHOLE_RESULTS = BasicTestSuite.RESULTS_DIR + "/upstreamdrainage_noholes_results.json";
	public static final String DOWNSTREAMEDRAINAGE_HOLE_RESULTS = BasicTestSuite.RESULTS_DIR + "/downstreamdrainage_holes_results.json";
	public static final String DOWNSTREAMEDRAINAGE_NOHOLE_RESULTS = BasicTestSuite.RESULTS_DIR + "/downstreamdrainage_noholes_results.json";
	
	@Rule
	public TestRule rule = BasicTestSuite.SETUP_RULE;
	
	@Test
	public void test_UpstreamDrainage() {
		try {
			validateDrainage(UPSTREAMEDRAINAGE_HOLE_RESULTS, 
					point-> BasicTestSuite.DATASTORE.getHyGraph().getUpstreamDrainageArea( BasicTestSuite.DATASTORE.getHyGraph().getECatchment(point), false),
					"Upstream");
			
			validateDrainage(UPSTREAMEDRAINAGE_NOHOLE_RESULTS, 
					point-> BasicTestSuite.DATASTORE.getHyGraph().getUpstreamDrainageArea( BasicTestSuite.DATASTORE.getHyGraph().getECatchment(point), true),
					"Upstream");
		}catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail(ex.getMessage());
		}
	}
	
	@Test
	public void test_DownstreamElementaryCatchment() {
		try {
			validateDrainage(DOWNSTREAMEDRAINAGE_HOLE_RESULTS, 
					point-> BasicTestSuite.DATASTORE.getHyGraph().getDownstreamDrainageArea( BasicTestSuite.DATASTORE.getHyGraph().getECatchment(point), false),
					"Downstream");
			
			validateDrainage(DOWNSTREAMEDRAINAGE_NOHOLE_RESULTS, 
					point-> BasicTestSuite.DATASTORE.getHyGraph().getDownstreamDrainageArea( BasicTestSuite.DATASTORE.getHyGraph().getECatchment(point), true),
					"Downstream");
		}catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail(ex.getMessage());
		}
	}
	
	private void validateDrainage(String dataFile, Function<Point, DrainageArea> function, String type) throws Exception{
		
		HashMap<Coordinate, String> testData = new HashMap<>();
		try(JsonReader reader = new JsonReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(dataFile)))){
			reader.beginArray();
			while(reader.hasNext()) {
				reader.beginObject();
				Coordinate point = null;
				String text = null;
				
				while(reader.hasNext()) {
					String name = reader.nextName();
					if (name.equals("point")) {
						reader.beginArray();
						double x = reader.nextDouble();
						double y = reader.nextDouble();
						reader.endArray();
						point = new Coordinate(x,y);
					}else if (name.equals("results")) {
						text = reader.nextString();
					}
				}
				testData.put(point, text);
				reader.endObject();
			}
		}
		
		for (Entry<Coordinate, String> result : testData.entrySet()) {
			Point pnt = GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(result.getKey()), BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);
			
			DrainageArea paths = function.apply(pnt);
			
			Geometry expectedResult = (new WKTReader()).read(result.getValue());
			expectedResult.setSRID(BasicTestSuite.TEST_DATA_SRID);
			expectedResult = GeotoolsGeometryReprojector.reproject(expectedResult,  BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);
			expectedResult = GeometryPrecisionReducer.reduce(expectedResult, ChyfDatastore.PRECISION_MODEL);
			
			Geometry overlap = expectedResult.intersection(paths.getGeometry());
			double a1 = paths.getGeometry().getArea();
			double a2 = overlap.getArea();
			if (  Math.abs((a2 - a1) / a2) > 0.002  ) {
//			if (!paths.getGeometry().equalsExact(expectedResult, 0.00001)) {
				Assert.fail(type + " drainage at (" + result.getKey().x + ", " +result.getKey().y + ") does not match excepted results");
			}
		}
	}
	 
}
