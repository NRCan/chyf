package net.refractions.chyf.rest.exceptions;

public class NotFoundException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	private ErrorMessage errorMessage;
	
	public NotFoundException(String message) {
		errorMessage = new ErrorMessage(message);
	}
	
	public ErrorMessage getErrorMessage() {
		return errorMessage;
	}
	
}
