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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.locationtech.jts.geom.Coordinate;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.ChyfShapeDataReader;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

/**
 * Tests specific to the pourpoint relationship tree.  This uses
 * a different set of small test data identified in the small_data2
 * directory
 * 
 * @author Emily
 *
 */
public class PointRelationshipTreeTest {

	private static ChyfDatastore datastore; 
	
	
	private static Coordinate[] c = {
			null,
			new Coordinate(-73.46219281751237, 45.101525614055305),
			new Coordinate(-73.47965054788692, 45.11306791512938),
			new Coordinate(-73.4779192027258, 45.10989378233401),
			new Coordinate(-73.46599215828259, 45.11585730455562),
			new Coordinate(-73.46411653435806, 45.10609444156379),
			new Coordinate(-73.46498220693861, 45.1048440256141), 
			new Coordinate(-73.46195235290666, 45.1237445436229),
			new Coordinate(-73.46710554909613, 45.10834327170833),
	};
	private static String[] ids = {
		null,
		"P1",
		"P2",
		"P3",
		"P4",
		"P5",
		"P6",
		"P7",
		"P8",
	};
	
	@BeforeClass 
	public static void startup() throws URISyntaxException{
		URL url = ClassLoader.getSystemResource("data_small2/" + ChyfShapeDataReader.FLOWPATH_FILE);
		Path datapath = Paths.get(url.toURI()).getParent();
		datastore = new ChyfDatastore(datapath.toString() + "/");
	}
	
	
	@Test
	public void test_PointRelationshipTree() {
		
		List<TestData> tests = new ArrayList<>();
		
		TestData tdata = new TestData();
		
		tdata = new TestData();
		tdata.index = new int[]{1,2,3,4,5,7};
		tdata.results = "P1(x1(P5,x2(x3(P3,P2),P4(P7))))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{1,2,3};
		tdata.results = "P1(x1(P3,P2))";
		tests.add(tdata);

		tdata = new TestData();
		tdata.index = new int[]{1};
		tdata.results = "P1";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{7,4,6,1};
		tdata.results = "P1(P6(P4(P7)))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{6,1,5, 7};
		tdata.results = "P1(x1(P5,P6(P7)))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{7,4,6,1,5};
		tdata.results = "P1(x1(P5,P6(P4(P7))))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{7,4,6,1,5};
		tdata.results = "P1(x1(P5,P6(P4(P7))))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{1,5,6,7};
		tdata.results = "P1(x1(P5,P6(P7)))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{1,5,8,7};
		tdata.results = "P1(x1(P5,P8(P7)))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{1,2,3};
		tdata.results = "P1(x1(P3,P2))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{1,2,3,4};
		tdata.results = "P1(x1(x2(P3,P2),P4))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{1,2,3,4,5,6,7};
		tdata.results = "P1(x1(P5,P6(x2(x3(P3,P2),P4(P7)))))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{1,2,3,4,5,7};
		tdata.results = "P1(x1(P5,x2(x3(P3,P2),P4(P7))))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{2,3,4,7};
		tdata.results = "x1(x2(P3,P2),P4(P7))";
		tests.add(tdata);
		
		tdata = new TestData();
		tdata.index = new int[]{4,7,3,2};
		tdata.results = "x1(x2(P3,P2),P4(P7))";
		tests.add(tdata);
		
		for (TestData t : tests) {
			List<Pourpoint> points = new ArrayList<>();
			for (int i : t.index) {
				points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(c[i]),  BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS), -2, ids[i]));
			}
			PourpointOutput results = ( new PourpointEngine(points, datastore.getHyGraph(), true) ).compute(Collections.singleton( PourpointEngine.OutputType.PRT) );
			System.out.println(results.getPointRelationshipTree() );
			System.out.println(t.results);
			
			Assert.assertEquals("prt incorrect", t.results, results.getPointRelationshipTree());
		}
		

		
	}
	
	private class TestData{
		int index[];
		String results;
	}
}
