package net.refractions.chyf.datatools.writer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.datatools.processor.SEAResult;
import net.refractions.chyf.datatools.readers.ChyfGeoPackageDataSource;


/*
 * ****************************** NOT TESTED ************************
 */

/**
 * Creates a new geopackage data files, copying all layers except 
 * catchments to the new file, then creating a new catchment layers
 * with SEA data included
 * 
 * @author Emily
 *
 */
public class ChyfGeoPackageDataSourceSEAWriter {
	
	static final Logger logger = LoggerFactory.getLogger(ChyfGeoPackageDataSourceSEAWriter.class.getCanonicalName());
	
	private ChyfGeoPackageDataSource dataStore;
	private Path outputFile;
	
	public ChyfGeoPackageDataSourceSEAWriter(ChyfGeoPackageDataSource dataStore, Path outputFile) throws IOException {
		this.dataStore = dataStore;
		this.outputFile = outputFile;
	}
	
	public void write(SEAResult seavalues) throws IOException{
		
		GeoPackage reader = new GeoPackage(dataStore.getFile().toFile());
		GeoPackage writer = new GeoPackage(outputFile.toFile());
		
		for (FeatureEntry d : reader.features()) {
			if (!d.getIdentifier().equals(ChyfGeoPackageDataSource.CATCHMENT_LAYER))
				writer.add(d, DataUtilities.collection(reader.reader(d, null, null)));
		}

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
			
			FeatureEntry in = reader.feature(ChyfGeoPackageDataSource.CATCHMENT_LAYER);
			
			FeatureEntry catchment = new FeatureEntry();
			catchment.setIdentifier(in.getIdentifier());
			catchment.setGeometryType(in.getGeometryType());
			catchment.setSrid(in.getSrid());
			catchment.setBounds(in.getBounds());
			catchment.setDataType(in.getDataType());
			catchment.setDescription(in.getDescription());
			catchment.setGeometryColumn(in.getGeometryColumn());
			
			
			List<SimpleFeature> features = new ArrayList<>();
		    
		   	while (freader.hasNext()) {
				SimpleFeature feature = freader.next();
		
				List<Object> values = new ArrayList<>();
				
				for (AttributeDescriptor d : featureType.getAttributeDescriptors()) {
					values.add( feature.getAttribute(d.getLocalName()) );
				}
				
				SEAResult.Statistics stats = seavalues.getStats().get(feature.getID());
				if (stats != null) {
					values.add(stats.getMinElevation());
					values.add(stats.getMaxElevation());
					values.add(stats.getAverageElevation());
					if (stats.getAverageSlope() != Double.NaN) {
						values.add(stats.getMinSlope());
						values.add(stats.getMaxSlope());
						values.add( stats.getAverageSlope());
						values.add(stats.getNorthPercent() * 100);
						values.add(stats.getSouthPercent() * 100);
						values.add(stats.getEastPercent() * 100);
						values.add(stats.getWestPercent() * 100);
						values.add(stats.getFlatPercent() * 100);
					}
				}
				features.add(SimpleFeatureBuilder.build(newType, values, feature.getID()));
			
				writer.add(catchment, DataUtilities.collection(features));
			}   		    
		}
	}
	
	
	
}
