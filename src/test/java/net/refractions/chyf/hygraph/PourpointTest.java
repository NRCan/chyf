package net.refractions.chyf.hygraph;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

/**
 * A simple pourpoint test, that test all aspects
 * of the pourpoint processing.  The expected results are
 * stotred in the pourpoint1.json file in the results test folder
 * 
 * @author Emily
 *
 */
public class PourpointTest {

	@Rule
	public TestRule rule = BasicTestSuite.SETUP_RULE;
	
	public static final String TEST1_RESULTS = BasicTestSuite.RESULTS_DIR + "/pourpoint1.json";

	
	@Test
	public void test_PourpointService() throws ParseException, IOException {
		Pourpoint p1 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32492, 45.43535)), ChyfDatastore.BASE_SRS), -2, "P1");
		Pourpoint p2 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32653, 45.43866)), ChyfDatastore.BASE_SRS), -2, "P2");
		Pourpoint p3 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.33106, 45.43065)), ChyfDatastore.BASE_SRS), -2, "P3");
		Pourpoint p4 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32812, 45.41484)), ChyfDatastore.BASE_SRS), -2, "P4");
		
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
		Integer[][] actual = out.getPartitionedCatchmentRelationship();
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
		Double[][] actual1 = out.getProjectedPourpointMinDistanceMatrix();
		Double[][] actual2 = out.getProjectedPourpointMaxDistanceMatrix();
		for (int i = 0; i < distances.length; i ++) {
			for (int j = 0; j < distances.length; j ++) {
				Assert.assertEquals("Invalid PP Distance", distances[i][j], actual1[i][j]);
				Assert.assertEquals("Invalid PP Distance", distances[i][j], actual2[i][j]);
			}
		}
		
		
		HashMap<String, Object> typeidgeom = new HashMap<>();
		try(JsonReader reader = new JsonReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(TEST1_RESULTS)))){
			reader.beginObject();
			while(reader.hasNext()) {
				String key = reader.nextName();
				if (key.equals( "tcc")) {
					ArrayList<String> tccs = new ArrayList<>();
					typeidgeom.put(key,  tccs);
					reader.beginArray();
					while(reader.hasNext()) {
						String wkt = reader.nextString();
						tccs.add(wkt);
					}
					reader.endArray();
				}else {
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
			}
			reader.endObject();
		}
		WKTReader reader = new WKTReader(BasicTestSuite.GF);

		//upstream catchments
		for (Pourpoint p : out.getPoints()) {
			String wkt = ((HashMap<String,String>)typeidgeom.get(PourpointEngine.OutputType.CATCHMENTS.key)).get(p.getId());
			Geometry g = reader.read(wkt);
			Geometry a = GeotoolsGeometryReprojector.reproject(out.getCatchment(p).getGeometry(), BasicTestSuite.TEST_DATA_SRID);
			if (!g.equalsExact(a, 0.00001)) {
				Assert.fail("catchment incorrect for pp: " + p.getId());
			}
		}
		
		//upstream unique catchments
		for (Pourpoint p : out.getPoints()) {
			String wkt = ((HashMap<String,String>)typeidgeom.get(PourpointEngine.OutputType.PARTITIONED_CATCHMENTS.key)).get(p.getId());
			Geometry g = reader.read(wkt);
			Geometry a = GeotoolsGeometryReprojector.reproject(out.getPartitionedCatchments(p).getGeometry(), BasicTestSuite.TEST_DATA_SRID);
			if (!g.equalsExact(a, 0.00001)) {
				System.out.println(g.toText());
				System.out.println(a.toText());
				Assert.fail("non overlapping catchments incorrect for pp: " + p.getId());
			}
		}
		
		//upstream unique sub catchments
		HashMap<String, Integer> indexToId = new HashMap<>();
		ArrayList<String> expectedMergedCoverages = (ArrayList<String>) typeidgeom.get(PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS.key);
		
		for (Pourpoint point : out.getPoints()) {
			for (UniqueSubCatchment pcat : point.getTraversalCompliantCatchments()) {
				for (int i = 0; i < expectedMergedCoverages.size();i ++) {
					Geometry expectedp = GeotoolsGeometryReprojector.reproject(reader.read(expectedMergedCoverages.get(i)), ChyfDatastore.BASE_SRS);
					if (pcat.getDrainageArea().getGeometry().equalsExact(expectedp, 0.0001)) {
						indexToId.put(pcat.getId(), i);
					}
				}
			}
		}
		for (int i = 0; i < expectedMergedCoverages.size(); i++) {
			if (!indexToId.containsValue(i)) {
				Assert.fail("Traversal compliant coverages do not match");
			}
		}

		Integer[][] expectedCatRel = new Integer[][] {
			{null, null,    1, null, null, null},
			{null, null,    1, null, null, null},
			{  -1,   -1, null,   -1,   -1,   -1},
			{null, null,    1, null,   -1,   -1},
			{null, null,    1,    1, null, null},
			{null, null,    1,    1, null, null},
		};
		Integer[][] catRel = out.getTraversalCompliantCatchmentRelationship();
		for (int i = 0; i < expectedCatRel.length; i ++) {
			for (int j = 0; j < expectedCatRel.length; j ++) {
				String id1 = out.getTraversalCompliantCatchments().get(i).getId();
				String id2 = out.getTraversalCompliantCatchments().get(j).getId();
				Integer val = expectedCatRel[indexToId.get(id1)][indexToId.get(id2)];
				Assert.assertEquals("Catchment relationship matrix incorrect (" + out.getTraversalCompliantCatchments().get(i).getId() + " to " + out.getTraversalCompliantCatchments().get(j).getId() + ")", val,catRel[i][j]);
			}
		}
			
	}
	
	
	@Test
	public void test_PourpointServiceBankM2() throws ParseException, IOException {
		Pourpoint p1 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.36910403700568, 45.376500627354744)), ChyfDatastore.BASE_SRS), -2, "P1");
		
		ArrayList<Pourpoint> points = new ArrayList<>();
		points.add(p1);
		
			
		PourpointEngine engine = new PourpointEngine(points, BasicTestSuite.DATASTORE.getHyGraph(), true);
		PourpointOutput out = engine.compute(null);
		
		Coordinate pp1 = new Coordinate(-73.37019030000002, 45.3757165);
	
		// test PP Projections
		for (Pourpoint p : out.getPoints()) {
			Coordinate c = null;
			if (p.getId().equals(p1.getId())) {
				c = pp1;
			}
			Coordinate actual = GeotoolsGeometryReprojector.reproject(p.getProjectedPoint(), BasicTestSuite.TEST_DATA_SRID).getCoordinate();
			Assert.assertTrue("Projected PP Incorrect: " + p.getId(),actual.equals2D(c));
		}
	
		WKTReader reader = new WKTReader(BasicTestSuite.GF);
		String polygon = "POLYGON (( -73.3686485 45.3741461, -73.3686611 45.3742, -73.368712 45.3742811, -73.3687246 45.3743351, -73.3687883 45.3744161, -73.3689285 45.3745153, -73.3690433 45.3745604, -73.3692984 45.3746597, -73.3694387 45.3747049, -73.369579 45.374768, -73.36963 45.374804, -73.3697575 45.3748582, -73.3698086 45.3748943, -73.370051 45.3750025, -73.3701657 45.3750836, -73.3701654 45.3751736, -73.3701653 45.3752276, -73.3701652 45.3753086, -73.3701394 45.3753716, -73.3701137 45.3754255, -73.3700625 45.3755064, -73.3700242 45.3755604, -73.3699856 45.3756323, -73.3699599 45.3756773, -73.3699471 45.3757403, -73.3699214 45.3757853, -73.3698957 45.3758482, -73.3698573 45.3759201, -73.3698188 45.3759831, -73.3697805 45.376028, -73.3697547 45.376082, -73.3697292 45.3761179, -73.3696524 45.3762168, -73.3695883 45.3762888, -73.3695244 45.3763337, -73.3694604 45.3763966, -73.3693325 45.3764955, -73.3692814 45.3765494, -73.3692301 45.3766033, -73.3690637 45.3767831, -73.369038 45.3768281, -73.369038 45.376882, -73.3690251 45.3769361, -73.3690249 45.377008, -73.3690247 45.37708, -73.3690118 45.377125, -73.3688868 45.3770618, -73.3688551 45.3770522, -73.3687967 45.3770257, -73.3687575 45.377012, -73.3687075 45.3769896, -73.36866 45.3769719, -73.3686185 45.3769535, -73.3685627 45.376932, -73.3683598 45.3768423, -73.3682401 45.3768019, -73.3682048 45.376789, -73.3679878 45.3766534, -73.3679704 45.3766287, -73.3678664 45.3764696, -73.3676005 45.3760901, -73.3675871 45.3760716, -73.3675604 45.3760309, -73.3674542 45.3758744, -73.3674247 45.3758361, -73.3673507 45.3756833, -73.367287 45.3756568, -73.3672507 45.3756419, -73.3672393 45.3756373, -73.3672624 45.3755852, -73.3672692 45.3755705, -73.3672993 45.3755025, -73.3673075 45.3754849, -73.3673362 45.3754198, -73.3673736 45.375339, -73.3674117 45.3752608, -73.3675238 45.3750417, -73.3678629 45.3747511, -73.3679575 45.3746646, -73.3679647 45.3746584, -73.3680384 45.3745899, -73.3682457 45.3744104, -73.3683081 45.3743727, -73.3683247 45.374364, -73.3684053 45.374304, -73.3684093 45.3743004, -73.3685134 45.3742224, -73.3686485 45.3741461 ))";
		Geometry g = reader.read(polygon);
		
		//upstream catchments
		for (Pourpoint p : out.getPoints()) {
			Geometry a = GeotoolsGeometryReprojector.reproject(out.getCatchment(p).getGeometry(), BasicTestSuite.TEST_DATA_SRID);
			if (!g.equalsExact(a, 0.00001)) {
				Assert.fail("catchment incorrect for pp: " + p.getId());
			}
		}
		
		//upstream unique catchments
		for (Pourpoint p : out.getPoints()) {
			Geometry a = GeotoolsGeometryReprojector.reproject(out.getPartitionedCatchments(p).getGeometry(), BasicTestSuite.TEST_DATA_SRID);
			if (!g.equalsExact(a, 0.00001)) {
				Assert.fail("unique catchment incorrect for pp: " + p.getId());
			}
		}
		
		//upstream unique sub catchments
		for (Pourpoint p : out.getPoints()) {
			for (UniqueSubCatchment s : out.getTraversalCompliantCatchments(p)) {
				Geometry a = GeotoolsGeometryReprojector.reproject(s.getDrainageArea().getGeometry(), BasicTestSuite.TEST_DATA_SRID);
				if (!g.equalsExact(a, 0.00001)) {
					Assert.fail("unique sub catchment incorrect for pp: " + p.getId());
				}
			}
		}
	}
	
	
}

