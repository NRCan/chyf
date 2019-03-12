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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import net.refractions.chyf.datatools.processor.Distance2DProcessor;
import net.refractions.chyf.datatools.processor.Distance2DResult;
import net.refractions.chyf.datatools.processor.ProgressMonitor;
import net.refractions.chyf.datatools.readers.ChyfDataSource;
import net.refractions.chyf.datatools.readers.ChyfGeoPackageDataSource;
import net.refractions.chyf.datatools.readers.ChyfShapeDataSource;
import net.refractions.chyf.datatools.writer.ChyfGeoPackageDataSourceDistance2DWriter;
import net.refractions.chyf.datatools.writer.ChyfShapeDataSourceDistance2DWriter;

public class ChyfDistance2Water2DProcessor {

	/**
	 * Takes three parameters
	 * 1 - input dataset
	 * 2 - Working EPSG Code
	 * 3 - output dataset file name
	 * 
	 * Supports either geopackage or shapefile input datasets 
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		if (args.length != 3) {
			printUsage();
			return;
		}

		String sepsg = args[0];
		String sinput = args[1];
		String sout = args[2];
		
		//for shapefiles we want the parent directory
		Path infile = Paths.get(sinput);
		if (infile.toString().endsWith(".shp")) {
			infile = infile.getParent();
		}
				
		try {
			ProgressMonitor progressPrinter = new ProgressMonitor() {
				public void worked(int amount) {
					super.worked(amount);
					if (amount % 10 == 0) System.out.println(getPercentage() + "%");
				}
			};
			
			(new ChyfDistance2Water2DProcessor()).compute(infile.toString(), sout, sepsg, progressPrinter);
		}catch (Exception ex) {
			ex.printStackTrace();
			System.out.println(ex.getMessage());
			printUsage();
		}
	};
	
	/**
	 * 
	 * @param sinFile  Either a geopackage file or a directory containing dataset shapefiles
	 * @param soutFile Either a geopackage file or a path to shapefile
	 * @param srid
	 * @throws Exception
	 */
	public void compute(String sinFile, String soutFile, String srid, ProgressMonitor monitor) throws Exception{
		Path input = Paths.get(sinFile);
		Path outFile = Paths.get(soutFile);
		
		CoordinateReferenceSystem workingCRS;
		try{
			workingCRS = CRS.decode(srid);
		}catch (Exception ex) {
			throw new Exception("Could not parse epsg code :" + srid, ex);
			
		}
		
		if (Files.isDirectory(input)) {
			//SHAPEFILE
			try(ChyfDataSource dataSource = new ChyfShapeDataSource(input)){
			
				if (!outFile.toString().endsWith(".shp")) {
					throw new Exception("Output must be a shapefile (.shp) for shpaefile input.");
				}
				if (Files.exists(outFile)) {
					throw new Exception("Output file exists - cannot overwrite.");
				}
				
				Distance2DResult results = run(dataSource, workingCRS, monitor);
				
				if (results != null) {
					ChyfShapeDataSourceDistance2DWriter writer = new ChyfShapeDataSourceDistance2DWriter(dataSource, outFile);
					writer.write(results);
				}
			}
					
		}else if (input.toString().endsWith(".gpkg")) {
			//GEOPACKAGE
			try(ChyfDataSource dataSource = new ChyfGeoPackageDataSource(input)){
			
				if (!outFile.toString().endsWith(".gpkg")) {
					throw new Exception("Output must be a geopackage (.gpkg) for geopackage input.");
				}
				if (Files.exists(outFile)) {
					throw new Exception("Output file exists - cannot overwrite.");
				}
				Distance2DResult results = run(dataSource, workingCRS, monitor);
				
				if (results != null) {
					ChyfGeoPackageDataSourceDistance2DWriter writer = new ChyfGeoPackageDataSourceDistance2DWriter((ChyfGeoPackageDataSource) dataSource, outFile);
					writer.write(results);
				}
			}
		}else {
			throw new Exception("Input data source: '" + sinFile + "' not supported.  Must be geopackage file or a directory");
		}
	}
	
	private static Distance2DResult run(ChyfDataSource dataSource, CoordinateReferenceSystem crs, ProgressMonitor monitor) throws Exception{
		Distance2DProcessor engine = new Distance2DProcessor(dataSource, crs);
//		engine.setCellSize(100);
		engine.doWork(monitor);
		return engine.getResults();		
	}
	
	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("ChyfDistance2DDataProcessor  [srid] [input] [output]");
		System.out.println("[srid] - the equal area projection valid for the input dataset to compute distances in (eg EPSG:3978)");
		System.out.println("[input] - the input dataset (must be either the Catchment.shp file OR a geopackage file).  If providing Catchment.shp file, the Waterbody.shp and Flowpath.shp files must exist in the same directory.");
		System.out.println("[output] - the output location (either a shapefile or a geopackage file)");
	}
}
