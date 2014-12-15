package nodash.exceptions;

public class NoDashTemporaryError extends Exception {
	private static final long serialVersionUID = 7940405670235375662L;

	public NoDashTemporaryError() {
		super();
	}
	
	public NoDashTemporaryError(Exception e) {
		super(e);
	}
}
