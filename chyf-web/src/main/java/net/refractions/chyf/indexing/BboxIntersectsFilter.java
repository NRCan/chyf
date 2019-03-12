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
package net.refractions.chyf.indexing;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;

public class BboxIntersectsFilter<T extends SpatiallyIndexable> implements Filter<T> {
	
	private Envelope bbox;
	
	public BboxIntersectsFilter(Envelope bbox) {
		this.bbox = bbox;
	}

	public BboxIntersectsFilter(Point centre, double radius) {
		bbox = new Envelope(
				centre.getX() - radius,
				centre.getX() + radius,
				centre.getY() - radius,
				centre.getY() + radius);
	}

	@Override
	public boolean pass(SpatiallyIndexable item) {
		return bbox.intersects(item.getEnvelope());
	}
	
}
