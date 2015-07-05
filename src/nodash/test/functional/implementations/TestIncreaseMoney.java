package nodash.test.functional.implementations;

import nodash.models.NoInfluence;
import nodash.models.NoUser;

public class TestIncreaseMoney extends NoInfluence {
  private static final long serialVersionUID = 1L;
  private int increaseBy;
  
  public TestIncreaseMoney(int increaseBy) {
    this.increaseBy = increaseBy;
  }

  @Override
  public void applyTo(NoUser user) {
    TestNoUser testUser = (TestNoUser) user;
    testUser.setMoney(testUser.getMoney() + increaseBy);
  }

}
