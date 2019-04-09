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
package net.refractions.chyf;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.hygraph.HyGraphBuilder;

public class ChyfDatastore {

	static final Logger logger = LoggerFactory.getLogger(ChyfDatastore.class.getCanonicalName());
	
	public static final int BASE_SRS = 4326; //6624;//4617; // Quebec Albers
	
	public static CoordinateReferenceSystem BASE_CRS = null;
	static {
		try {
			BASE_CRS = CRS.decode("EPSG:" + BASE_SRS, true);
		} catch (FactoryException e) {
			throw new RuntimeException("Could not find CoordinateReferenceSystem object for EPSG Code: " + BASE_SRS, e);
		}
	}
	
	public static GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), BASE_SRS);
	//public static PrecisionModel PRECISION_MODEL = new PrecisionModel(100_000_000_000_000.0);
	public static PrecisionModel PRECISION_MODEL = new PrecisionModel(100_000_000.0);
	
	public static final int MAX_RESULTS = 50000;
	
	private HyGraph hyGraph;

	/**
	 * Creates a new datastore reading input data from database
	 */
	public ChyfDatastore() {
		try {
			HyGraphBuilder gb = new HyGraphBuilder();
			List<Geometry> boundaries = new ArrayList<>();

			ChyfPostgresqlReader reader = new ChyfPostgresqlReader();
			reader.read(gb);
			boundaries = reader.getBoundaries();

			hyGraph = gb.build(boundaries);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Creates a new datastore reading input data from the data folder provided.  This will
	 * read all input datasets in this folder and subfolders (including shape and geopackage).
	 *   
	 * @param dataDir
	 */
	public ChyfDatastore(String dataDir) {
		try {
			//search for all possible input datasets
			//these can either be geopackages or shapefiles
			Path p = Paths.get(dataDir);
			Set<String> inputs = new HashSet<>();
			try(Stream<Path> stream = Files.walk(p)){
				for (Iterator<Path> iterator = stream.iterator(); iterator.hasNext();) {
					Path item = iterator.next();
					if (item.toString().endsWith(".gpkg")) {
						inputs.add(item.toString());
					}else if (item.toString().endsWith(".shp")) {
						inputs.add(item.getParent().toString() + File.separator);
					}
				}
			}
			
		    HyGraphBuilder gb = new HyGraphBuilder();
		    List<Geometry> boundaries = new ArrayList<>();
		    //add each of these datasets
		    for (String in : inputs) {
		    	boundaries.addAll(addDataset(in, gb));
		    }
		    hyGraph = gb.build(boundaries);
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public HyGraph getHyGraph() {
		return hyGraph;
	}
	
	private List<Geometry> addDataset(String dataset, HyGraphBuilder gb) throws Exception {
		Path p = Paths.get(dataset);
		if (Files.isDirectory(p)) {
			ChyfShapeDataReader reader = new ChyfShapeDataReader(dataset);
		    reader.read(gb);
		    return reader.getBoundaries();
		}else if (p.toString().endsWith(".gpkg")) {
			ChyfGeoPackageReader reader = new ChyfGeoPackageReader(p);
		    reader.read(gb);
		    return reader.getBoundaries();
		}
		throw new Exception("No reader found for dataset: " + dataset);
	}
}
