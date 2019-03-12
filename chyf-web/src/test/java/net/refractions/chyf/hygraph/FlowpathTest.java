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

import java.io.IOException;
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
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

/**
 * For for testing upstream and downstream flow path features.
 * 
 * @author Emily
 *
 */
public class FlowpathTest {

	/**
	 * Test data and expected results for upstream and downstream 
	 * results.  This is a json file which contains an
	 * array of test data.  Each array contains an object that
	 * contains the point and results which is a array of
	 * linestrings representing the upstream/downstream edges
	 */
	public static final String UPSTREAMFLOWPATH_RESULTS = BasicTestSuite.RESULTS_DIR + "/upstreamflowpath_results.json";
	public static final String DOWNSTREAMFLOWPATH_RESULTS = BasicTestSuite.RESULTS_DIR + "/downstreamflowpath_results.json";
	
	@Rule
	public TestRule rule = BasicTestSuite.SETUP_RULE;
	
	@Test
	public void test_UpstreamFlowPath() throws Exception {
		validateFlowpaths(UPSTREAMFLOWPATH_RESULTS, 
			point-> BasicTestSuite.DATASTORE.getHyGraph().getUpstreamEFlowpaths( BasicTestSuite.DATASTORE.getHyGraph().getEFlowpath(point), ChyfDatastore.MAX_RESULTS),
			"Upstream");
	}
	
	
	
	@Test
	public void test_DownstreamFlowPath() throws Exception {
		validateFlowpaths(DOWNSTREAMFLOWPATH_RESULTS, 
			point-> BasicTestSuite.DATASTORE.getHyGraph().getDownstreamEFlowpaths( BasicTestSuite.DATASTORE.getHyGraph().getEFlowpath(point), ChyfDatastore.MAX_RESULTS),
			"Downstream");
	}
	
	private void validateFlowpaths(String dataFile, Function<Point, Collection<EFlowpath>> function, String type) throws IOException{
		
		HashMap<Coordinate, List<LineString>> testData = new HashMap<>();
		try(JsonReader reader = new JsonReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(dataFile)))){
			reader.beginArray();
			while(reader.hasNext()) {
				reader.beginObject();
				Coordinate point = null;
				List<LineString> lss = new ArrayList<>();
				
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
							reader.beginArray();
							List<Coordinate> points = new ArrayList<>();
							while(reader.hasNext()) {
								reader.beginArray();
								double x = reader.nextDouble();
								double y = reader.nextDouble();
								reader.endArray();
								points.add(new Coordinate(x,y));
							}
							LineString ls = BasicTestSuite.GF.createLineString(points.toArray(new Coordinate[points.size()]));
							lss.add(ls);
							reader.endArray();
						}
						reader.endArray();
					}
				}
				testData.put(point, lss);
				reader.endObject();
			}
		}
		
		for (Entry<Coordinate, List<LineString>> result : testData.entrySet()) {
			Point pnt = GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(result.getKey()),  BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);
			
			Collection<EFlowpath> paths = function.apply(pnt);
			
			Assert.assertEquals(type + " flowpath at (" + result.getKey().x + ", " +result.getKey().y + ") returned incorrect number of segments", result.getValue().size(), paths.size());
			
			for (LineString ls : result.getValue()) {
				LineString projection = GeotoolsGeometryReprojector.reproject(ls,  BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);
				//find the same linestring in the actual results
				EFlowpath found = null;
				for (EFlowpath p : paths) {
					if (p.getLineString().equalsExact(projection, 0.00001)) {
						found = p;
						break;
					}
				}
				Assert.assertNotNull(type + " flowpath at (" + result.getKey().x + ", " +result.getKey().y + ") does not match excepted results", found);
				if (found != null) paths.remove(found);
			}
		}
	}
	 
}
