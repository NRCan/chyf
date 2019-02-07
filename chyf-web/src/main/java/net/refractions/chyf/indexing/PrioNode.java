package net.refractions.chyf.indexing;

public class PrioNode<T> implements Comparable<PrioNode<T>>{
	public final double priority;
	public final T item;
	
	public PrioNode(double priority, T item) {
		this.priority = priority;
		this.item = item;
	}

	@Override
	public int compareTo(PrioNode<T> other) {
		return Double.compare(priority, other.priority);
	}
	
	@Override
	public String toString() {
		return "PrioNode(" + priority + ", " + item + ")";
	}
}
