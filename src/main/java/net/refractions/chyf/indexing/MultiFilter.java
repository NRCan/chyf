package net.refractions.chyf.indexing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiFilter<T> implements Filter<T> {
	
	@SuppressWarnings("rawtypes")
	private List<Filter> filters;
	
	@SuppressWarnings("rawtypes")
	@SafeVarargs
	public MultiFilter(Filter<? super T>... filters) {
		this.filters = new ArrayList<Filter>(Arrays.asList(filters));
	}

	@SuppressWarnings("rawtypes")
	public MultiFilter() {
		this.filters = new ArrayList<Filter>();
	}

	@SuppressWarnings("rawtypes")
	public MultiFilter(int numFilters) {
		this.filters = new ArrayList<Filter>(numFilters);
	}

	public void add(Filter<? super T> filter) {
		filters.add(filter);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean pass(T item) {
		for(Filter<T> filter : filters) {
			if(!filter.pass(item)) {
				return false;
			}
		}
		return true;
	}
	
}
