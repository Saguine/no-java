package nodash.exceptions;

import nodash.models.NoInfluence;

public class NoCannotGetInfluenceException extends NoDashException {
	private static final long serialVersionUID = 4581361079067540974L;
	
	private NoInfluence returnable;
	
	public NoCannotGetInfluenceException(NoInfluence returnable) {
		super();
		this.returnable = returnable;
	}
	
	public NoInfluence getResponseInfluence() {
		return returnable;
	}
}
