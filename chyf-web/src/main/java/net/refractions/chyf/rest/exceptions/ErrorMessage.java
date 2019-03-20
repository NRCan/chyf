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
package net.refractions.chyf.rest.exceptions;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public class ErrorMessage {
	
	private String message;
	
	public ErrorMessage(String message) {
		this.message = message;
	}
	
	public ErrorMessage(BindingResult bindingResult) {
		StringBuilder msg = new StringBuilder(
				"Invalid parameter values for the following parameters:");
		for(ObjectError error : bindingResult.getAllErrors()) {
			if(error instanceof FieldError) {
				FieldError fe = (FieldError)error;
				msg.append("<br>Parameter '" + fe.getField()
						+ "', value '" + fe.getRejectedValue() + "'");
			} else {
				msg.append("<br>" + error.getDefaultMessage());
			}
		}
		message = msg.toString();
	}
	
	public String getMessage() {
		return message;
	}
}
