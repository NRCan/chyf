package net.refracitons.chyf.datatools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.refractions.chyf.datatools.processor.SEAProcessor;
import net.refractions.chyf.datatools.processor.SEAResult;
import net.refractions.chyf.datatools.readers.ChyfDataSource;
import net.refractions.chyf.datatools.readers.ChyfGeoPackageDataSource;
import net.refractions.chyf.datatools.readers.ChyfShapeDataSource;
import net.refractions.chyf.datatools.readers.GeoTiffDemReader;
import net.refractions.chyf.datatools.writer.ChyfGeoPackageDataSourceSEAWriter;
import net.refractions.chyf.datatools.writer.ChyfShapeDataSourceSEAWriter;

public class ChyfSEADataProcessor {

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
		
		Path input = Paths.get(sinput);
		Path outFile = Paths.get(sout);
		
		if (Files.isDirectory(input)) {
			//SHAPEFILE
			try(ChyfDataSource dataSource = new ChyfShapeDataSource(input)){
			
				if (!Files.exists(outFile)) {
					Files.createDirectories(outFile);
				}
				if (Files.exists(outFile) && !Files.isDirectory(outFile)) {
					System.out.println("Output must be a directory for shapefile input.");
					printUsage();
					return;
				}
				
				outFile = outFile.resolve(ChyfShapeDataSource.CATCHMENT_FILE);
				if (Files.exists(outFile)) {
					System.out.println("Output file exists - cannot overwrite.");
					printUsage();
					return;
				}
				
				SEAResult results = run(dataSource, dem);
				
				if (results != null) {
					ChyfShapeDataSourceSEAWriter writer = new ChyfShapeDataSourceSEAWriter(dataSource, outFile);
					writer.write(results);
				}
			}
					
		}else if (input.toString().endsWith(".gpkg")) {
			//GEOPACKAGE
			try(ChyfDataSource dataSource = new ChyfGeoPackageDataSource(input)){
			
				if (!outFile.toString().endsWith(".gpkg")) {
					System.out.println("Output must be a geopackage (.gpkg) for geopackage input.");
					printUsage();
					return;
				}
				if (Files.exists(outFile)) {
					System.out.println("Output file exists - cannot overwrite.");
					printUsage();
					return;
				}
				SEAResult results = run(dataSource, dem);
				
				if (results != null) {
					ChyfGeoPackageDataSourceSEAWriter writer = new ChyfGeoPackageDataSourceSEAWriter((ChyfGeoPackageDataSource) dataSource, outFile);
					writer.write(results);
				}
			}
		}else {
			System.out.println("Input data source: '" + sinput + "' not supported.  Must be geopackage file or a directory");
			printUsage();
			return;
		}
	}
	
	private static SEAResult run(ChyfDataSource dataSource, Path dem) throws Exception{
		SEAResult results = null;
		try(GeoTiffDemReader demReader = new GeoTiffDemReader(dem)){
			SEAProcessor engine = new SEAProcessor(dataSource, demReader);
			results = engine.doWork();
			return results;
		}
	}
	
	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("ChyfSEADataProcessor [input] [dem] [output]");
		System.out.println("[input] - the input dataset (either a directory containing shapefiles OR geopackage files)");
		System.out.println("[dem] - the tiled DEM in geotiff format.  Must be in a projection that maintains area and aspect");
		System.out.println("[output] - the output location (either a directory or a geopackage file)");
	}
}
