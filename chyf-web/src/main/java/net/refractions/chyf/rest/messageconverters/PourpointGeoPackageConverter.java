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
package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeatureType;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

public class PourpointGeoPackageConverter {

	protected GeoPackage geopkg;
	
	private PourpointOutput result;
	private ApiResponse response;
	
	public PourpointGeoPackageConverter(GeoPackage geopkg) {
		this.geopkg = geopkg;
	}
	
	public void convertResponse(ApiResponse response) throws IOException {
		result = (PourpointOutput) response.getData();
		this.response = response;

		PourpointEngine.OutputType[] geomOutputs = new PourpointEngine.OutputType[] {
				PourpointEngine.OutputType.OUTPUT_PP,
				PourpointEngine.OutputType.INTERIOR_CATCHMENT,
				PourpointEngine.OutputType.CATCHMENTS,
				PourpointEngine.OutputType.SUBCATCHMENTS,
				PourpointEngine.OutputType.PARTITIONED_CATCHMENTS,
		};
		
		for (PourpointEngine.OutputType out : geomOutputs) {
			if (result.getAvailableOutputs().contains(out)) writeGeometryLayer(out);
		}
		
		//at this time we cannot convert the relationships to geopackage layers
		//so they are not included in the geopackage output
		
	}
	
	
	private void writeGeometryLayer(PourpointEngine.OutputType type) throws IOException {
		SimpleFeatureType featureType = getFeatureType(type);
		ListFeatureCollection features = new ListFeatureCollection(featureType);
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		
		if (type == PourpointEngine.OutputType.INTERIOR_CATCHMENT) {
			int id = 1; 
			for (DrainageArea area : result.getInteriorCatchments()) {
				featureBuilder.set("id",id);
				featureBuilder.set("area", area.getArea() / 10_000);
				featureBuilder.set("geometry", reproject(area.getGeometry(), response.getSrs()));
				//stats
				if (response.includeStats() && area.hasStats()) {
					for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
						Double v = area.getStat(s);
						if (v != null) featureBuilder.set(s.getFieldName().toLowerCase(), v);
					}
				}
				features.add(featureBuilder.buildFeature(String.valueOf(id++)));
			}
		}else if (type == PourpointEngine.OutputType.PARTITIONED_CATCHMENTS) {
			for (DrainageArea c : result.getPartitionedCatchments()) {
				featureBuilder.set("id", c.getId());
				featureBuilder.set("area", c.getArea() / 10_000);
				featureBuilder.set("geometry", reproject(c.getGeometry(), response.getSrs()));
				//stats
				if (response.includeStats() && c.hasStats()) {
					for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
						Double v = c.getStat(s);
						if (v != null) featureBuilder.set(s.getFieldName().toLowerCase(), v);
					}
				}
				features.add(featureBuilder.buildFeature(c.getId()));
			}
		}else {
			for (Pourpoint p : result.getPoints()) {
				if (type == PourpointEngine.OutputType.CATCHMENTS) {
					DrainageArea area = result.getCatchment(p);
					featureBuilder.set("id", p.getId());
					featureBuilder.set("area", area.getArea() / 10_000);
					featureBuilder.set("geometry", reproject(area.getGeometry(), response.getSrs()));
					//stats
					if (response.includeStats() && area.hasStats()) {
						for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
							Double v = area.getStat(s);
							if (v != null) featureBuilder.set(s.getFieldName().toLowerCase(), v);
						}
					}
					features.add(featureBuilder.buildFeature(p.getId()));
				}else if (type == PourpointEngine.OutputType.SUBCATCHMENTS) {
					DrainageArea area = result.getSubcatchment(p);
					
					featureBuilder.set("id", p.getId());
					featureBuilder.set("area", area.getArea() / 10_000);
					featureBuilder.set("geometry", reproject(area.getGeometry(), response.getSrs()));
					//stats
					if (response.includeStats() && area.hasStats()) {
						for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
							Double v = area.getStat(s);
							if (v != null) featureBuilder.set(s.getFieldName().toLowerCase(), v);
						}
					}
					features.add(featureBuilder.buildFeature(p.getId()));
				
				}else if (type == PourpointEngine.OutputType.OUTPUT_PP) {
					Point raw =  reproject(p.getRawPoint(), response.getSrs());
					featureBuilder.set("id", p.getId());
					featureBuilder.set("ccode", p.getCcode());
					featureBuilder.set("raw_x", raw.getX());
					featureBuilder.set("raw_y", raw.getY());
					featureBuilder.set("geometry",reproject(p.getProjectedPoint(), response.getSrs()));
					features.add(featureBuilder.buildFeature(p.getId()));	
				}
			}
		}
		
				
		FeatureEntry entry = new FeatureEntry();
		entry.setDataType(org.geotools.geopkg.Entry.DataType.Feature);
		entry.setGeometryColumn("geometry");
		entry.setGeometryType(Geometries.LINESTRING);
		entry.setSrid(response.getSrs());
		entry.setDescription(type.layername);
		entry.setTableName(type.key);
		geopkg.add(entry, features);
	}
	
	private SimpleFeatureType getFeatureType(PourpointEngine.OutputType type) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setSRS("EPSG:" + String.valueOf(response.getSrs()));
		builder.setName(type.layername);
		switch(type) {
			case CATCHMENTS:
			case INTERIOR_CATCHMENT:
			case SUBCATCHMENTS:		
			case PARTITIONED_CATCHMENTS:
				builder.add("id", String.class);
				builder.add("area", Double.class);
				if (response.includeStats()) {
					for(ConverterHelper.ECatchmentField field : ConverterHelper.ECatchmentField.values()) {
						builder.add(field.fieldName, field.type);
					}
				}
				builder.add("geometry", Polygon.class);
				
				break;
			case OUTPUT_PP:
				builder.add("id", String.class);
				builder.add("ccode", Integer.class);
				builder.add("raw_x", Double.class);
				builder.add("raw_y", Double.class);
				builder.add("geometry", Point.class);
				break;
				
			case CATCHMENT_CONTAINMENT:
			case DISTANCE_MAX:
			case DISTANCE_PRIMARY:
			case DISTANCE_MIN:
			case SUBCATCHMENT_RELATIONSHIP:
			case PARTITIONED_CATCHMENT_RELATION:
			default:
				return null;
		
		}
	
		return builder.buildFeatureType();
	}

	private <T extends Geometry> T reproject(T geom, int tosrs) {
		return (T)GeotoolsGeometryReprojector.reproject(geom, ChyfDatastore.BASE_CRS, GeotoolsGeometryReprojector.srsCodeToCRS(tosrs));
	}
}
