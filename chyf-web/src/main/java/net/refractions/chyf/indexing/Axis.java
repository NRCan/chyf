package net.refractions.chyf.indexing;

public enum Axis {
	X, Y;
	
	public Axis nextAxis() {
		if(this == X) {
			return Y;
		}
		return X;
	}
}
