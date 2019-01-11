package org.refractions.chyf.hygraph;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.hygraph.HyGraphBuilder;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.pourpoint.UniqueSubCatchment;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;
import net.refractions.chyf.rest.messageconverters.PourpointJsonConverter;

public class TestData {
	
	public static void main(String[] args) throws Exception{
		
		GeometryFactory temp = new GeometryFactory(new PrecisionModel(), ChyfDatastore.BASE_SRS);
		
		Point p = temp.createPoint(new Coordinate(-367805.77835080196, 173960.1858683691));
		System.out.println(GeotoolsGeometryReprojector.reproject(p, BasicTestSuite.TEST_DATA_SRID).toText());
//		
//		
//		Coordinate[][] testPourpoints = new Coordinate[][] {
//			{new Coordinate(-73.12938, 45.49113), new Coordinate(-73.14689, 45.46687)},
//			{new Coordinate(-73.19585, 45.48135), new Coordinate(-73.19890, 45.47975), new Coordinate(-73.19770, 45.47903), new Coordinate(-73.19915, 45.47917)},
//			{new Coordinate(-73.21821, 45.53076), new Coordinate(-73.22057, 45.54014), new Coordinate(-73.22628, 45.52174)}
//		};
//		
//		int file = 1;
//		Path datapath = Paths.get("C:\\data\\CHyF\\github\\chyf-pilot\\src\\test\\resources\\data");
//		ChyfDatastore ds = new ChyfDatastore(datapath.toString() + "/");
//		
//		for (Coordinate[] test : testPourpoints) {
//			
//			List<Pourpoint> points = new ArrayList<>();
//			for (int i = 0; i < test.length; i ++) {
//				points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(test[i]), ChyfDatastore.BASE_SRS), -1, "P" + (i+1)));
//			}
//			File outfile = new File("C:\\data\\CHyF\\github\\chyf-pilot\\src\\test\\resources\\results\\pourpoint_secondary_" + file + ".json");
//			file++;
//			try(FileWriter writer = new FileWriter(outfile)){
//				PourpointJsonConverter c = new PourpointJsonConverter(writer);
//				
//				PourpointEngine eng = new PourpointEngine(points,ds.getHyGraph(), true);
//				PourpointOutput out = eng.compute(null);
//				
////				ApiResponse rr = new ApiResponse(out);
////				rr.setSrs(BasicTestSuite.TEST_DATA_SRID);
////				c.convertResponse(rr);
//				
//				
//				
//				StringBuilder sb = new StringBuilder();
//				sb.append("{\"" + PourpointEngine.OutputType.OUTPUT_PP.key + "\": {");
//				sb.append("\n");
//				for (Pourpoint point : out.getPoints()) {
//					sb.append("\"" + point.getId() + "\": \"");
//					sb.append(GeotoolsGeometryReprojector.reproject(point.getProjectedPoint(), BasicTestSuite.TEST_DATA_SRID).toText());
//					sb.append("\",");
//					sb.append("\n");
//				}
//				sb.deleteCharAt(sb.length() - 1);
//				sb.deleteCharAt(sb.length() - 1);
//				sb.append("},");
//				
//				sb.append("\n");
//				
//				sb.append("\"" + PourpointEngine.OutputType.CATCHMENTS.key +"\": {");
//				sb.append("\n");
//				for (Pourpoint point : points) {
//					sb.append("\"" + point.getId() + "\": \"");
//					sb.append(GeotoolsGeometryReprojector.reproject(out.getCatchment(point).getGeometry(), BasicTestSuite.TEST_DATA_SRID).toText());
//					sb.append("\",");
//					sb.append("\n");
//				}
//				sb.deleteCharAt(sb.length() - 1);
//				sb.deleteCharAt(sb.length() - 1);
//				sb.append("},");
//				sb.append("\n");
//				
//				sb.append("\"" + PourpointEngine.OutputType.NONOVERLAPPING_CATCHMENTS.key +"\": {");
//				sb.append("\n");
//				for (Pourpoint point : points) {
//					sb.append("\"" + point.getId() + "\": \"");
//					sb.append(GeotoolsGeometryReprojector.reproject(out.getNonOverlappingCatchments(point).getGeometry(), BasicTestSuite.TEST_DATA_SRID).toText());
//					sb.append("\",");
//					sb.append("\n");
//				}
//				sb.deleteCharAt(sb.length() - 1);
//				sb.deleteCharAt(sb.length() - 1);
//				sb.append("},");
//				sb.append("\n");
//				
//				sb.append("\"" + PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS.key +"\": {");
//				sb.append("\n");
//				for (Pourpoint point : points) {
//					for (UniqueSubCatchment a : out.getTraversalCompliantCatchments(point)) {
//						sb.append("\"" + a.getId() + "\": \"");
//						sb.append(GeotoolsGeometryReprojector.reproject(a.getDrainageArea().getGeometry(), BasicTestSuite.TEST_DATA_SRID).toText());
//						sb.append("\",\n");
//					}
//				}
//				sb.deleteCharAt(sb.length() - 1);
//				sb.deleteCharAt(sb.length() - 1);
//				sb.append("},");
//				sb.append("\n");
//				
//				sb.append("\"" + PourpointEngine.OutputType.CATCHMENT_CONTAINMENT.key +"\": [");
//				sb.append("\n");
//				Integer[][] rel = out.getCatchmentContainment();
//					
//				for (int i = 0; i < rel.length; i ++) {
//					sb.append("[");
//					for (int j = 0; j < rel.length; j ++) {
//						sb.append(rel[i][j]);
//						sb.append(",");
//					}
//					sb.deleteCharAt(sb.length() - 1);
//					sb.append("],");
//				}
//				sb.deleteCharAt(sb.length() - 1);
//
//				sb.append("]");
//				sb.append(",\n");
//				
//				sb.append("\"" + PourpointEngine.OutputType.NONOVERLAPPINGCATCHMENT_RELATIONSHIP.key +"\": [");
//				sb.append("\n");
//				rel = out.getNonOverlappingCatchmentRelationship();
//					
//				for (int i = 0; i < rel.length; i ++) {
//					sb.append("[");
//					for (int j = 0; j < rel.length; j ++) {
//						sb.append(rel[i][j]);
//						sb.append(",");
//
//					}
//					sb.deleteCharAt(sb.length() - 1);
//
//					sb.append("],");
//				}
//				sb.deleteCharAt(sb.length() - 1);
//
//				sb.append("]");
//				sb.append(",\n");
//				
//				sb.append("\"" + PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION.key +"\": [");
//				sb.append("\n");
//				rel = out.getTraversalCompliantCatchmentRelationship();
//					
//				for (int i = 0; i < rel.length; i ++) {
//					sb.append("[");
//					for (int j = 0; j < rel.length; j ++) {
//						sb.append(rel[i][j]);
//						sb.append(",");
//					}
//					sb.deleteCharAt(sb.length() - 1);
//					sb.append("],");
//				}
//				sb.deleteCharAt(sb.length() - 1);
//
//				sb.append("]");
//				sb.append(",\n");
//				
//				sb.append("\"" + PourpointEngine.OutputType.DISTANCE_MIN.key +"\": [");
//				sb.append("\n");
//				Double[][] dis = out.getProjectedPourpointMinDistanceMatrix();
//					
//				for (int i = 0; i < dis.length; i ++) {
//					sb.append("[");
//					for (int j = 0; j < dis.length; j ++) {
//						sb.append(dis[i][j]);
//						sb.append(",");
//					}
//					sb.deleteCharAt(sb.length() - 1);
//
//					sb.append("],");
//				}
//				sb.deleteCharAt(sb.length() - 1);
//
//				sb.append("]");
//				sb.append(",\n");
//				
//				sb.append("\"" + PourpointEngine.OutputType.DISTANCE_MAX.key +"\": [");
//				sb.append("\n");
//				dis = out.getProjectedPourpointMaxDistanceMatrix();
//					
//				for (int i = 0; i < dis.length; i ++) {
//					sb.append("[");
//					for (int j = 0; j < dis.length; j ++) {
//						sb.append(dis[i][j]);
//						sb.append(",");
//
//					}
//					sb.deleteCharAt(sb.length() - 1);
//
//					sb.append("],");
//				}
//				sb.deleteCharAt(sb.length() - 1);
//
//				sb.append("]");
//				sb.append("\n");
//				sb.append("}");
//				
//				writer.write(sb.toString());
//			}
//		}
	}
}

