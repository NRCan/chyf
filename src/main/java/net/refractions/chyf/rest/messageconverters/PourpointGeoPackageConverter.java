package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.hygraph.Nexus;
import net.refractions.chyf.indexing.SpatiallyIndexable;
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


//		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.OUTPUT_PP)){
//			writeProjectedPoutpoint(response);
//		}
//		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.DISTANCE_MIN)){
//			writeRelationship(PourpointEngine.OutputType.DISTANCE_MIN, result.getProjectedPourpointMinDistanceMatrix());
//		}
//		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.DISTANCE_MAX)){
//			writeRelationship(PourpointEngine.OutputType.DISTANCE_MAX, result.getProjectedPourpointMaxDistanceMatrix());
//		}
//		
//		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.NONOVERLAPPINGCATCHMENT_RELATIONSHIP)){
//			String[] headers =new String[result.getPoints().size()];
//			for (int i = 0; i < headers.length; i ++) {
//				headers[i] = result.getPoints().get(i).getId();
//			}
//			writeRelationship(PourpointEngine.OutputType.NONOVERLAPPINGCATCHMENT_RELATIONSHIP, headers, result.getNonOverlappingCatchmentRelationship());
//		}
//		
//		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION)){
//			List<UniqueSubCatchment> items = result.getTraversalCompliantCatchments();
//			String[] headers =new String[items.size()];
//			for (int i = 0; i < headers.length; i ++) {
//				headers[i] = items.get(i).getId();
//			}
//			writeRelationship(PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION, headers, result.getTraversalCompliantCatchmentRelationship());
//		}
//		
//		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.CATCHMENTS)){
//			result.get
//			writeCatchments(response);
//		}
//		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.NONOVERLAPPING_CATCHMENTS)){
//			writeNonOverlappingCatchments(response);
//		}
//		
//		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS)){
//			writeTraversalCompliantCatchments(response);
//		}
//		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.CATCHMENT_CONTAINMENT)) {
//			writeCatchmentContainment(response);
//		}
		
	}
	
	
	private void writeGeometryLayer(PourpointEngine.OutputType type) throws IOException {
		SimpleFeatureType featureType = getFeatureType(type);
		ListFeatureCollection features = new ListFeatureCollection(featureType);
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		
		for (Pourpoint p : result.getPoints()) {
			if (type == PourpointEngine.OutputType.CATCHMENTS) {
				DrainageArea area = result.getCatchment(p);
					
				featureBuilder.set("id", p.getId());
				featureBuilder.set("area", area.getArea());
				featureBuilder.set("geometry", GeotoolsGeometryReprojector.reproject(area.getGeometry(), response.getSrs()));
				features.add(featureBuilder.buildFeature(p.getId()));
			}else if (type == PourpointEngine.OutputType.NONOVERLAPPING_CATCHMENTS) {
				DrainageArea area = result.getNonOverlappingCatchments(p);
				
				featureBuilder.set("id", p.getId());
				featureBuilder.set("area", area.getArea());
				featureBuilder.set("geometry", GeotoolsGeometryReprojector.reproject(area.getGeometry(), response.getSrs()));
				features.add(featureBuilder.buildFeature(p.getId()));
			}else if (type == PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS) {
				for (UniqueSubCatchment c : result.getTraversalCompliantCatchments(p)) {
					featureBuilder.set("id", c.getId());
					featureBuilder.set("area", c.getDrainageArea().getArea());
					featureBuilder.set("geometry", GeotoolsGeometryReprojector.reproject(c.getDrainageArea().getGeometry(), response.getSrs()));
					features.add(featureBuilder.buildFeature(c.getId()));
				}
			}
		}
		
				
		FeatureEntry entry = new FeatureEntry();
		entry.setDataType(org.geotools.geopkg.Entry.DataType.Feature);
		entry.setGeometryColumn("geometry");
		entry.setGeometryType(Geometries.LINESTRING);
		entry.setSrid(response.getSrs());
		entry.setDescription("Collection of Flowpaths");
		entry.setTableName("flowpath");
		geopkg.add(entry, features);
	}
	
	private SimpleFeatureType getFeatureType(PourpointEngine.OutputType type) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setSRS("EPSG:" + String.valueOf(response.getSrs()));
		builder.setName(type.key);
		switch(type) {
			case CATCHMENTS:
			case INTERIOR_CATCHMENT:
			case NONOVERLAPPING_CATCHMENTS:		
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
			case NONOVERLAPPINGCATCHMENT_RELATIONSHIP:
			case TRAVERSAL_COMPLIANT_CATCHMENT_RELATION:
			default:
				return null;
		
		}
	
		return builder.buildFeatureType();
	}
}
