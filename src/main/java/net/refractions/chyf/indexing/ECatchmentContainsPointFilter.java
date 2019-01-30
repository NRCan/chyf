package net.refractions.chyf.indexing;

import org.locationtech.jts.geom.Point;

import net.refractions.chyf.hygraph.ECatchment;

public class ECatchmentContainsPointFilter implements Filter<ECatchment> {

	private Point point;
	
	public ECatchmentContainsPointFilter(Point point) {
		this.point = point;
	}

	@Override
	public boolean pass(ECatchment eCatchment) {
		return eCatchment.getPolygon().contains(point);
	}

}
