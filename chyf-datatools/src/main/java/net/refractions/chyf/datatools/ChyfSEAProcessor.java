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

import net.refractions.chyf.datatools.processor.ProgressMonitor;
import net.refractions.chyf.datatools.processor.SEAProcessor;
import net.refractions.chyf.datatools.processor.SEAResult;
import net.refractions.chyf.datatools.readers.ChyfDataSource;
import net.refractions.chyf.datatools.readers.ChyfGeoPackageDataSource;
import net.refractions.chyf.datatools.readers.ChyfShapeDataSource;
import net.refractions.chyf.datatools.readers.GeoTiffDemReader;
import net.refractions.chyf.datatools.writer.ChyfGeoPackageDataSourceSEAWriter;
import net.refractions.chyf.datatools.writer.ChyfShapeDataSourceSEAWriter;

public class ChyfSEAProcessor {

	/**
	 * Takes three parameters
	 * 1 - input dataset
	 * 2 - input dem
	 * 3 - output dataset file name
	 * 
	 * Supports either geopackage or shapefile input datasets 
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		//Path dem = Paths.get("C:\\data\\CHyF\\data\\Richelieu_DEM_3978.tiled.tif");
		//String datasource ="C:\\data\\CHyF\\github\\chyf-pilot\\data\\quebec\\";
		
		if (args.length != 3) {
			printUsage();
			return;
		}
		
		String sinput = args[0];
		String sdem = args[1];
		String sout = args[2];
		
		Path dem = Paths.get(sdem);
		if (!Files.exists(dem)) {
			System.out.println("DEM file not found.");
			printUsage();
			return;
		}
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
			
			(new ChyfSEAProcessor()).compute(infile.toString(), sdem, sout, progressPrinter);
		}catch (Exception ex) {
			ex.printStackTrace();
			System.out.println(ex.getMessage());
			printUsage();
			return;
		}
	}
	
	/**
	 * 
	 * @param sinFile  Either a geopackage file or a directory containing dataset shapefiles
	 * @param soutFile Either a geopackage file or a path to shapefile
	 * @param srid
	 * @throws Exception
	 */
	public void compute(String sinFile, String sDemFile, String soutFile, ProgressMonitor monitor) throws Exception{
		Path input = Paths.get(sinFile);
		Path outFile = Paths.get(soutFile);
		Path demFile = Paths.get(sDemFile);
		
		if (!Files.exists(demFile)) {
			throw new Exception("DEM file not found.");
		}
		
		if (Files.isDirectory(input)) {
			//SHAPEFILE
			try(ChyfDataSource dataSource = new ChyfShapeDataSource(input)){
			
				if (!outFile.toString().endsWith(".shp")) {
					throw new Exception("Output must be shapefile for shapefile input.");
				}
				if (Files.exists(outFile)) {
					throw new Exception("Output file exists - cannot overwrite.");
				}
				
				SEAResult results = run(dataSource, demFile, monitor);
				
				if (results != null) {
					ChyfShapeDataSourceSEAWriter writer = new ChyfShapeDataSourceSEAWriter(dataSource, outFile);
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
				SEAResult results = run(dataSource, demFile, monitor);
				
				if (results != null) {
					ChyfGeoPackageDataSourceSEAWriter writer = new ChyfGeoPackageDataSourceSEAWriter((ChyfGeoPackageDataSource) dataSource, outFile);
					writer.write(results);
				}
			}
		}else {
			throw new Exception("Input data source: '" + sinFile + "' not supported.  Must be geopackage file or a directory");
		}
	}
	
	private static SEAResult run(ChyfDataSource dataSource, Path dem,  ProgressMonitor monitor) throws Exception{
		SEAResult results = null;
		try(GeoTiffDemReader demReader = new GeoTiffDemReader(dem)){
			SEAProcessor engine = new SEAProcessor(dataSource, demReader);
			results = engine.doWork(monitor);
			return results;
		}
	}
	
	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("ChyfSEADataProcessor [input] [dem] [output]");
		System.out.println("[input] - the input dataset (must be either the Catchment.shp shapefile OR a geopackage file)");
		System.out.println("[dem] - the tiled DEM in geotiff format.  Must be in a projection that maintains area and aspect");
		System.out.println("[output] - the output location (either a shapefile or a geopackage file)");
	}
}
