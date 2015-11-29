package nodash.test.functional.implementations;

import java.util.ArrayList;
import java.util.List;

import nodash.models.NoHash;
import nodash.models.NoUser;

public class TestNoUser extends NoUser {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  @NoHash
  private String username;
  
  @NoHash
  private int money;
  
  @NoHash
  private List<String> receipts;
  
  public TestNoUser(String username) {
    super();
    this.username = username;
    this.money = 0;
    this.receipts = new ArrayList<String>();
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
  
  public void addReceipt(String receipt) {
    this.receipts.add(receipt);
  }
  
  public List<String> getReceipts() {
    return receipts;
  }
  
}
