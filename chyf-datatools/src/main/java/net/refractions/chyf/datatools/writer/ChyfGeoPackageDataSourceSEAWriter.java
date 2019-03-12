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
import net.refractions.chyf.datatools.writer.ChyfShapeDataSourceSEAWriter.StatField;


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
			if (!d.getIdentifier().equals(ChyfGeoPackageDataSource.CATCHMENT_LAYER)) {
				d.setM(false);//don't support 4d geometries
				writer.add(d, DataUtilities.collection(reader.reader(d, null, null)));
			}
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
					boolean add = true;
					for (StatField field : StatField.values()) {
						if (field.fieldName.equalsIgnoreCase(d.getLocalName())) add = false;
					}
					if (add) values.add( feature.getAttribute(d.getLocalName()) );
				}
				
				SEAResult.Statistics stats = seavalues.getStats().get(feature.getID());
				if (stats != null) {
					for (StatField s : StatField.values()) {
						Double d = s.getValue(stats);
						if (!Double.isNaN(d)) {
							values.add(d);
						}
					}
				}
				features.add(SimpleFeatureBuilder.build(newType, values, feature.getID()));
			}   
		   	writer.add(catchment, DataUtilities.collection(features));
		}
	}
	
	
	
}
