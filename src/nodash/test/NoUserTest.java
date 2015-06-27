package nodash.test;

import nodash.models.NoUser;

public class NoUserTest extends NoUser {

  /**
	 * 
	 */
  private static final long serialVersionUID = 7791713515804652613L;

  private String changableString;

  public NoUserTest(String changableString) {
    super();
    setChangableString(changableString);
  }

  public String getChangableString() {
    return changableString;
  }

  public void setChangableString(String changableString) {
    this.changableString = changableString;
  }
}
