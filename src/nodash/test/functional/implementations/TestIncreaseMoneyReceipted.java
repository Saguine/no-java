package nodash.test.functional.implementations;

import java.security.PublicKey;

import nodash.models.NoInfluence;
import nodash.models.NoUser;

public class TestIncreaseMoneyReceipted extends NoInfluence {
  private static final long serialVersionUID = 1L;
  private int increaseBy;
  private PublicKey receiptTarget;
  
  public TestIncreaseMoneyReceipted(PublicKey receiptTarget, int increaseBy) {
    this.receiptTarget = receiptTarget;
    this.increaseBy = increaseBy;
  }

  @Override
  public void applyTo(NoUser user) {
    TestNoUser testUser = (TestNoUser) user;
    testUser.setMoney(testUser.getMoney() + increaseBy);
    testUser.addAction(new TestSendReceipt(receiptTarget, "Money received - " + testUser.getUsername()));
  }

}
