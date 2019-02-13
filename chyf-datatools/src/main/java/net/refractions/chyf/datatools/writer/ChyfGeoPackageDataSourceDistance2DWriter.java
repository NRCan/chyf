package net.refractions.chyf.datatools.writer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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

import net.refractions.chyf.datatools.readers.ChyfGeoPackageDataSource;
import net.refractions.chyf.datatools.writer.ChyfShapeDataSourceDistance2DWriter.StatField;


/*
 * ****************************** NOT TESTED ************************
 */

/**
 * Creates a new geopackage data files, copying all layers except 
 * catchments to the new file, then creating a new catchment layers
 * with 2D distance to water added
 * 
 * @author Emily
 *
 */
public class ChyfGeoPackageDataSourceDistance2DWriter {
	
	static final Logger logger = LoggerFactory.getLogger(ChyfGeoPackageDataSourceDistance2DWriter.class.getCanonicalName());
	
	private ChyfGeoPackageDataSource dataStore;
	private Path outputFile;
	
	public ChyfGeoPackageDataSourceDistance2DWriter(ChyfGeoPackageDataSource dataStore, Path outputFile) throws IOException {
		this.dataStore = dataStore;
		this.outputFile = outputFile;
	}
	
	public void write(HashMap<String,Double> distance2dValues) throws IOException{
		
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
				boolean add = true;
				for (StatField field : StatField.values()) {
					if (field.fieldName.equalsIgnoreCase(d.getLocalName())) add = false;
				}
				if (add) b.add(d);
			}
			
			for (StatField s : StatField.values()) {
				b.add(s.fieldName, Double.class);
			}
			
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
				
				for (AttributeDescriptor d : newType.getAttributeDescriptors()) {
					values.add( feature.getAttribute(d.getLocalName()) );
				}
				Double value = distance2dValues.get(feature.getID());
				if (value != null) {
					values.add(value);
				}
				features.add(SimpleFeatureBuilder.build(newType, values, feature.getID()));
				writer.add(catchment, DataUtilities.collection(features));
			}   		    
		}
	}
	
	
	
}
