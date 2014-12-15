package nodash.models.noactiontypes;

import java.security.PublicKey;

import nodash.core.NoCore;
import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.exceptions.NoDashFatalException;
import nodash.models.NoAction;
import nodash.models.NoByteSet;
import nodash.models.NoInfluence;

public abstract class NoTargetedAction extends NoAction {
	private static final long serialVersionUID = -8893381130155149646L;
	protected PublicKey target;
	
	protected abstract NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException;
	
	public NoTargetedAction(PublicKey target) {
		this.target = target;
	}
	
	public void execute() {
		NoInfluence influence;
		try {
			influence = this.generateTargetInfluence();
			if (influence != null) {
				NoByteSet byteSet = influence.getByteSet(this.target);
				NoCore.addByteSet(byteSet, this.target);
			}
		} catch (NoCannotGetInfluenceException e) {
			if (e.getResponseInfluence() != null) {
				throw new NoDashFatalException("Unsourced action has generated an error with an undeliverable influence.");
			}
		}
	}
	
	public void purge() {
		this.target = null;
	}
}
