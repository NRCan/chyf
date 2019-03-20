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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.indexing.PredicateFilter;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;
import net.refractions.chyf.rest.PredicateParameter;

/**
 * Tests strahler, horton and hack order assignments
 * 
 * This test makes use of the hygraph network and the
 * order_results.json file.
 * 
 */
public class StreamOrderTest {
	
	/**
	 * The expected results for order computations.  This file
	 * should be a point geojson file with each feature containing  
	 * "strahlerorder", "hortonorder", and "hackorder".  The geometry should
	 * be a point geometry that is next to the linestring being tested. All data 
	 * should be in same projection as the test data.
	 * 
	 */
	public static final String ORDER_EXPECTED_RESULTS = BasicTestSuite.RESULTS_DIR + "/order_results.json";
	
	@Rule
	public TestRule rule = BasicTestSuite.SETUP_RULE;

	@BeforeClass
	public static void setup() throws Exception{
		//ensure file exists
		try(InputStream is = ClassLoader.getSystemResourceAsStream(ORDER_EXPECTED_RESULTS)){
		}		
	}
	

	@Test
	public void test_findEFlowpaths() throws URISyntaxException{
		ChyfDatastore datastore = BasicTestSuite.DATASTORE;
		Coordinate c = new Coordinate(-73.13851109999999, 45.99991639999997);
		Point pnt = BasicTestSuite.GF.createPoint(c);
		pnt = GeotoolsGeometryReprojector.reproject(pnt, BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);
		List<EFlowpath> paths = datastore.getHyGraph().findEFlowpaths(pnt, 1, 1.0, 
				new PredicateFilter<EFlowpath>(EFlowpath::getType, 
						PredicateParameter.notEquals.get(), 
						FlowpathType.BANK));
		Assert.assertEquals(1, paths.size());
		Assert.assertTrue(paths.get(0).getLineString().getNumPoints() > 2);
	}


	@Test
	public void test_Order() throws Exception {
		ChyfDatastore datastore = BasicTestSuite.DATASTORE;
		List<TestPoint> tests = readOrderJson();
		for (TestPoint point : tests) {		
			Point pnt = GeotoolsGeometryReprojector.reproject(point.point, BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);
			List<EFlowpath> paths = datastore.getHyGraph().findEFlowpaths(pnt, 1, 1.0,  null);
						
			Assert.assertEquals(1, paths.size());				
			EFlowpath path = paths.get(0);
			Assert.assertEquals("Strahler order failure at POINT(" + point.point.getX() + " " + point.point.getY() + ")", point.strahlerorder, path.getStrahlerOrder());
			Assert.assertEquals("Horton order failure at POINT(" + point.point.getX() + " " + point.point.getY() + ")", point.hortonorder, path.getHortonOrder());
			Assert.assertEquals("Hack order failure at POINT(" + point.point.getX() + " " + point.point.getY() + ")", point.hackorder, path.getHackOrder());			
		}
		
	}
	
	/*
	 * reads expected results for order from json file
	 * 
	 */
	private List<TestPoint> readOrderJson() throws Exception{
		List<TestPoint> results = new ArrayList<>();
		try(JsonReader reader = new JsonReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(ORDER_EXPECTED_RESULTS)))){
			reader.beginArray();
			while(reader.hasNext()) {
				reader.beginObject();
			
				Double x = null; 
				Double y = null;
				Integer sorder= null;
				Integer horder= null;
				Integer hackorder= null;
				
				while(reader.hasNext()) {
					
					String name = reader.nextName();
					if (name.equals("geometry")) {
						reader.beginObject();
						String type = null;
						while(reader.hasNext()) {
							String geomname = reader.nextName();
							if (geomname.equals("type")) {
								type = reader.nextString();
							}else if (geomname.equals("coordinates")) {
								if (type.equalsIgnoreCase("Point")) {
									reader.beginArray();
									x = reader.nextDouble();
									y = reader.nextDouble();
									reader.endArray();
								}else {
									throw new Exception("Geometry type of " + type + " not supported for order testing.");
								}
							}else {
								reader.skipValue();
							}
						}
						reader.endObject();
					}else if (name.equals("properties")) {
						reader.beginObject();
						while(reader.hasNext()) {
							String geomname = reader.nextName();
							if (reader.peek() == JsonToken.NULL) {
								reader.skipValue();
							}else if (geomname.equals("strahlerorder")) {
								 sorder = reader.nextInt();
							}else if (geomname.equals("hortonorder")) {
								horder = reader.nextInt();
							}else if (geomname.equals("hackorder")) {
								hackorder = reader.nextInt();
							}else {
								reader.skipValue();
							}
						}
						reader.endObject();
					}else {
						reader.skipValue();
					}
					
				}
				reader.endObject();
				
				if (x == null || y == null){
					throw new Exception("Could not read test data successfully");
				}
		
				TestPoint point = new TestPoint();
				point.point = BasicTestSuite.GF.createPoint(new Coordinate(x,y));
				point.strahlerorder = sorder;
				point.hackorder = hackorder;
				point.hortonorder = horder;
				results.add(point);
			}
		}
		return results;
	}
	
	class TestPoint{
		Point point = null;
		Integer strahlerorder = null;
		Integer hortonorder = null;
		Integer hackorder = null;
		
		public TestPoint() {
		}
	}
		
}
