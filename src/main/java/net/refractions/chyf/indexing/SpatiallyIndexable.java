package net.refractions.chyf.indexing;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;


public interface SpatiallyIndexable {

	Envelope getEnvelope();
	
	double distance(Point p);
}
