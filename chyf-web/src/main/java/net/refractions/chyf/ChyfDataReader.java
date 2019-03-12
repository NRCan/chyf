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
package net.refractions.chyf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.geotools.data.FeatureReader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.enumTypes.Rank;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.HyGraphBuilder;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;
import net.refractions.util.UuidUtil;

/**
 * CHyF data source reader that provides general functions for reading input data
 * from geotools feature readers.
 * 
 * @author Emily
 *
 */
public abstract class ChyfDataReader {

	static final Logger logger = LoggerFactory.getLogger(ChyfDataReader.class.getCanonicalName());

    protected List<Geometry> boundaries = new ArrayList<>();

    public abstract void read(HyGraphBuilder builder)  throws Exception ;
    
	protected void readWaterbody(FeatureReader<SimpleFeatureType, SimpleFeature> reader, HyGraphBuilder gb) throws Exception {

		while (reader.hasNext()) {
			SimpleFeature feature = reader.next();
			// System.out.print(feature.getID());
			Geometry g = (Geometry) feature.getDefaultGeometryProperty().getValue();
			Polygon catchment = null;
			if (g instanceof Polygon) {
				catchment = (Polygon)g;
			}else if (g instanceof MultiPolygon) {
				catchment = (Polygon) ((MultiPolygon)g).getGeometryN(0);
			}
			catchment = GeotoolsGeometryReprojector.reproject(catchment, reader.getFeatureType().getCoordinateReferenceSystem(), ChyfDatastore.BASE_CRS);
			CatchmentType type = CatchmentType.UNKNOWN;
			Object def = feature.getAttribute("DEFINITION");
			Integer intValue = -1;
			if (def instanceof Integer) {
				intValue = (Integer) def;
			}else if (def instanceof Long) {
				intValue = ((Long)def).intValue();
			}
			switch (intValue) {
			case 1:
				type = CatchmentType.WATER_CANAL;
				break;
			case 4:
				type = CatchmentType.WATER_LAKE;
				break;
			case 6:
				type = CatchmentType.WATER_RIVER;
				break;
			case 9:
				type = CatchmentType.WATER_POND;
				break;
			}
			double area = (double) feature.getAttribute("AREA");
			
			catchment = (Polygon) GeometryPrecisionReducer.reduce(catchment, ChyfDatastore.PRECISION_MODEL);
			gb.addECatchment(type, area, catchment);
		}

	}

	protected void readCatchment(FeatureReader<SimpleFeatureType, SimpleFeature> reader, HyGraphBuilder gb) throws Exception {
		
		
		boolean[] hasAttribute = new boolean[ECatchment.ECatchmentStat.values().length];
		for (int i = 0; i < hasAttribute.length; i ++) hasAttribute[i] = false;
		
		for (AttributeDescriptor d : reader.getFeatureType().getAttributeDescriptors()) {
			for (int i = 0; i < hasAttribute.length; i ++) {
				if (d.getLocalName().equalsIgnoreCase(ECatchment.ECatchmentStat.values()[i].getFieldName())) hasAttribute[i] = true;
			}
		}
				
		while (reader.hasNext()) {
			SimpleFeature feature = reader.next();
			Geometry g = (Geometry) feature.getDefaultGeometryProperty().getValue();
			Polygon catchment = null;
			if (g instanceof Polygon) {
				catchment = (Polygon)g;
			}else if (g instanceof MultiPolygon) {
				catchment = (Polygon) ((MultiPolygon)g).getGeometryN(0);
			}
			double area = (double) feature.getAttribute("AREA");
			
			catchment = GeotoolsGeometryReprojector.reproject(catchment, reader.getFeatureType().getCoordinateReferenceSystem(), ChyfDatastore.BASE_CRS);
			catchment = (Polygon) GeometryPrecisionReducer.reduce(catchment, ChyfDatastore.PRECISION_MODEL);
			
			ECatchment newCatchment = gb.addECatchment(CatchmentType.UNKNOWN, area, catchment);
			if (newCatchment != null) {
				//statistic attributes if applicable
				for (int i = 0; i < hasAttribute.length; i ++) {
					if (hasAttribute[i]) {
						ECatchment.ECatchmentStat stat = ECatchment.ECatchmentStat.values()[i];
						Double x = (Double)feature.getAttribute(stat.getFieldName());
						if (x != null) {
							stat.updateCatchment(newCatchment, x);
						}
					}
				}
			}
		}
	}

	protected void readFlowpath(FeatureReader<SimpleFeatureType, SimpleFeature> reader, HyGraphBuilder gb) throws Exception {
		
		while (reader.hasNext()) {
			SimpleFeature feature = reader.next();
			// System.out.print(feature.getID());
			Geometry g = (Geometry) feature.getDefaultGeometryProperty().getValue();
			LineString flowPath = null;
			if (g instanceof LineString) {
				flowPath = (LineString)g;
			}else if (g instanceof MultiLineString) {
				flowPath = (LineString) ((MultiLineString)g).getGeometryN(0);
			}
			
			flowPath = GeotoolsGeometryReprojector.reproject(flowPath, reader.getFeatureType().getCoordinateReferenceSystem(), ChyfDatastore.BASE_CRS);
			FlowpathType type = FlowpathType.convert((String) feature.getAttribute("TYPE"));
			String rankString = (String) feature.getAttribute("RANK");
			Rank rank = Rank.convert(rankString);
			String name = ((String) feature.getAttribute("NAME")).intern();
			UUID nameId = null;
			try {
				nameId = UuidUtil.UuidFromString((String) feature.getAttribute("NAMEID"));
			} catch (IllegalArgumentException iae) {
				logger.warn("Exception reading UUID: " + iae.getMessage());
			}
			double length = (double) feature.getAttribute("LENGTH");
			
			flowPath = (LineString) GeometryPrecisionReducer.reduce(flowPath, ChyfDatastore.PRECISION_MODEL);
			gb.addEFlowpath(type, rank, name, nameId, length, flowPath);
		}
	}

	protected void readBoundary(FeatureReader<SimpleFeatureType, SimpleFeature> reader, HyGraphBuilder gb) throws Exception {
		while (reader.hasNext()) {
			SimpleFeature feature = reader.next();
			Geometry g = (Geometry) feature.getDefaultGeometryProperty().getValue();
			g = GeotoolsGeometryReprojector.reproject(g, reader.getFeatureType().getCoordinateReferenceSystem(), ChyfDatastore.BASE_CRS);
			g = GeometryPrecisionReducer.reduce(g, ChyfDatastore.PRECISION_MODEL);
			boundaries.add(g);
		}
	}

	public List<Geometry> getBoundaries(){
		return this.boundaries;
	}
}
