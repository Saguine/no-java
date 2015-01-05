package nodash.test;

import nodash.exceptions.NoDashFatalException;

public class NoTestNotReadyException extends NoDashFatalException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8955061212302042899L;

	public NoTestNotReadyException(String string) {
		super(string);
	}

}
