package nodash.exceptions;

public class NoByteSetBadDecryptionException extends Exception {
	private static final long serialVersionUID = -8579497499272656543L;

	public NoByteSetBadDecryptionException() {
		super();
	}
	
	public NoByteSetBadDecryptionException(Exception e) {
		super(e);
	}
}
