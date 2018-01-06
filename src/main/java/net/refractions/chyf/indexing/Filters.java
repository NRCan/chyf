package net.refractions.chyf.indexing;

import java.util.Iterator;

public class Filters {

	public static <T> void filter(Filter<? super T> filter, Iterable<T> items) {
		Iterator<T> it = items.iterator();
		while(it.hasNext()) {
			T item = it.next();
			if(!filter.pass(item)) {
				it.remove();
			}
		}
	}

}
