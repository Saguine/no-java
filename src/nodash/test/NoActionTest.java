package nodash.test;

import java.security.PublicKey;

import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoInfluence;
import nodash.models.noactiontypes.NoTargetedAction;

public class NoActionTest extends NoTargetedAction {
  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;

  private String newName;

  public NoActionTest(PublicKey target, String newName) {
    super(target);
    this.newName = newName;
  }

  @Override
  protected NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException {
    return new NoInfluenceTest(newName);
  }

  @Override
  public void process() {
    // Nothing;
  }

}
