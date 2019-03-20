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
package net.refractions.chyf.datatools.processor;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Reprojection tools.
 * 
 * @author Emily
 *
 */
public class ReprojectionUtils {
	
	public static <T extends Geometry> T reproject(T geom, CoordinateReferenceSystem fromCRS, CoordinateReferenceSystem toCRS) {
		if(geom == null) {
			return null;
		}
		try {
			
			MathTransform transform = CRS.findMathTransform(fromCRS, toCRS, true);
			
			if(fromCRS.getCoordinateSystem().getAxis(0).getDirection().absolute()
					.equals(AxisDirection.NORTH)) {
				geom = flipAxes(geom);
			}
			
			T newGeom = (T)JTS.transform(geom, transform);
			if(toCRS.getCoordinateSystem().getAxis(0).getDirection().absolute()
					.equals(AxisDirection.NORTH)) {
				newGeom = flipAxes(newGeom);
			}
			return newGeom;
		} catch(FactoryException fe) {
			throw new RuntimeException("Unexpected error in coordinate reprojection.", fe);
		} catch(TransformException te) {
			throw new RuntimeException("Unexpected error in coordinate reprojection.", te);
		}
	}

	public static ReferencedEnvelope reproject(ReferencedEnvelope env, CoordinateReferenceSystem toCRS) {
		if(env == null) {
			return null;
		}
		try {
			MathTransform transform = CRS.findMathTransform(env.getCoordinateReferenceSystem(), toCRS, true);
			if(env.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getDirection().absolute()
					.equals(AxisDirection.NORTH)) {
				env = flipAxes(env);
			}
			
			
			Envelope e = JTS.transform(env, transform);
			ReferencedEnvelope re = new ReferencedEnvelope(e, toCRS);
			
			if(toCRS.getCoordinateSystem().getAxis(0).getDirection().absolute()
					.equals(AxisDirection.NORTH)) {
				re = flipAxes(re);
			}
			
			
			return re;
		} catch(FactoryException fe) {
			throw new RuntimeException("Unexpected error in coordinate reprojection.", fe);
		} catch(TransformException te) {
			throw new RuntimeException("Unexpected error in coordinate reprojection.", te);
		}
	}

	
	public static CoordinateReferenceSystem srsCodeToCRS(int srsCode) {
		try {
			return CRS.decode("EPSG:" + srsCode);
		} catch(NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException("Invalid srsCode: \"" + srsCode + "\"");
		} catch(FactoryException e) {
			throw new RuntimeException("Unexpected error in coordinate reprojection.");
		}
	}

	private static <T extends Geometry> T flipAxes(T geom) {
		AffineTransformation transform = new AffineTransformation(0, 1, 0, 1, 0, 0);
		@SuppressWarnings("unchecked")
		T newGeom = (T)transform.transform(geom);
		newGeom.setSRID(geom.getSRID());
		return newGeom;
	}
	
	private static ReferencedEnvelope flipAxes(ReferencedEnvelope env) {
		AffineTransformation transform = new AffineTransformation(0, 1, 0, 1, 0, 0);
		
		Coordinate c1 = new Coordinate();
		Coordinate c2 = new Coordinate();
		transform.transform(new Coordinate(env.getMinX(), env.getMinY()), c1);
		transform.transform(new Coordinate(env.getMaxX(), env.getMaxY()), c2);
		
		return new ReferencedEnvelope(c1.x, c2.x, c1.y, c2.y, env.getCoordinateReferenceSystem());
	}
}
