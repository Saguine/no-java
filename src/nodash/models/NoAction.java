package nodash.models;

import java.io.Serializable;

public abstract class NoAction implements Serializable {
	private static final long serialVersionUID = -194752850197321803L;
	public abstract void execute();
	public abstract void purge();
}
