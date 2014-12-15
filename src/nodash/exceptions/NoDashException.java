package nodash.exceptions;

public class NoDashException extends Exception {
	private static final long serialVersionUID = -8579497499272656543L;

	public NoDashException() {
		super();
	}
	
	public NoDashException(Exception e) {
		super(e);
	}
}
