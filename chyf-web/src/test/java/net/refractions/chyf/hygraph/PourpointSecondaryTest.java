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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;


/**
 * These test cases are specifically aimed at testing the processing of secondary flows.
 * 
 * The expected results from these pourpoints are stored in the pourpoint_secondary_X.json files
 * in the results test folder.
 * 
 * @author Emily
 *
 */
public class PourpointSecondaryTest {
	
	@Rule
	public TestRule rule = BasicTestSuite.SETUP_RULE;
	
	//test pourpoints
	private Coordinate[][] testPourpoints = new Coordinate[][] {
		{new Coordinate(-73.12938, 45.49113), new Coordinate(-73.14689, 45.46687)},
		{new Coordinate(-73.19585, 45.48135), new Coordinate(-73.19890, 45.47975), new Coordinate(-73.19770, 45.47903), new Coordinate(-73.19915, 45.47917)},
		{new Coordinate(-73.21821, 45.53076), new Coordinate(-73.22057, 45.54014), new Coordinate(-73.22628, 45.52174)}
	};
	
	
	@Test
	public void test_PourpointSecondary1() throws ParseException, IOException {
		int counter = 1;
		for (Coordinate[] test : testPourpoints) {
			List<Pourpoint> points = new ArrayList<>();
			for (int i = 0; i < test.length; i ++) {
				points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(test[i]), BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS), -2, "P" + (i+1)));
			}
			PourpointEngine engine = new PourpointEngine(points, BasicTestSuite.DATASTORE.getHyGraph(), true);
			PourpointOutput out = engine.compute(null);
			
			String file = BasicTestSuite.RESULTS_DIR + "/pourpoint_secondary_" + counter + ".json";
			counter ++;
			
			HashMap<String, HashMap<String,String>> typeidgeom = new HashMap<>();
			HashMap<String, Object[][]> relationships = new HashMap<>();
			try(JsonReader reader = new JsonReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(file)))){
				reader.beginObject();
				while(reader.hasNext()) {
					String key = reader.nextName();
					if (key.equals(PourpointEngine.OutputType.CATCHMENTS.key) ||
						key.equals(PourpointEngine.OutputType.OUTPUT_PP.key) || 
						key.equals(PourpointEngine.OutputType.SUBCATCHMENTS.key) || 
						key.equals(PourpointEngine.OutputType.PARTITIONED_CATCHMENTS.key)) {
											
						HashMap<String, String> idgeom = new HashMap<>();
						typeidgeom.put(key,  idgeom);
						
						reader.beginObject();
						while(reader.hasNext()) {
							String id = reader.nextName();
							String wkt = reader.nextString();
							idgeom.put(id, wkt);
						}
						reader.endObject();
					}else if (key.equals(PourpointEngine.OutputType.CATCHMENT_CONTAINMENT.key) ||
							key.equals( PourpointEngine.OutputType.SUBCATCHMENT_RELATIONSHIP.key) || 
							key.equals( PourpointEngine.OutputType.PARTITIONED_CATCHMENT_RELATION.key)) {
						//int array
						List<List<Integer>> items = new ArrayList<>();
						reader.beginArray();
						while(reader.hasNext()) {
							reader.beginArray();
							ArrayList<Integer> i = new ArrayList<>();
							items.add(i);
							while(reader.hasNext()) {
								if (reader.peek()== JsonToken.NULL) {
									reader.nextNull();
									i.add(null);
								}else {
									i.add(reader.nextInt());
								}
							}
							reader.endArray();
						}
						reader.endArray();
						
						Integer[][] array = new Integer[items.size()][items.size()];
						for (int i = 0; i < array.length; i ++) {
							for (int j = 0; j < array.length; j ++) {
								array[i][j] = items.get(i).get(j);
							}
						}
						relationships.put(key,  array);
					}else if (key.equals(PourpointEngine.OutputType.DISTANCE_MAX.key) ||
							key.equals(PourpointEngine.OutputType.DISTANCE_MIN.key)) { 
						//int array
						List<List<Double>> items = new ArrayList<>();
						reader.beginArray();
						while(reader.hasNext()) {
							reader.beginArray();
							ArrayList<Double> i = new ArrayList<>();
							items.add(i);
							while(reader.hasNext()) {
								
								if (reader.peek()== JsonToken.NULL) {
									reader.nextNull();
									i.add(null);
								}else {
									i.add(reader.nextDouble());
								}
							}
							reader.endArray();
						}
						reader.endArray();
						
						Double[][] array = new Double[items.size()][items.size()];
						for (int i = 0; i < array.length; i ++) {
							for (int j = 0; j < array.length; j ++) {
								array[i][j] = items.get(i).get(j);
							}
						}
						relationships.put(key,  array);
					}
				}
				reader.endObject();
			}
			WKTReader reader = new WKTReader(BasicTestSuite.GF);

			//pourpoints
			for (Pourpoint p : out.getPoints()) {
				String wkt = typeidgeom.get(PourpointEngine.OutputType.OUTPUT_PP.key).get(p.getId());
				Geometry g = reader.read(wkt);
				Geometry a = GeotoolsGeometryReprojector.reproject(p.getProjectedPoint(),  ChyfDatastore.BASE_CRS, BasicTestSuite.TEST_CRS);
				if (!g.equalsExact(a, 0.00001)) {
					Assert.fail("file: " + file + " - catchment incorrect for pp: " + p.getId());
				}
			}
			
			//catchments
			for (Pourpoint p : out.getPoints()) {
				String wkt = typeidgeom.get(PourpointEngine.OutputType.CATCHMENTS.key).get(p.getId());
				Geometry g = reader.read(wkt);
				Geometry a = GeotoolsGeometryReprojector.reproject(out.getCatchment(p).getGeometry(),  ChyfDatastore.BASE_CRS, BasicTestSuite.TEST_CRS);
				if (!g.equalsExact(a, 0.00001)) {
					System.out.println(a.toText());
					System.out.println(g.toText());
					Assert.fail("file: " + file + " - " + PourpointEngine.OutputType.CATCHMENTS.layername + " incorrect for pp: " + p.getId());

				}
			}
			
			//non overlapping catchments
			for (Pourpoint p : out.getPoints()) {
				String wkt = typeidgeom.get(PourpointEngine.OutputType.SUBCATCHMENTS.key).get(p.getId());
				Geometry g = reader.read(wkt);
				Geometry a = GeotoolsGeometryReprojector.reproject(out.getSubcatchment(p).getGeometry(), ChyfDatastore.BASE_CRS, BasicTestSuite.TEST_CRS);
				if (!g.equalsExact(a, 0.00001)) {
					Assert.fail("file: " + file + " - " + PourpointEngine.OutputType.SUBCATCHMENTS.layername + " incorrect : " + p.getId());
				}
			}
			
			//upstream unique sub catchments
			HashMap<String,String> exptcc = typeidgeom.get(PourpointEngine.OutputType.PARTITIONED_CATCHMENTS.key);
			HashMap<String,String> systemToExp = new HashMap<>();
			
			for (DrainageArea s : out.getPartitionedCatchments()) {
				String systemId = s.getId();
					
				Entry<String,String> matched = null;
					
				for (Entry<String,String> expected : exptcc.entrySet()) {
					String wkt = expected.getValue();
					Geometry g = reader.read(wkt);
					Geometry a = GeotoolsGeometryReprojector.reproject(s.getGeometry(), ChyfDatastore.BASE_CRS, BasicTestSuite.TEST_CRS);
					if (g.equalsExact(a, 0.00001)) {
						matched = expected;
						break;
						}
				}
				if (matched == null) {
					Assert.fail("file: " + file + " - " + PourpointEngine.OutputType.PARTITIONED_CATCHMENTS.layername );
				}
				systemToExp.put(systemId, matched.getKey());
			}
			
			
			//relationships
			Integer[][] actual = out.getCatchmentContainment();
			Integer[][] expected = (Integer[][]) relationships.get(PourpointEngine.OutputType.CATCHMENT_CONTAINMENT.key);
			for (int i = 0; i < expected.length; i ++) {
				for (int j = 0; j < expected.length; j ++) {
					Assert.assertEquals("file: " + file + " - catchment containment relationship incorrect", expected[i][j], actual[i][j]);
				}
			}
			
			actual = out.getPartitionedCatchmentRelationship();
			expected = (Integer[][]) relationships.get(PourpointEngine.OutputType.PARTITIONED_CATCHMENT_RELATION.key);
			for (int i = 0; i < expected.length; i ++) {
				for (int j = 0; j < expected.length; j ++) {
					Assert.assertEquals("file: " + file + " - traversal compliant catchment relationship incorrect", expected[i][j], actual[i][j]);
				}
			}
			
			actual = out.getSubCatchmentRelationship();
			expected = (Integer[][]) relationships.get(PourpointEngine.OutputType.SUBCATCHMENT_RELATIONSHIP.key);
			for (int i = 0; i < expected.length; i ++) {
				for (int j = 0; j < expected.length; j ++) {
					Assert.assertEquals("file: " + file + " - non overlapping catchment relationship incorrect", expected[i][j], actual[i][j]);
				}
			}
			
			Double[][] dactual = out.getProjectedPourpointMinDistanceMatrix();
			Double[][] dexpected = (Double[][]) relationships.get(PourpointEngine.OutputType.DISTANCE_MIN.key);
			for (int i = 0; i < expected.length; i ++) {
				for (int j = 0; j < expected.length; j ++) {
					Assert.assertEquals("file: " + file + " - pourpoint distances incorrect", dexpected[i][j], dactual[i][j]);
				}
			}
			
			dactual = out.getProjectedPourpointMaxDistanceMatrix();
			dexpected = (Double[][]) relationships.get(PourpointEngine.OutputType.DISTANCE_MAX.key);
			for (int i = 0; i < expected.length; i ++) {
				for (int j = 0; j < expected.length; j ++) {
					Assert.assertEquals("file: " + file + " - pourpoint distances incorrect", dexpected[i][j], dactual[i][j]);
				}
			}
			
		}
	}
	
	
}
