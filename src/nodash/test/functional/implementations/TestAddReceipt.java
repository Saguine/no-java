package nodash.test.functional.implementations;

import nodash.models.NoInfluence;
import nodash.models.NoUser;

public class TestAddReceipt extends NoInfluence {

  private static final long serialVersionUID = 1L;
  private String receipt;
  
  public TestAddReceipt(String receipt) {
    this.receipt = receipt;
  }
  
  @Override
  public void applyTo(NoUser user) {
    TestNoUser test = (TestNoUser) user;
    System.out.println("Applying " + receipt + " to " + test.getUsername());
    test.addReceipt(receipt);
  }

}
