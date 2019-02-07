package net.refractions.chyf.datatools.processor;

import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Processing tile that contains an envelope.
 * @author Emily
 *
 */
public class Tile {

	private ReferencedEnvelope env;
	
	public Tile(ReferencedEnvelope e) {
		this.env = e;
	}
	
	public ReferencedEnvelope getEnvelope() {
		return this.env;
	}
}
