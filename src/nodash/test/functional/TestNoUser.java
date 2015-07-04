package nodash.test.functional;

import nodash.models.NoUser;

public class TestNoUser extends NoUser {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private String username;
  private int money;
  
  public TestNoUser(String username) {
    super();
    this.username = username;
    this.money = 0;
  }

  public String getUsername() {
    return username;
  }

  public int getMoney() {
    return money;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setMoney(int money) {
    this.money = money;
  }
  
}
