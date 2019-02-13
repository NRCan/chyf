package net.refractions.chyf.datatools;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.refractions.chyf.datatools.processor.Distance2DProcessor;
import net.refractions.chyf.datatools.readers.ChyfShapeDataSource;

public class TestDistance2DComputation {

	private static ChyfShapeDataSource chyfData;
	
	@BeforeClass 
	public static void startup() throws Exception{
		URL urlc = ClassLoader.getSystemResource("testdata/small_distance2d/" + ChyfShapeDataSource.CATCHMENT_FILE);
		Path datasource = Paths.get(urlc.toURI()).getParent();
		chyfData = new ChyfShapeDataSource(datasource);
	}
	
	@AfterClass 
	public static void shutdown() throws Exception{
		chyfData.close();
	}
	
	@Test
	public void testCatchmentComputations() throws Exception {
		Distance2DProcessor processor = new Distance2DProcessor(chyfData, CRS.decode("EPSG:3978"));
		processor.setCellSize(100);
		processor.doWork();
		HashMap<String, Double> results = processor.getResults();
		
		Assert.assertEquals("Invalid 2d distance for catchment 1", 84.3136166269, results.get("Catchment.1"), 0.000000001);
		Assert.assertEquals("Invalid 2d distance for catchment 2", 105.3403686506, results.get("Catchment.2"), 0.000000001);
		Assert.assertEquals("Invalid 2d distance for catchment 3", 93.9446550784, results.get("Catchment.3"), 0.000000001);
		Assert.assertEquals("Invalid 2d distance for catchment 4", 0.0, results.get("Catchment.4"), 0.000000001);
		Assert.assertEquals("Invalid 2d distance for catchment 5", 95.7209231318, results.get("Catchment.5"), 0.000000001);
	}
}
