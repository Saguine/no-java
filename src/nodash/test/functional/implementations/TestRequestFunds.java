package nodash.test.functional.implementations;

import java.security.PublicKey;

import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoInfluence;
import nodash.models.noactiontypes.NoErrorableAction;
import nodash.models.noactiontypes.NoSourcedAction;

public class TestRequestFunds extends NoErrorableAction {
  private static final long serialVersionUID = 1L;

  public TestRequestFunds(PublicKey source) {
    super(source);
  }

  @Override
  protected NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException {
    return new TestIncreaseMoney(100);
  }

  @Override
  public void process() {}

}
