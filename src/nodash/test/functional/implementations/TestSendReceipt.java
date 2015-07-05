package nodash.test.functional.implementations;

import java.security.PublicKey;

import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoInfluence;
import nodash.models.noactiontypes.NoTargetedAction;

public class TestSendReceipt extends NoTargetedAction {
  private static final long serialVersionUID = 1L;
  private String receipt;

  public TestSendReceipt(PublicKey target, String receipt) {
    super(target);
    this.receipt = receipt;
  }

  @Override
  protected NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException {
    System.out.println("Adding receipt influence: " + receipt);
    return new TestAddReceipt(receipt);
  }

  @Override
  public void process() {}

}
