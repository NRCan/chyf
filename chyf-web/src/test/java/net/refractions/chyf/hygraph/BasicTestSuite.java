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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.geotools.referencing.CRS;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.ChyfShapeDataReader;

/**
 * Test suite that tests ChyfDatastore/HyGraph 
 * directly without using web server, so this does not test 
 * REST interface.
 * 
 * @author Emily
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
	StreamOrderTest.class, 
	FlowpathTest.class, 
	DrainageTest.class, 
	ElementaryDrainageTest.class,
	MultiDimensionalDownstreamTest.class,
	PourpointProjectionTest.class,
	PourpointTest.class,
	SimpleDataPourpointTest.class,
	PointRelationshipTreeTest.class,
	PourpointSecondaryTest.class})

public class BasicTestSuite extends Suite {

	protected static ChyfDatastore DATASTORE = null;
	
	/**
	 * directory in the resources folder of location
	 * of the results datasets
	 */
	protected static String RESULTS_DIR = "results";
	/**
	 * The SRID of the test data.  This is defined here and not read from the
	 * prj files
	 */
	public static int TEST_DATA_SRID = 4617;
	public static CoordinateReferenceSystem TEST_CRS = null;
	static {
		try {
			TEST_CRS = CRS.decode("EPSG:" + TEST_DATA_SRID);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static GeometryFactory GF = new GeometryFactory(new PrecisionModel(), TEST_DATA_SRID);
	
	public BasicTestSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
		super(klass, builder);
	}
	
	@ClassRule
	public static ExternalResource SETUP_RULE = new ExternalResource() {
		@Override
		protected void before() throws Throwable {
			if (DATASTORE != null) return;
			URL url = ClassLoader.getSystemResource("data/" + ChyfShapeDataReader.FLOWPATH_FILE);
			Path datapath = Paths.get(url.toURI()).getParent();
//			Path datapath = Paths.get("C:\\data\\CHyF\\github\\chyf-pilot\\data\\data_small");
//			Path datapath = Paths.get("C:\\data\\CHyF\\github\\chyf-pilot\\src\\test\\resources\\data");
			DATASTORE = new ChyfDatastore(datapath.toString() + "/");
		};

		@Override
		protected void after() {

		};
	};
	
}
