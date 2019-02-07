package net.refractions.chyf.datatools.writer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.datatools.processor.SEAResult;
import net.refractions.chyf.datatools.readers.ChyfDataSource;

/**
 * Write Slope/Aspect/Elevation catchments statistics to
 * a new catchment shapefile, copy all existing attributes and adding
 * new ones.
 * 
 * @author Emily
 *
 */
public class ChyfShapeDataSourceSEAWriter {
	
	static final Logger logger = LoggerFactory.getLogger(ChyfShapeDataSourceSEAWriter.class.getCanonicalName());
	
	private ChyfDataSource dataStore;
	private Path outputFile;
	
	public ChyfShapeDataSourceSEAWriter(ChyfDataSource dataStore, Path outputFile) throws IOException {
		this.dataStore = dataStore;
		this.outputFile = outputFile;
			
	}
	
	public void write(SEAResult seavalues) throws IOException{
		
		try(SimpleFeatureReader freader = dataStore.getECatchments(null)){
		
			//current featuretype
			SimpleFeatureType featureType = freader.getFeatureType();
			
			//create a new feature type copying all existing attributes and adding new ones
			SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
			b.setName(featureType.getName());
			for (AttributeDescriptor d : featureType.getAttributeDescriptors()) {
				b.add(d);
			}
			b.add("ELV_MIN", Double.class);
			b.add("ELV_MAX", Double.class);
			b.add("ELV_MEAN", Double.class);
			
			b.add("SLOPE_MIN", Double.class);
			b.add("SLOPE_MAX", Double.class);
			b.add("SLOPE_MEAN", Double.class);
			
			b.add("NORTH_PCT", Double.class);
			b.add("SOUTH_PCT", Double.class);
			b.add("EAST_PCT", Double.class);
			b.add("WEST_PCT", Double.class);
			b.add("FLAT_PCT", Double.class);
			
			SimpleFeatureType newType = b.buildFeatureType();
			
	        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
	
	        Map<String, Serializable> params = new HashMap<String, Serializable>();
	        params.put("url", outputFile.toUri().toURL());
	        params.put("create spatial index", Boolean.FALSE);
	        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		    newDataStore.createSchema(newType);
		    newDataStore.forceSchemaCRS(featureType.getCoordinateReferenceSystem());
			
		    try(FeatureWriter<SimpleFeatureType, SimpleFeature> writer = newDataStore.getFeatureWriter(Transaction.AUTO_COMMIT)){
				while (freader.hasNext()) {
					SimpleFeature feature = freader.next();
		
					SimpleFeature toWrite = writer.next();
		
					for (Property p : feature.getProperties()) {
						toWrite.setAttribute(p.getName().toString(), p.getValue());
					}
		
					SEAResult.Statistics stats = seavalues.getStats().get(feature.getID());
					if (stats != null) {
						toWrite.setAttribute("ELV_MIN", stats.getMinElevation());
						toWrite.setAttribute("ELV_MAX", stats.getMaxElevation());
						toWrite.setAttribute("ELV_MEAN", stats.getAverageElevation());
		
						if (stats.getAverageSlope() != Double.NaN) {
							toWrite.setAttribute("SLOPE_MIN", stats.getMinSlope());
							toWrite.setAttribute("SLOPE_MAX", stats.getMaxSlope());
							toWrite.setAttribute("SLOPE_MEAN", stats.getAverageSlope());
		
							toWrite.setAttribute("NORTH_PCT", stats.getNorthPercent() * 100);
							toWrite.setAttribute("SOUTH_PCT", stats.getSouthPercent() * 100);
							toWrite.setAttribute("EAST_PCT", stats.getEastPercent() * 100);
							toWrite.setAttribute("WEST_PCT", stats.getWestPercent() * 100);
							toWrite.setAttribute("FLAT_PCT", stats.getFlatPercent() * 100);
						}
					}
					writer.write();
				}   
		    }
		    newDataStore.dispose();
		}
		
	}
	
	
	
}
