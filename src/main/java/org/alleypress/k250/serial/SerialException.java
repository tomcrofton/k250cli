package org.alleypress.k250.serial;

public class SerialException extends Exception {
	private static final long serialVersionUID = 1984L;

	public SerialException() {
		super();
	}

	public SerialException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SerialException(String message, Throwable cause) {
		super(message, cause);
	}

	public SerialException(String message) {
		super(message);
	}

	public SerialException(Throwable cause) {
		super(cause);
	}

}
