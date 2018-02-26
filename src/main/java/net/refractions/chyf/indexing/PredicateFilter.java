package net.refractions.chyf.indexing;

import java.util.function.BiPredicate;
import java.util.function.Function;

public class PredicateFilter<T> implements Filter<T> {

	private final Function<? super T, ?> propertyExtractor;
	@SuppressWarnings("rawtypes")
	private final BiPredicate predicate;
	private final Object value; 
	
	public <K extends Comparable<K>> PredicateFilter(Function<? super T, K> propertyExtractor, BiPredicate<K, K> biPredicate, K value) {
		this.propertyExtractor = propertyExtractor;
		this.predicate = biPredicate;
		this.value = value;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean pass(T item) {
		if(propertyExtractor.apply(item) == null) return false;
		return predicate.test(propertyExtractor.apply(item), value);
	}

}
