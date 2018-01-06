package net.refractions.chyf.indexing;

public interface Filter<T> {
	public boolean pass(T item);	
}
