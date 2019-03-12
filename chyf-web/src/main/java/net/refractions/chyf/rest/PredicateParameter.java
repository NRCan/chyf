/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
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
