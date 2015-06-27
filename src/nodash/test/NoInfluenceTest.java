package nodash.test;

import nodash.models.NoInfluence;
import nodash.models.NoUser;

public class NoInfluenceTest extends NoInfluence {
  /**
	 * 
	 */
  private static final long serialVersionUID = 5710677031891178814L;
  private String newName;

  public NoInfluenceTest(String newName) {
    super();
    this.newName = newName;
  }

  @Override
  public void applyTo(NoUser user) {
    ((NoUserTest) user).setChangableString(this.newName);
  }

}
