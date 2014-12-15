package nodash.models.noactiontypes;

import java.security.PublicKey;

import nodash.core.NoCore;
import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoByteSet;
import nodash.models.NoInfluence;

public abstract class NoErrorableAction extends NoTargetedAction {
	private static final long serialVersionUID = -6077150774349400823L;

	public NoErrorableAction(PublicKey target) {
		super(target);
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
				NoByteSet byteSet = errorInfluence.getByteSet(this.target);
				NoCore.addByteSet(byteSet, this.target);
			}
		}
	}
}
