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

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;

public class GeotoolsGeometryReprojector {
	
	public static <T extends Geometry> T reproject(T geom, CoordinateReferenceSystem fromCRS, CoordinateReferenceSystem toCRS) {
		if(geom == null) {
			return null;
		}
		try {
			//TODO: fix this
			MathTransform transform = CRS.findMathTransform(fromCRS, toCRS, true);
			if(fromCRS.getCoordinateSystem().getAxis(0).getDirection().absolute()
					.equals(AxisDirection.NORTH)) {
				geom = flipAxes(geom);
			}
			@SuppressWarnings("unchecked")
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
}
