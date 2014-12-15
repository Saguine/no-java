package nodash.exceptions;

public class NoUserNotValidException extends NoDashException {
	private static final long serialVersionUID = -6432604940919299965L;

	public NoUserNotValidException(Exception e) {
		super(e);
	}

	public NoUserNotValidException() {
		
	}
}
