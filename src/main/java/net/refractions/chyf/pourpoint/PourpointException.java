package net.refractions.chyf.pourpoint;

import org.springframework.validation.BindingResult;

public class PourpointException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PourpointException(String message) {
		super(message);
	}
	
	public PourpointException(String message, BindingResult result) {
		super(message);
	}
	
}
