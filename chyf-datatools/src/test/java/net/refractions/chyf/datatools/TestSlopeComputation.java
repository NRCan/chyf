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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import net.refractions.chyf.datatools.processor.ProgressMonitor;
import net.refractions.chyf.datatools.processor.SEAProcessor;
import net.refractions.chyf.datatools.processor.SEAResult;
import net.refractions.chyf.datatools.processor.SEATileProcessor;
import net.refractions.chyf.datatools.readers.ChyfShapeDataSource;
import net.refractions.chyf.datatools.readers.GeoTiffDemReader;


/**
 * Slope computation test that works on a small made up dataset
 * in testdata
 * 
 * @author Emily
 *
 */
public class TestSlopeComputation {

	private static ChyfShapeDataSource chyfData;
	private static GeoTiffDemReader elevationData;
	
	@BeforeClass 
	public static void startup() throws Exception{
		URL url = ClassLoader.getSystemResource("testdata/small/test_elevations.tif");
		elevationData = new GeoTiffDemReader(Paths.get(url.toURI()));
		
		URL urlc = ClassLoader.getSystemResource("testdata/small/" + ChyfShapeDataSource.CATCHMENT_FILE);
		Path datasource = Paths.get(urlc.toURI()).getParent();
		
		chyfData = new ChyfShapeDataSource(datasource);
	}
	
	
	@AfterClass 
	public static void shutdown() throws Exception{
		elevationData.close();
		chyfData.close();
	}
	
	@Test
	public void testCatchmentComputations() throws Exception {
		SEAProcessor processor = new SEAProcessor(chyfData, elevationData);
		SEAResult r = processor.doWork(new ProgressMonitor());
		
		List<Geometry> geoms = new ArrayList<>();
		SimpleFeatureReader reader = chyfData.getECatchments(chyfData.getCatchmentBounds());
		while(reader.hasNext()) {
			SimpleFeature sf = reader.next();
			Geometry g = (Geometry) sf.getDefaultGeometry();
			g.setUserData(sf.getID());
			geoms.add(g);
		}
		Collections.sort(geoms, (a,b)->Double.compare(a.getArea(), b.getArea()));
		
		//smallest area should be the same as the next one 
		String fid = (String) geoms.get(0).getUserData();
		SEAResult.Statistics stats = r.getStats().get(fid);
		checkSummary(1, stats, 85.0, 17.0, 54.8333333333, 14.4340948309, 8.0773441454, 11.1185455434);
		checkAspect(1, stats, 1/3.0, 0, 2/3.0, 0, 0);
		
		//smallest area
		fid = (String) geoms.get(1).getUserData();
		stats = r.getStats().get(fid);
		checkSummary(1, stats, 85.0, 17.0, 54.8333333333, 14.4340948309, 8.0773441454, 11.1185455434);
		
		fid = (String) geoms.get(2).getUserData();
		stats = r.getStats().get(fid);
		checkSummary(2, stats, 86.0, 3.0, 39.0625, 11.1991490964, 2.840891507, 7.7906328685);
		checkAspect(2, stats, 2/16.0, 5/16.0, 3/16.0, 5/16.0, 1.0 / 16);
		
		
		fid = (String) geoms.get(3).getUserData();
		stats = r.getStats().get(fid);
		checkSummary(3, stats, 86.0, 3.0, 41.8421052632, 15.959163665310195, 1.017800465088096, 7.171605831564775);
		checkAspect(3, stats, 4/19.0, 6/19.0, 5/19.0, 2/19.0, 2.0 / 19);
	}
	
	private void checkSummary(int id, SEAResult.Statistics stats, double maxe, double mine, double avge, double maxs, double mins, double avgs) {
		Assert.assertEquals("maximum elevation incorrect for polygon " + id, maxe, stats.getMaxElevation(), 1e-7);
		Assert.assertEquals("minimum elevation incorrect for polygon " + id, mine, stats.getMinElevation(), 1e-7);
		Assert.assertEquals("average elevation incorrect for polygon " + id, avge, stats.getAverageElevation(), 1e-7);
		
		Assert.assertEquals("maximum slope incorrect for polygon " + id, maxs, stats.getMaxSlope(), 1e-7);
		Assert.assertEquals("minimum slope incorrect for polygon " + id, mins, stats.getMinSlope(), 1e-7);
		Assert.assertEquals("average slope incorrect for polygon " + id, avgs, stats.getAverageSlope(), 1e-7);
	}
	
