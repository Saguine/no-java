package nodash.exceptions;

public class NoDashSessionBadUUID extends Exception {
	private static final long serialVersionUID = -402131397575158344L;

	public NoDashSessionBadUUID() {
		super();
	}
	
	public NoDashSessionBadUUID(Exception e) {
		super(e);
	}
}
