package nodash.models.noactiontypes;

import java.security.PublicKey;

import nodash.core.NoCore;
import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoByteSet;
import nodash.models.NoInfluence;

public abstract class NoHandshakeAction extends NoSourcedAction {	
	private static final long serialVersionUID = 3195466136587475680L;

	protected abstract NoInfluence generateReturnedInfluence();
	
	public NoHandshakeAction(PublicKey target, PublicKey source) {
		super(target, source);
	}
	
	public void execute() {
		try {
			NoInfluence influence = this.generateTargetInfluence();
			if (influence != null) {
				NoByteSet byteSet = influence.getByteSet(this.target);
				NoCore.addByteSet(byteSet, this.target);
			}
			
			NoInfluence result = this.generateReturnedInfluence();
			if (result != null) {
				NoByteSet byteSet = result.getByteSet(this.source);
				NoCore.addByteSet(byteSet, this.source);
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
	}
}