	private void checkAspect(int id, SEAResult.Statistics stats, double n, double s, double e, double w, double flat) {
		Assert.assertEquals("north aspect incorrect for polygon " + id, n, stats.getNorthPercent(), 1e-7);
		Assert.assertEquals("south aspect incorrect for polygon " + id, s, stats.getSouthPercent(), 1e-7);
		Assert.assertEquals("east aspect incorrect for polygon " + id, e, stats.getEastPercent(), 1e-7);
		Assert.assertEquals("west aspect incorrect for polygon " + id, w, stats.getWestPercent(), 1e-7);
		Assert.assertEquals("flat aspect incorrect for polygon " + id, flat, stats.getFlatPercent(), 1e-7);
	}

		
	@Test
	public void testSlopeComputation() throws Exception {
		
		//these were computed in R and the floating points are not exactly the same 
		double[][] expectedSlope = new double[][] {
			{7.0398650169372558594, 4.2341947555541992188, 4.9751944541931152344, 10.964083671569824219, 10.953388214111328125, 14.434094429016113281, 9.7548913955688476563, 22.870038986206054688},
			{2.7918756008148193359, 3.0891180038452148438, 5.7617678642272949219, 9.85498809814453125, 2.8408915996551513672, 10.173574447631835938, 8.0773439407348632813, 10.891666412353515625},
			{6.5108323097229003906, 3.4529032707214355469, 11.199149131774902344, 9.7143373489379882813, 6.2837281227111816406, 8.7745561599731445313, 12.870397567749023438, 9.7077302932739257813},
			{14.211904525756835938, 15.959163665771484375, 10.881791114807128906, 4.4761190414428710938, 7.2984066009521484375, 5.2694444656372070313, 9.9584531784057617188, 11.783150672912597656},
			{3.7963559627532958984, 10.08177947998046875, 9.3651580810546875, 7.5490536689758300781, 3.7761530876159667969, 6.2393860816955566406, 11.616092681884765625, 12.110372543334960938},
			{6.1076760292053222656, 15.011850357055664063, 11.218709945678710938, 1.017800450325012207, 6.2231812477111816406, 7.8386554718017578125, 13.270669937133789063, 6.7970166206359863281},
			{9.435161590576171875, 8.5686168670654296875, 6.5855002403259277344, 3.7038762569427490234, 7.054843902587890625, 6.047817230224609375, 11.359738349914550781, 4.9711098670959472656},
			{7.9896531105041503906, 8.03766632080078125, 8.6891393661499023438, 1.2194558382034301758, 8.1609859466552734375, 8.6062917709350585938, 12.051178932189941406, 8.36563873291015625}
		};
		
		//these were computed in R and the floating points are not exactly the same
		double[][] expectedAspect = new double[][] {
			{58.240520477294921875, 258.310638427734375, 147.90740966796875, 70.39591217041015625, 28.5513916015625, 335.92449951171875, 319.127593994140625, 220.9143829345703125},
			{88.53119659423828125, 76.60750579833984375, 138.012786865234375, 147.339080810546875, 220.9143829345703125, 296.02960205078125, 95.55994415283203125, 155.432830810546875},
			{208.810791015625, 346.82745361328125, 45, 61.189205169677734375, 267.397430419921875, 245.0952301025390625, 107.84020233154296875, 111.879150390625},
			{201.1187286376953125, 202.0804443359375, 159.4439544677734375, 70.40771484375, 341.21136474609375, 302.82855224609375, 77.24997711181640625, 81.3843536376953125},
			{93.2397003173828125, 207.6459808349609375, 194.0362396240234375, 215.788970947265625, 217.303955078125, 292.166351318359375, 76.63977813720703125, 84.98688507080078125},
			{37.40535736083984375, 339.5377197265625, 8.3343648910522460938, 309.289398193359375, 241.9661407470703125, 237.608612060546875, 111.12471771240234375, 80.3401031494140625},
			{224.3904876708984375, 247.051055908203125, 109.612091064453125, 100.0079803466796875, 270.5787353515625, 289.2900390625, 73.369049072265625, 82.5685882568359375},
			{274.085601806640625, 190.7131195068359375, 183.7517242431640625, 130.236358642578125, 284.641876220703125, 292.328643798828125, 72.6259613037109375, 72.18111419677734375}
		};
		
		SEATileProcessor tp = new SEATileProcessor(elevationData, chyfData);
		
		GeneralEnvelope env = elevationData.getFileBounds();
		ReferencedEnvelope re = new ReferencedEnvelope(env.getMinimum(0), env.getMaximum(0), env.getMinimum(1), env.getMaximum(1), elevationData.getCrs());
		GridCoverage2D allData = elevationData.getData(re);
		
		int width = allData.getRenderedImage().getData().getWidth();
		int height = allData.getRenderedImage().getData().getWidth();
		
		for (int xa = 1; xa < width - 1; xa ++) {
			for (int ya = 1; ya < height - 1; ya ++) {
				
				int x = allData.getRenderedImage().getMinX() + xa;
				int y = allData.getRenderedImage().getMinY() + ya;
				
				double d1 = allData.getRenderedImage().getData().getSampleDouble(x-1, y-1, 0);
				double d2 = allData.getRenderedImage().getData().getSampleDouble(x, y-1, 0);
				double d3 = allData.getRenderedImage().getData().getSampleDouble(x+1, y-1, 0);
				
				double d4 = allData.getRenderedImage().getData().getSampleDouble(x-1, y, 0);
				double d5 = allData.getRenderedImage().getData().getSampleDouble(x, y, 0);
				double d6 = allData.getRenderedImage().getData().getSampleDouble(x+1, y, 0);
				
				double d7 = allData.getRenderedImage().getData().getSampleDouble(x-1, y+1, 0);
				double d8 = allData.getRenderedImage().getData().getSampleDouble(x, y+1, 0);
				double d9 = allData.getRenderedImage().getData().getSampleDouble(x+1, y+1, 0);
				
				double[] slope = tp.computeSlopeAspect(new double[][] {{d1,d4,d7},{d2, d5, d8},{d3, d6, d9}});
				
				int s1 = (int)Math.round( slope[0] * 100_000);
				int s2 = (int)Math.round( expectedSlope[ya-1][xa-1] * 100_000);
				Assert.assertEquals(s2, s1);
				
				s1 = (int)Math.round( slope[1] * 100);
				s2 = (int)Math.round( expectedAspect[ya-1][xa-1] * 100);
				Assert.assertEquals(s2, s1);
			}
			
		}
		
	}

}
