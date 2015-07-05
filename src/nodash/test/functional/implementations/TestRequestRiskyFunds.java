package nodash.test.functional.implementations;

import java.security.PublicKey;

import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoInfluence;
import nodash.models.noactiontypes.NoErrorableAction;

public class TestRequestRiskyFunds extends NoErrorableAction {
  private static final long serialVersionUID = 1L;

  public TestRequestRiskyFunds(PublicKey source) {
    super(source);
  }

  @Override
  protected NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException {
    throw new NoCannotGetInfluenceException(new TestAddReceipt("Could not request."));
  }

  @Override
  public void process() {}

}
