package nodash.test.functional.implementations;

import java.security.PublicKey;

import nodash.models.NoInfluence;
import nodash.models.NoUser;

public class TestIncreaseMoneySourced extends NoInfluence {
  private static final long serialVersionUID = 1L;
  private int increaseBy;
  private PublicKey source;
  
  public TestIncreaseMoneySourced(PublicKey source, int increaseBy) {
    this.source = source;
    this.increaseBy = increaseBy;
  }

  @Override
  public void applyTo(NoUser user) {
    TestNoUser testUser = (TestNoUser) user;
    testUser.setMoney(testUser.getMoney() + increaseBy);
    testUser.addReceipt("Given money by " + source.toString());
  }

}
