package nodash.exceptions;

public class NoDashFatalException extends RuntimeException {	
	private static final long serialVersionUID = -8254102569327237811L;

	public NoDashFatalException(Exception e) {
		super(e);
	}

	public NoDashFatalException(String string) {
		super(string);
	}
}
