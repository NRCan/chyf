package net.refractions.chyf.indexing;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;


public interface SpatiallyIndexable {

	Envelope getEnvelope();
	
	double distance(Point p);
}
