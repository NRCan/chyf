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
package net.refractions.chyf.datatools;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.refractions.chyf.datatools.processor.Distance2DProcessor;
import net.refractions.chyf.datatools.processor.Distance2DResult;
import net.refractions.chyf.datatools.processor.ProgressMonitor;
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
		processor.doWork(new ProgressMonitor());
		Distance2DResult results = processor.getResults();
		
		Assert.assertEquals("Invalid 2d mean distance for catchment 1", 84.3136166269, results.getResult("Catchment.1").getMean(), 0.000000001);
		Assert.assertEquals("Invalid 2d mean distance for catchment 2", 105.3403686506, results.getResult("Catchment.2").getMean(), 0.000000001);
		Assert.assertEquals("Invalid 2d mean distance for catchment 3", 93.9446550784, results.getResult("Catchment.3").getMean(), 0.000000001);
		Assert.assertEquals("Invalid 2d mean distance for catchment 4", 0.0, results.getResult("Catchment.4").getMean(), 0.000000001);
		Assert.assertEquals("Invalid 2d mean distance for catchment 5", 95.7209231318, results.getResult("Catchment.5").getMean(), 0.000000001);
		
		Assert.assertEquals("Invalid 2d max distance for catchment 1", 189.75376149104397, results.getResult("Catchment.1").getMax(), 0.000000001);
		Assert.assertEquals("Invalid 2d max distance for catchment 2", 267.49315085875656, results.getResult("Catchment.2").getMax(), 0.000000001);
		Assert.assertEquals("Invalid 2d max distance for catchment 3", 226.82422279127996, results.getResult("Catchment.3").getMax(), 0.000000001);
		Assert.assertEquals("Invalid 2d max distance for catchment 4", 0.0, results.getResult("Catchment.4").getMax(), 0.000000001);
		Assert.assertEquals("Invalid 2d max distance for catchment 5", 166.52822583574235, results.getResult("Catchment.5").getMax(), 0.000000001);
		
	}
}
