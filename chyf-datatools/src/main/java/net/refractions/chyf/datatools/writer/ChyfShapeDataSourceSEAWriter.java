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
	
	public static enum StatField{
		ELV_MIN ("ELV_MIN"),
		ELV_MAX ("ELV_MAX"),
		ELV_MEAN ("ELV_MEAN"),
		
		SLOPE_MIN ("SLOPE_MIN"),
		SLOPE_MAX ("SLOPE_MAX"),
		SLOPE_MEAN ("SLOPE_MEAN"),
		
		NORTH_PCT ("NORTH_PCT"),
		SOUTH_PCT ("SOUTH_PCT"),
		EAST_PCT ("EAST_PCT"),
		WEST_PCT ("WEST_PCT"),
		FLAT_PCT ("FLAT_PCT");
		
		public String fieldName;
		
		StatField(String fieldName){
			this.fieldName = fieldName;
		}
		public Double getValue(SEAResult.Statistics stats) {
			switch(this) {
			case EAST_PCT: return stats.getEastPercent()* 100;
			case ELV_MAX: return stats.getMaxElevation();
			case ELV_MEAN: return stats.getAverageElevation();
			case ELV_MIN: return stats.getMinElevation();
			case FLAT_PCT: return stats.getFlatPercent()* 100;
			case NORTH_PCT: return stats.getNorthPercent()* 100;
			case SLOPE_MAX: return stats.getMaxSlope();
			case SLOPE_MEAN: return stats.getAverageSlope();
			case SLOPE_MIN: return stats.getMinSlope();
			case SOUTH_PCT: return stats.getSouthPercent() * 100;
			case WEST_PCT: return stats.getWestPercent()* 100;
			}
			return null;
		}
	}
	
	
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
						for (StatField s : StatField.values()) {
							Double d = s.getValue(stats);
							if (!Double.isNaN(d)) {
								toWrite.setAttribute(s.fieldName, d);
							}
						}
					}
					writer.write();
				}   
		    }
		    newDataStore.dispose();
		}
		
	}
	
	
	
}
