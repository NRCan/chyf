package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.pourpoint.UniqueSubCatchment;
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
				PourpointEngine.OutputType.PARTITIONED_CATCHMENTS,
				PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS,
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
				featureBuilder.set("geometry", GeotoolsGeometryReprojector.reproject(area.getGeometry(), response.getSrs()));
				features.add(featureBuilder.buildFeature(String.valueOf(id++)));
			}
		}else if (type == PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS) {
			for (UniqueSubCatchment c : result.getTraversalCompliantCatchments()) {
				featureBuilder.set("id", c.getId());
				featureBuilder.set("area", c.getDrainageArea().getArea() / 10_000);
				featureBuilder.set("geometry", GeotoolsGeometryReprojector.reproject(c.getDrainageArea().getGeometry(), response.getSrs()));
				features.add(featureBuilder.buildFeature(c.getId()));
			}
		}else {
			for (Pourpoint p : result.getPoints()) {
				if (type == PourpointEngine.OutputType.CATCHMENTS) {
					DrainageArea area = result.getCatchment(p);
					featureBuilder.set("id", p.getId());
					featureBuilder.set("area", area.getArea() / 10_000);
					featureBuilder.set("geometry", GeotoolsGeometryReprojector.reproject(area.getGeometry(), response.getSrs()));
					features.add(featureBuilder.buildFeature(p.getId()));
				}else if (type == PourpointEngine.OutputType.PARTITIONED_CATCHMENTS) {
					DrainageArea area = result.getPartitionedCatchments(p);
					
					featureBuilder.set("id", p.getId());
					featureBuilder.set("area", area.getArea() / 10_000);
					featureBuilder.set("geometry", GeotoolsGeometryReprojector.reproject(area.getGeometry(), response.getSrs()));
					features.add(featureBuilder.buildFeature(p.getId()));
				
				}else if (type == PourpointEngine.OutputType.OUTPUT_PP) {
					Point raw =  GeotoolsGeometryReprojector.reproject(p.getRawPoint(), response.getSrs());
					featureBuilder.set("id", p.getId());
					featureBuilder.set("ccode", p.getCcode());
					featureBuilder.set("raw_x", raw.getX());
					featureBuilder.set("raw_y", raw.getY());
					featureBuilder.set("geometry", GeotoolsGeometryReprojector.reproject(p.getProjectedPoint(), response.getSrs()));
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
			case PARTITIONED_CATCHMENTS:		
			case TRAVERSAL_COMPLIANT_CATCHMENTS:
				builder.add("id", String.class);
				builder.add("area", Double.class);
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
			case DISTANCE_MIN:
			case PARTITIONEDCATCHMENT_RELATIONSHIP:
			case TRAVERSAL_COMPLIANT_CATCHMENT_RELATION:
			default:
				return null;
		
		}
	
		return builder.buildFeatureType();
	}
}
