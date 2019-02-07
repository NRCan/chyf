package net.refractions.chyf.rest;

import java.util.function.BiPredicate;

public enum PredicateParameter {
	equals((a,b)->a.equals(b)),
	notEquals((a,b)->!a.equals(b)),
	greaterThan((a,b)->a.compareTo(b) > 0),
	lessThan((a,b)->a.compareTo(b) < 0);
	
	@SuppressWarnings("rawtypes")
	private BiPredicate pred;
	
	private <T extends Comparable<T>> PredicateParameter(BiPredicate<T,T> pred) {
		this.pred = pred;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Comparable<T>> BiPredicate<T,T> get() {
		return pred;
	}
	
	public static PredicateParameter convert(String predicateParameter) {
		for(PredicateParameter pred : values()) {
			if(pred.toString().equalsIgnoreCase(predicateParameter)) {
				return pred;
			}
		}
		String errMsg = "The predicate parameter must be one of (equals, lessThan, greaterThan).";
		throw new IllegalArgumentException(errMsg);		
	}


}
