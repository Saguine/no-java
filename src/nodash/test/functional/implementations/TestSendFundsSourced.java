package nodash.test.functional.implementations;

import java.security.PublicKey;

import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoInfluence;
import nodash.models.noactiontypes.NoSourcedAction;

public class TestSendFundsSourced extends NoSourcedAction {
  private static final long serialVersionUID = 1L;
  private int fundsToSend;

  public TestSendFundsSourced(PublicKey target, PublicKey source, int fundsToSend) {
    super(target, source);
    this.fundsToSend = fundsToSend;
  }

  @Override
  protected NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException {
    return new TestIncreaseMoneySourced(source, fundsToSend);
  }

  @Override
  public void process() {}

}