//	public static void main(String[] args) throws IOException {
//		String path = "C:\\data\\CHyF\\github\\chyf-pilot\\src\\test\\resources\\data\\";
//		
////		String path = "C:\\data\\CHyF\\github\\chyf-pilot\\target\\test-classes\\data\\";
//		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4617);
//		ChyfDatastore datastore = new ChyfDatastore(path);	
//		
//		
////		Pourpoint p1 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32492, 45.43535)), ChyfDatastore.BASE_SRS), -1, "P1");
////		Pourpoint p2 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32653, 45.43866)), ChyfDatastore.BASE_SRS), -1, "P2");
////		Pourpoint p3 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.33106, 45.43065)), ChyfDatastore.BASE_SRS), -1, "P3");
////		Pourpoint p4 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32812, 45.41484)), ChyfDatastore.BASE_SRS), -1, "P4");
////		
//		Pourpoint p1 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32492, 45.43535)), ChyfDatastore.BASE_SRS), -1, "P1");
//		Pourpoint p2 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32653, 45.43866)), ChyfDatastore.BASE_SRS), -1, "P2");
//		Pourpoint p3 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.33106, 45.43065)), ChyfDatastore.BASE_SRS), -1, "P3");
//		Pourpoint p4 = new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate(-73.32812, 45.41484)), ChyfDatastore.BASE_SRS), -1, "P4");
//		
//		ArrayList<Pourpoint> points = new ArrayList<>();
//		points.add(p1);
//		points.add(p2);
//		points.add(p3);
//		points.add(p4);
//		PourpointEngine engine = new PourpointEngine(points, datastore.getHyGraph());
//		PourpointOutput out = engine.compute(null);
//		
//		
//		
////		File outfile = new File("C:\\data\\CHyF\\github\\chyf-pilot\\src\\test\\resources\\results\\pourpoint1.json");
////		PourpointJsonConverter c = new PourpointJsonConverter(new FileWriter(outfile));
////		ApiResponse rr = new ApiResponse(out);
////		c.convertResponse(rr);
//		
////		
//		StringBuilder sb = new StringBuilder();
//		sb.append("{\"pc\": {");
//		sb.append("\n");
//		for (Pourpoint point : points) {
//			sb.append("\"" + point.getId() + "\": \"");
//			sb.append(GeotoolsGeometryReprojector.reproject(out.getCatchment(point).getGeometry(), BasicTestSuite.TEST_DATA_SRID).toText());
//			sb.append("\",");
//			sb.append("\n");
//		}
//		sb.append("},");
//		sb.append("\n");
//		
//		sb.append("\"puc\": {");
//		sb.append("\n");
//		for (Pourpoint point : points) {
//			sb.append("\"" + point.getId() + "\": \"");
//			sb.append(GeotoolsGeometryReprojector.reproject(out.getUniqueCatchment(point).getGeometry(), BasicTestSuite.TEST_DATA_SRID).toText());
//			sb.append("\",");
//			sb.append("\n");
//		}
//		sb.append("},");
//		sb.append("\n");
//		
//		sb.append("\"pusc\": {");
//		sb.append("\n");
//		for (Pourpoint point : points) {
//			Collection<UniqueSubCatchment> items = out.getUniqueSubCatchments(point);
//			for (UniqueSubCatchment i : items) {
//				sb.append("\"" + i.getId() + "\": ");
//				sb.append("\"");
//				sb.append(GeotoolsGeometryReprojector.reproject(i.getDrainageArea().getGeometry(), BasicTestSuite.TEST_DATA_SRID).toText());
//				sb.append("\",");
//				sb.append("\n");
//			}
//		}
//		sb.append("}");
//		sb.append("}");
//		
////		Paths.get("C:\\data\\CHyF\\github\\chyf-pilot\\src\\test\\resources\\results\\pourpoint1.json")
////		try(BufferedWriter w = Files.newBufferedWriter(Paths.get("C:\\data\\CHyF\\github\\chyf-pilot\\src\\test\\resources\\results\\pourpoint1.json"))){
////			w.write(sb.toString());
////		}
//		
//		Integer[][] dd = out.getPourpointCatchmentRelationship();
//		out.getUniqueSubCatchments().forEach(e->System.out.println(e.getId()));
//		for (int i = 0; i < dd.length; i ++) {
//			for (int j = 0; j < dd.length; j ++) {
//				System.out.print(dd[i][j]);
//			}
//			System.out.println();
//			
//		}
////		
////		System.out.println("----");
////		
////		for (Pourpoint point : points) {
////			System.out.println(GeotoolsGeometryReprojector.reproject(out.getUniqueCatchment(point).getGeometry(), BasicTestSuite.TEST_DATA_SRID).toText());
////		}
////		
////		System.out.println("----");
////		
////		Integer[][] matrix = engine.getPourpointRelationshipMatrix();
////		
////		StringBuilder sb = new StringBuilder();
////		sb.append("   ");
////		for (Pourpoint pp : engine.getPoints()) {
////			sb.append(" ");
////			sb.append(pp.getId());
////			sb.append("  ");
////		}
////		System.out.println(sb.toString());
////		
////		for (int i = 0; i < matrix.length; i ++) {
////			sb = new StringBuilder();
////			sb.append( engine.getPoints().get(i).getId() );
////			sb.append(" ");
////			for (int j = 0; j < matrix.length; j ++) {
////				sb.append(" ");
////				if (matrix[i][j] == null) {
////					sb.append("--");
////				}else if (matrix[i][j] > 0) {
////					sb.append(" ");
////					sb.append(matrix[i][j]);
////				}else {
////					sb.append(matrix[i][j]);
////				}
////				sb.append(" ");
////			}
////			System.out.println(sb.toString());
////		}
//		
//	
////		Path datapath = Paths.get("C:\\data\\CHyF\\github\\chyf-pilot\\data\\data_small2");
////		ChyfDatastore datastore = new ChyfDatastore(datapath.toString() + "/");
////		
////		List<Pourpoint> points = new ArrayList<>();
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.46978918017754, 45.11160935985887)), ChyfDatastore.BASE_SRS), -1, "A"));
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.46729944386514, 45.10864108733049)), ChyfDatastore.BASE_SRS), -1, "B"));
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.46220404415703, 45.101901140626644)), ChyfDatastore.BASE_SRS), -1, "C"));
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.47479426459982, 45.1126850553528)), ChyfDatastore.BASE_SRS), -1, "D"));
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.46357899328461, 45.116513345080584)), ChyfDatastore.BASE_SRS), -1, "E"));
//		 
//		
////		List<Pourpoint> points = new ArrayList<>();
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.4319822367642, 45.15971823621682)), ChyfDatastore.BASE_SRS), -1, "A"));
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.4258291074446, 45.15196360748522)), ChyfDatastore.BASE_SRS), 0, "B"));
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.44395133763257, 45.15044639751599)), ChyfDatastore.BASE_SRS), -1, "C"));
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.43223510509243, 45.12574959079471)), ChyfDatastore.BASE_SRS), -1, "D"));
////		points.add(new Pourpoint(GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(new Coordinate( -73.4122585071643, 45.16072970952963)), ChyfDatastore.BASE_SRS), -1, "E"));
////		
////		
////		PourpointEngine engine = new PourpointEngine(points, datastore.getHyGraph());
////		engine.compute(null);
////		
////		Integer[][] matrix = engine.getPourpointRelationshipMatrix();
////		
////		StringBuilder sb = new StringBuilder();
////		sb.append("   ");
////		for (Pourpoint pp : engine.getPoints()) {
////			sb.append(" ");
////			sb.append(pp.getId());
////			sb.append("  ");
////		}
////		System.out.println(sb.toString());
////		
////		for (int i = 0; i < matrix.length; i ++) {
////			sb = new StringBuilder();
////			sb.append( engine.getPoints().get(i).getId() );
////			sb.append(" ");
////			for (int j = 0; j < matrix.length; j ++) {
////				sb.append(" ");
////				if (matrix[i][j] == null) {
////					sb.append("--");
////				}else if (matrix[i][j] > 0) {
////					sb.append(" ");
////					sb.append(matrix[i][j]);
////				}else {
////					sb.append(matrix[i][j]);
////				}
////				sb.append(" ");
////			}
////			System.out.println(sb.toString());
////		}
////		
////		Double[][] min = engine.getPourpointMinDistanceMatrix();
////		Double[][] max = engine.getPourpointMaxDistanceMatrix();
////		
////		sb = new StringBuilder();
////		sb.append("   ");
////		for (Pourpoint pp : engine.getPoints()) {
////			sb.append(" ");
////			sb.append(pp.getId());
////			sb.append("  ");
////		}
////		System.out.println(sb.toString());
////		
////		for (int i = 0; i < min.length; i ++) {
////			sb = new StringBuilder();
////			sb.append( engine.getPoints().get(i).getId() );
////			sb.append(" ");
////			for (int j = 0; j < min.length; j ++) {
////				sb.append(" ");
////				if (min[i][j] == null) {
////					sb.append("--");
////				}else if (min[i][j] > 0) {
////					sb.append(" ");
////					sb.append(min[i][j]);
////				}else {
////					sb.append(min[i][j]);
////				}
////				sb.append(" to ");
////				if (max[i][j] == null) {
////					sb.append("--");
////				}else if (max[i][j] > 0) {
////					sb.append(" ");
////					sb.append(max[i][j]);
////				}else {
////					sb.append(max[i][j]);
////				}
////				sb.append(" ");
////			}
////			System.out.println(sb.toString());
////		}
//		
////		// TODO Auto-generated method stub
////		TestData td = new TestData();
////		td.generateMultiDimensional();
//		
////		Coordinate outFlow = new Coordinate(3,3);
////		Coordinate nexus = new Coordinate(1, 2);
////		
////		Coordinate in1 = new Coordinate(2, 0);
////		Coordinate in2 = new Coordinate(0,1);
////		Coordinate in3 = new Coordinate(.25,3);
////		
////		System.out.println(Angle.toDegrees( Angle.angleBetweenOriented(outFlow, nexus, in1)) );
////		
////		System.out.println(Angle.toDegrees(Angle.angleBetweenOriented(outFlow, nexus, in2)));
////		System.out.println(Angle.toDegrees(Angle.angleBetweenOriented(outFlow, nexus, in3)));
////		System.out.println(Angle.toDegrees(Angle.angleBetweenOriented(outFlow, nexus, in3) - 2*Math.PI));
//	}
//	
//	
//	public void generateMultiDimensional() throws IOException {
//		String path = "C:\\data\\CHyF\\github\\chyf-pilot\\src\\test\\resources\\data\\";
//		
//		String outpath = "C:\\data\\CHyF\\github\\chyf-pilot\\src\\test\\resources\\results\\multidimensionalupstream_results.json";
//		ChyfDatastore datastore = new ChyfDatastore(path);
//		
//		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4617);
//		
//		Coordinate[] points = new Coordinate[] {
//				new Coordinate(-73.208567, 45.275963),
//				new Coordinate(-73.20165, 45.458269),
//				new Coordinate(-73.20118, 45.460096),
//				new Coordinate(-73.310721, 45.450997),
//				new Coordinate(-73.310986, 45.450286)
//		};
//		
//		HashMap<Point, List<Geometry>> results = new HashMap<>();
//		
//		for (Coordinate c : points) {
//			System.out.println(c);		
//			Point pnt = gf.createPoint(c);
//
//			pnt = GeotoolsGeometryReprojector.reproject(pnt, ChyfDatastore.BASE_SRS);
//			Collection<SpatiallyIndexable> geoms = datastore.getHyGraph().getUpstreamMultiDimensional(datastore.getHyGraph().getECatchment(pnt), ChyfDatastore.MAX_RESULTS);
//		
//			List<Geometry> ii = new ArrayList<>();
//			for (SpatiallyIndexable i : geoms) {
//				Geometry g = null;
//				if (i instanceof EFlowpath) {
//					g = ((EFlowpath) i).getLineString();
//				}else if (i instanceof ECatchment) {
//					g = ((ECatchment) i).getPolygon();
//				}
//				g = GeotoolsGeometryReprojector.reproject(g, 4617);
//				ii.add(g);
//			}
//			
//			results.put(gf.createPoint(c), ii);
//			
//		}
//		
//		//write json
//		Path p = Paths.get(outpath);
//		try(BufferedWriter writer = Files.newBufferedWriter(p)){
//			writer.write("[\n");
//		
//			for (Entry<Point, List<Geometry>> item : results.entrySet()) {
//				writer.write("{\n");
//				
//				writer.write("\"point\": [" + item.getKey().getX() + ", " + item.getKey().getY() + "],\n");
//				writer.write("\"results\": ");
//				
//				writer.write("[\n");
//				for (Geometry g : item.getValue()) {
//					writer.write("\"" + g.toText() + "\",\n");
//				}
//				writer.write("]\n");
//				writer.write("},\n");
//			}
//			writer.write("]\n");
//		}
//	}
//
//}
