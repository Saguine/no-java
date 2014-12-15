package nodash.models.noactiontypes;

import java.security.PublicKey;

import nodash.core.NoCore;
import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoByteSet;
import nodash.models.NoInfluence;

public abstract class NoSourcedAction extends NoTargetedAction {	
	private static final long serialVersionUID = -2996690472537380062L;
	protected PublicKey source;
	protected abstract NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException;
	
	public NoSourcedAction(PublicKey target, PublicKey source) {
		super(target);
		this.source = source;
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
			NoInfluence errorInfluence = e.getResponseInfluence();
			if (errorInfluence != null) {
				NoByteSet byteSet = errorInfluence.getByteSet(this.source);
				NoCore.addByteSet(byteSet, this.source);
			}
		}
	}
	
	public void purge() {
		super.purge();
		this.source = null;
	}
}
