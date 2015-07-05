package nodash.test.functional.implementations;

import java.security.PublicKey;

import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoInfluence;
import nodash.models.noactiontypes.NoHandshakeAction;

public class TestSendFundsReceipted extends NoHandshakeAction {
  private static final long serialVersionUID = 1L;
  private int fundsToSend;

  public TestSendFundsReceipted(PublicKey target, PublicKey source, int fundsToSend) {
    super(target, source);
    this.fundsToSend = fundsToSend;
  }

  @Override
  protected NoInfluence generateReturnedInfluence() {
    return new TestAddReceipt("Sent funds to " + target.toString());
  }

  @Override
  protected NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException {
    System.out.println("Added influence to source.");
    return new TestIncreaseMoneyReceipted(this.source, fundsToSend);
  }

  @Override
  public void process() {}

}
