package net.refracitons.chyf.datatools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import net.refractions.chyf.datatools.processor.Distance2DProcessor;
import net.refractions.chyf.datatools.processor.Distance2DResult;
import net.refractions.chyf.datatools.readers.ChyfDataSource;
import net.refractions.chyf.datatools.readers.ChyfGeoPackageDataSource;
import net.refractions.chyf.datatools.readers.ChyfShapeDataSource;
import net.refractions.chyf.datatools.writer.ChyfGeoPackageDataSourceDistance2DWriter;
import net.refractions.chyf.datatools.writer.ChyfShapeDataSourceDistance2DWriter;

public class ChyfDistance2DDataProcessor {

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

		Long now = System.nanoTime();
		
		
		String sepsg = args[0];
		String sinput = args[1];
		String sout = args[2];
		
		Path input = Paths.get(sinput);
		Path outFile = Paths.get(sout);
		
		CoordinateReferenceSystem workingCRS;
		try{
			workingCRS = CRS.decode(sepsg);
		}catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Could not parse epsg code: " + sepsg);
			printUsage();
			return;
		}
		
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
				
				Distance2DResult results = run(dataSource, workingCRS);
				
				if (results != null) {
					ChyfShapeDataSourceDistance2DWriter writer = new ChyfShapeDataSourceDistance2DWriter(dataSource, outFile);
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
				Distance2DResult results = run(dataSource, workingCRS);
				
				if (results != null) {
					ChyfGeoPackageDataSourceDistance2DWriter writer = new ChyfGeoPackageDataSourceDistance2DWriter((ChyfGeoPackageDataSource) dataSource, outFile);
					writer.write(results);
				}
			}
		}else {
			System.out.println("Input data source: '" + sinput + "' not supported.  Must be geopackage file or a directory");
			printUsage();
			return;
		}
		
		Long then = System.nanoTime();
		System.out.println("TIME:" + (then - now));
	}
	
	private static Distance2DResult run(ChyfDataSource dataSource, CoordinateReferenceSystem crs) throws Exception{
		Distance2DProcessor engine = new Distance2DProcessor(dataSource, crs);
		engine.setCellSize(100);
		engine.doWork();
		return engine.getResults();		
	}
	
	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("ChyfDistance2DDataProcessor  [srid] [input] [output]");
		System.out.println("[srid] - the equal area projection valid for the input dataset to compute distances in (eg EPSG:4326)");
		System.out.println("[input] - the input dataset (either a directory containing shapefiles OR geopackage files)");
		System.out.println("[output] - the output location (either a directory or a geopackage file)");
	}
}
