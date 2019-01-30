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
