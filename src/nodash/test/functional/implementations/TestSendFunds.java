package nodash.test.functional.implementations;

import java.security.PublicKey;

import nodash.models.NoInfluence;
import nodash.models.noactiontypes.NoTargetedAction;

public class TestSendFunds extends NoTargetedAction {
  private static final long serialVersionUID = 1L;
  private int fundsToSend;
  
  public TestSendFunds(PublicKey target, int fundsToSend) {
    super(target);
    this.fundsToSend = fundsToSend;
  }

  @Override
  protected NoInfluence generateTargetInfluence() {
    return new TestIncreaseMoney(fundsToSend);
  }

  @Override
  public void process() {}

}
