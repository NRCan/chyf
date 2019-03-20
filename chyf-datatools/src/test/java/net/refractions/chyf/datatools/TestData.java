package net.refractions.chyf.datatools;

import java.nio.file.Path;
import java.nio.file.Paths;

import net.refractions.chyf.datatools.processor.SEAProcessor;
import net.refractions.chyf.datatools.processor.SEAResult;
import net.refractions.chyf.datatools.readers.ChyfDataSource;
import net.refractions.chyf.datatools.readers.ChyfShapeDataSource;
import net.refractions.chyf.datatools.readers.GeoTiffDemReader;
import net.refractions.chyf.datatools.writer.ChyfShapeDataSourceSEAWriter;

public class TestData {

	public static void main(String[] args) throws Exception {
		
		Path dem = Paths.get("C:\\data\\CHyF\\github\\chyf-datatools\\src\\test\\resources\\testdata\\small\\test_elevations.tif");
		String datasource ="C:\\data\\CHyF\\github\\chyf-datatools\\src\\test\\resources\\testdata\\small\\";
		
		String outdatasource ="C:\\data\\CHyF\\github\\chyf-datatools\\src\\test\\resources\\testdata\\small\\Catchment.2.shp";
		
		try(ChyfDataSource dataSource = new ChyfShapeDataSource(Paths.get(datasource));
				GeoTiffDemReader demReader = new GeoTiffDemReader(dem)){
			SEAProcessor engine = new SEAProcessor(dataSource, demReader);
			SEAResult result = engine.doWork();
			
			ChyfShapeDataSourceSEAWriter writer = new ChyfShapeDataSourceSEAWriter(dataSource, Paths.get(outdatasource));
			writer.write(result);
		}
				
		    
		  
		
//		GeometryFactory gf = new GeometryFactory();
//		
//		for (int i = 0; i < 100*10; i +=100) {
//			for (int j = 0; j < 100*10; j +=100) {
//				Coordinate c1 = new Coordinate(i,j);
//				Coordinate c2 = new Coordinate(i,j+100);
//				Coordinate c3 = new Coordinate(i+100,j+100);
//				Coordinate c4 = new Coordinate(i+100,j);
//				
////				Polygon p = gf.createPolygon(new Coordinate[] {c1,c2,c3,c4,c1});
//				System.out.println("POINT(" + (c1.x + 50) + " " + (c1.y + 50) + ")");
//			}
//			
//		}
	}
}
