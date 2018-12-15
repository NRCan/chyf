package org.refractions.chyf.hygraph;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.google.gson.stream.JsonReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.pourpoint.UniqueSubCatchment;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

public class PourpointTest {

	@Rule
	public TestRule rule = BasicTestSuite.SETUP_RULE;
	
	public static final String TEST1_RESULTS = BasicTestSuite.RESULTS_DIR + "/pourpoint1.json";

	
	@Test
	public void test_PourpointService1() throws ParseException, IOException {
		Pourpoint p1 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32492, 45.43535)), ChyfDatastore.BASE_SRS), -1, "P1");
		Pourpoint p2 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32653, 45.43866)), ChyfDatastore.BASE_SRS), -1, "P2");
		Pourpoint p3 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.33106, 45.43065)), ChyfDatastore.BASE_SRS), -1, "P3");
		Pourpoint p4 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32812, 45.41484)), ChyfDatastore.BASE_SRS), -1, "P4");
		
		ArrayList<Pourpoint> points = new ArrayList<>();
		points.add(p1);
		points.add(p2);
		points.add(p3);
		points.add(p4);
			
		PourpointEngine engine = new PourpointEngine(points, BasicTestSuite.DATASTORE.getHyGraph());
		PourpointOutput out = engine.compute(null);
		
		Coordinate pp1 = new Coordinate(-73.3252036,45.435359200000015);
		Coordinate pp2 = new Coordinate(-73.326548,45.438767199999965);
		Coordinate pp3 = new Coordinate(-73.3311985,45.4309229);
		Coordinate pp4 = new Coordinate(-73.3268128,45.414862899999946);
		
		// test PP Projections
		for (Pourpoint p : out.getPoints()) {
			Coordinate c = null;
			if (p.getId().equals(p1.getId())) {
				c = pp1;
			}else if (p.getId().equals(p2.getId())) {
				c = pp2;
			}else if (p.getId().equals(p3.getId())) {
				c = pp3;
			}else if (p.getId().equals(p4.getId())) {
				c = pp4;
			}
			Coordinate actual = GeotoolsGeometryReprojector.reproject(p.getProjectedPoint(), BasicTestSuite.TEST_DATA_SRID).getCoordinate();
			Assert.assertTrue("Projected PP Incorrect: " + p.getId(),actual.equals2D(c));
		}
		// test PP Relationships
		Integer[][] expected = {
				{null, 1, null, null},
				{-1,null, -1, -1},
				{null, 1, null, -1},
				{null, 1, 1, null},
		};
		Integer[][] actual = out.getPourpointRelationship();
		for (int i = 0; i < expected.length; i ++) {
			for (int j = 0; j < expected.length; j ++) {
				Assert.assertEquals("Invalid PP Relationships", expected[i][j], actual[i][j]);
			}
		}
	
		
		// test PP Distances
		Double[][] distances = {
				{null,493.3206295697041,null,null},
				{-493.3206295697041,null,-1254.2579560971265,-3361.317362294776},
				{null,1254.2579560971265,null,-2107.059406197649},
				{null,3361.317362294776,2107.059406197649,null}
		};
		Double[][] actual1 = out.getPourpointMinDistanceMatrix();
		Double[][] actual2 = out.getPourpointMaxDistanceMatrix();
		for (int i = 0; i < distances.length; i ++) {
			for (int j = 0; j < distances.length; j ++) {
				Assert.assertEquals("Invalid PP Distance", distances[i][j], actual1[i][j]);
				Assert.assertEquals("Invalid PP Distance", distances[i][j], actual2[i][j]);
			}
		}
		
		
		HashMap<String, HashMap<String,String>> typeidgeom = new HashMap<>();
		try(JsonReader reader = new JsonReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(TEST1_RESULTS)))){
			reader.beginObject();
			while(reader.hasNext()) {
				String key = reader.nextName();
				HashMap<String, String> idgeom = new HashMap<>();
				typeidgeom.put(key,  idgeom);
				
				reader.beginObject();
				while(reader.hasNext()) {
					String id = reader.nextName();
					String wkt = reader.nextString();
					idgeom.put(id, wkt);
				}
				reader.endObject();
			}
			reader.endObject();
		}
		WKTReader reader = new WKTReader(BasicTestSuite.GF);

		//upstream catchments
		for (Pourpoint p : out.getPoints()) {
			String wkt = typeidgeom.get("pc").get(p.getId());
			Geometry g = reader.read(wkt);
			Geometry a = GeotoolsGeometryReprojector.reproject(out.getCatchment(p).getGeometry(), BasicTestSuite.TEST_DATA_SRID);
			if (!g.equalsExact(a, 0.00001)) {
				Assert.fail("catchment incorrect for pp: " + p.getId());
			}
		}
		
		//upstream unique catchments
		for (Pourpoint p : out.getPoints()) {
			String wkt = typeidgeom.get("puc").get(p.getId());
			Geometry g = reader.read(wkt);
			Geometry a = GeotoolsGeometryReprojector.reproject(out.getUniqueCatchment(p).getGeometry(), BasicTestSuite.TEST_DATA_SRID);
			if (!g.equalsExact(a, 0.00001)) {
				Assert.fail("unique catchment incorrect for pp: " + p.getId());
			}
		}
		
		//upstream unique sub catchments
		for (Pourpoint p : out.getPoints()) {
			for (UniqueSubCatchment s : out.getUniqueSubCatchments(p)) {
				String wkt = typeidgeom.get("pusc").get(s.getId());
				Geometry g = reader.read(wkt);
				Geometry a = GeotoolsGeometryReprojector.reproject(s.getDrainageArea().getGeometry(), BasicTestSuite.TEST_DATA_SRID);
				if (!g.equalsExact(a, 0.00001)) {
					Assert.fail("unique sub catchment incorrect for pp: " + p.getId());
				}
			}
		}
		
		// test PP Relationships
		
		
		HashMap<String, HashMap<String, Integer>> cexpected = new HashMap<>();
		
		HashMap<String, Integer> row = new HashMap<>();
		cexpected.put("P1", row);
		row.put("P2_P1_P3", 1);
		
		row = new HashMap<>();
		cexpected.put("P2", row);
		row.put("P2_P1_P3", 1);
		
		row = new HashMap<>();
		cexpected.put("P3", row);
		row.put("P3_P4", 1);
		row.put("P2_P1_P3", 1);
		
		row = new HashMap<>();
		cexpected.put("P3_P4", row);
		row.put("P3", -1);
		row.put("P4", -1);
		row.put("P2_P1_P3", 1);
		
		row = new HashMap<>();
		cexpected.put("P4", row);
		row.put("P3_P4", 1);
		row.put("P2_P1_P3", 1);
		
		row = new HashMap<>();
		cexpected.put("P2_P1_P3", row);
		row.put("P2", -1);
		row.put("P3_P4", -1);
		row.put("P4", -1);
		row.put("P1", -1);
		row.put("P3", -1);
		
		for (int i = 0; i < out.getUniqueSubCatchments().size(); i ++) {
			for (int j = 0; j < out.getUniqueSubCatchments().size(); j ++) {
				Assert.assertEquals("Invalid Unique SubCatchments Relationships", cexpected.get(out.getUniqueSubCatchments().get(i).getId()).get(out.getUniqueSubCatchments().get(j).getId()), out.getPourpointCatchmentRelationship()[i][j]);
			}
			System.out.println();
		}
				
	}
	
	
	
}

