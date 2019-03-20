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
package net.refractions.chyf.rest;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.util.GeomUtil;

public class HygraphParameters extends SharedParameters {
	
	private double[] point;
	private Point pointPoint;
	private double[] bbox;
	private Envelope bboxEnvelope;
	private Double maxDistance;
	
	public Point getPoint() {
		return pointPoint;
	}
	
	public void setPoint(double[] point) {
		this.point = point;
	}
	
	public void setPointGeom(Point point) {
		this.pointPoint = point;
	}
	
	public Envelope getBbox() {
		return bboxEnvelope;
	}
	
	public void setBbox(double[] bbox) {
		this.bbox = bbox;
	}
	
	public Double getMaxDistance() {
		return maxDistance;
	}
	
	public void setMaxDistance(Double maxDistance) {
		this.maxDistance = maxDistance;
	}
	
	private static Geometry precisionReducer(Geometry geometry) {
		return GeometryPrecisionReducer.reduce(geometry, ChyfDatastore.PRECISION_MODEL);
	}
	
	public void resolveAndValidate() {
		super.resolveAndValidate();
		// convert any double[] into geometries in input SRS projection
		// then reproject any geometries to the internal projection
		// note the incoming geomtries may either come in as double[]
		// or as geometry objects in the inputSRS projection
		// so we may not need to create them from double[] but still need to reproject
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), getSrs());
		if(point != null && point.length != 0) {
			if(point.length == 2) {
				pointPoint = gf.createPoint(new Coordinate(point[0], point[1]));
			} else {
				throw new IllegalArgumentException(
						"Parameter \"point\" must be in the format \"x,y\".");
			}
		}
		if(pointPoint != null) {
			pointPoint = (Point) precisionReducer(GeotoolsGeometryReprojector.reproject(pointPoint, GeotoolsGeometryReprojector.srsCodeToCRS(getSrs()), ChyfDatastore.BASE_CRS));
		}
		Polygon bboxPolygon = null;
		if(bbox != null && bbox.length != 0) {
			if(bbox.length == 4) {
				bboxPolygon = GeomUtil.buildBbox(bbox, gf);
			} else {
				throw new IllegalArgumentException(
						"Parameter \"bbox\" must be in the format \"minx,miny,maxx,maxy\".");
			}
		}
		if(bboxPolygon != null) {
			bboxPolygon = (Polygon) precisionReducer(GeotoolsGeometryReprojector.reproject(bboxPolygon, GeotoolsGeometryReprojector.srsCodeToCRS(getSrs()), ChyfDatastore.BASE_CRS));
			bboxEnvelope = bboxPolygon.getEnvelopeInternal();
			// if their is a bbox, override the maxdistance with the radius of the bbox
			maxDistance = GeomUtil.getRadius(bboxEnvelope);
		}		
	}

}
