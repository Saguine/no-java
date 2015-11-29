package nodash.test.functional;

import static org.junit.Assert.*;

import java.security.PublicKey;
import java.util.Arrays;

import nodash.core.NoAdapter;
import nodash.core.NoCore;
import nodash.core.NoDefaultAdapter;
import nodash.exceptions.NoAdapterException;
import nodash.exceptions.NoSessionAlreadyAwaitingConfirmationException;
import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.exceptions.NoSessionNotAwaitingConfirmationException;
import nodash.exceptions.NoSessionNotChangedException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoRegister;
import nodash.test.functional.implementations.TestJustTouchStaticField;
import nodash.test.functional.implementations.TestNoUser;
import nodash.test.functional.implementations.TestRequestFunds;
import nodash.test.functional.implementations.TestRequestRiskyFunds;
import nodash.test.functional.implementations.TestSendFunds;
import nodash.test.functional.implementations.TestSendFundsReceipted;
import nodash.test.functional.implementations.TestSendFundsSourced;

import org.junit.Test;

public class NoRoutineTest {

  private byte[] getCopy(byte[] data) {
    return Arrays.copyOf(data, data.length);
  }

  private byte[] registerAndConfirm(TestNoUser user, String password)
      throws NoSessionExpiredException, NoSessionConfirmedException,
      NoSessionNotAwaitingConfirmationException, NoUserNotValidException {
    NoCore core = new NoCore(new NoDefaultAdapter(), TestNoUser.class);
    NoRegister registration = core.register(user, password.toCharArray());
    core.confirm(getCopy(registration.cookie), password.toCharArray(), getCopy(registration.data));
    return getCopy(registration.data);
  }

  @Test
  public void testUserChangeOwnData() throws NoSessionExpiredException,
      NoSessionConfirmedException, NoSessionNotAwaitingConfirmationException,
      NoUserNotValidException, NoUserAlreadyOnlineException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException, NoAdapterException {
    final byte[] userFile = registerAndConfirm(new TestNoUser("username"), "password");

    NoAdapter adapter = new NoDefaultAdapter();
    NoCore core = new NoCore(adapter, TestNoUser.class);

    byte[] cookie = core.login(getCopy(userFile), "password".toCharArray());
    TestNoUser userRegistered = (TestNoUser) core.getNoUser(Arrays.copyOf(cookie, cookie.length));
    assertEquals("username", userRegistered.getUsername());

    userRegistered.setUsername("newsername");
    byte[] data = core.save(getCopy(cookie), "password2".toCharArray());
    core.confirm(getCopy(cookie), "password2".toCharArray(), getCopy(data));

    try {
      core.login(getCopy(data), "password-bad".toCharArray());
      fail("Did not throw exception on login with bad password.");
    } catch (NoUserNotValidException e) {
      // Do nothing, correct
    }

    cookie = core.login(getCopy(data), "password2".toCharArray());
    TestNoUser userChanged = (TestNoUser) core.getNoUser(getCopy(cookie));

    assertEquals("newsername", userChanged.getUsername());
    assertEquals(0, userChanged.getMoney());
    assertTrue(adapter.isOnline(userChanged.createHash()));

    core.shred(getCopy(cookie));
    assertFalse(adapter.isOnline(userChanged.createHash()));
  }

  @Test
  public void testUserAction() throws NoSessionExpiredException, NoSessionConfirmedException,
      NoSessionNotAwaitingConfirmationException, NoUserNotValidException,
      NoUserAlreadyOnlineException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException { // testing TestJustTouchStaticField
    byte[] userFile = registerAndConfirm(new TestNoUser("username"), "password");
    NoCore core = new NoCore(new NoDefaultAdapter(), TestNoUser.class);

    int touchCount = TestJustTouchStaticField.touchMe;

    byte[] noSaveCookie = core.login(getCopy(userFile), "password".toCharArray());
    TestNoUser noSaveUser = (TestNoUser) core.getNoUser(getCopy(noSaveCookie));
    noSaveUser.addAction(new TestJustTouchStaticField());
    core.shred(noSaveCookie);
    // Assert action did not occur at shred.
    assertTrue(TestJustTouchStaticField.touchMe == touchCount);

    byte[] noConfirmCookie = core.login(getCopy(userFile), "password".toCharArray());
    TestNoUser noConfirmUser = (TestNoUser) core.getNoUser(getCopy(noConfirmCookie));
    noConfirmUser.addAction(new TestJustTouchStaticField());
    core.save(getCopy(noConfirmCookie), "password".toCharArray());
    // Assert action did not occur at save
    assertTrue(TestJustTouchStaticField.touchMe == touchCount);
    core.shred(getCopy(noConfirmCookie));

    byte[] confirmCookie = core.login(getCopy(userFile), "password".toCharArray());
    TestNoUser confirmUser = (TestNoUser) core.getNoUser(getCopy(confirmCookie));
    confirmUser.addAction(new TestJustTouchStaticField());
    byte[] saveConfirmFile = core.save(getCopy(confirmCookie), "password".toCharArray());
    core.confirm(getCopy(confirmCookie), "password".toCharArray(), getCopy(saveConfirmFile));
    // Assert action fired after confirm
    assertTrue(TestJustTouchStaticField.touchMe > touchCount);
  }

  @Test
  public void testUserErrorableActionWithoutError() throws NoSessionExpiredException,
      NoSessionConfirmedException, NoSessionNotAwaitingConfirmationException,
      NoUserNotValidException, NoUserAlreadyOnlineException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException { // testing TestRequestFunds
    byte[] requesterFile = registerAndConfirm(new TestNoUser("requester"), "password");
    NoCore core = new NoCore(new NoDefaultAdapter(), TestNoUser.class);

    byte[] requesterCookie = core.login(getCopy(requesterFile), "password".toCharArray());
    TestNoUser requester = (TestNoUser) core.getNoUser(getCopy(requesterCookie));
    requester.addAction(new TestRequestFunds(requester.getRsaPublicKey()));
    byte[] confirmFile = core.save(getCopy(requesterCookie), "password".toCharArray());
    core.confirm(getCopy(requesterCookie), "password".toCharArray(), getCopy(confirmFile));

    requesterCookie = core.login(getCopy(confirmFile), "password".toCharArray());
    requester = (TestNoUser) core.getNoUser(getCopy(requesterCookie));
    assertTrue(requester.getMoney() == 100);
  }

  @Test
  public void testUserErrorableActionWithError() throws NoSessionExpiredException,
      NoSessionConfirmedException, NoSessionNotAwaitingConfirmationException,
      NoUserNotValidException, NoUserAlreadyOnlineException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException { // testing TestRequestFunds
    byte[] requesterFile = registerAndConfirm(new TestNoUser("requester"), "password");
    NoCore core = new NoCore(new NoDefaultAdapter(), TestNoUser.class);

    byte[] requesterCookie = core.login(getCopy(requesterFile), "password".toCharArray());
    TestNoUser requester = (TestNoUser) core.getNoUser(getCopy(requesterCookie));
    requester.addAction(new TestRequestRiskyFunds(requester.getRsaPublicKey()));
    byte[] confirmFile = core.save(getCopy(requesterCookie), "password".toCharArray());
    core.confirm(getCopy(requesterCookie), "password".toCharArray(), getCopy(confirmFile));
    assertTrue(requester.getMoney() == 0);
    assertTrue(requester.getReceipts().size() == 0);

    requesterCookie = core.login(getCopy(confirmFile), "password".toCharArray());
    requester = (TestNoUser) core.getNoUser(getCopy(requesterCookie));
    assertTrue(requester.getMoney() == 0);
    assertTrue(requester.getReceipts().size() == 1);
    assertEquals(requester.getReceipts().get(0), "Could not request.");
  }

  @Test
  public void testUserTargetedAction() throws NoSessionExpiredException,
      NoSessionConfirmedException, NoSessionNotAwaitingConfirmationException,
      NoUserNotValidException, NoUserAlreadyOnlineException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException { // testing TestSendFunds
    byte[] fundSenderFile = registerAndConfirm(new TestNoUser("fund-sender"), "password1");
    byte[] fundGetterFile = registerAndConfirm(new TestNoUser("fund-getter"), "password2");
    NoCore core = new NoCore(new NoDefaultAdapter(), TestNoUser.class);

    byte[] senderCookie = core.login(getCopy(fundSenderFile), "password1".toCharArray());
    TestNoUser sender = (TestNoUser) core.getNoUser(senderCookie);
    sender.setMoney(1000);
    fundSenderFile = core.save(getCopy(senderCookie), "password1".toCharArray());
    core.confirm(getCopy(senderCookie), "password1".toCharArray(), getCopy(fundSenderFile));
    sender = null;

    byte[] getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    TestNoUser getter = (TestNoUser) core.getNoUser(getterCookie);
    PublicKey getterAddress = getter.getRsaPublicKey();
    core.shred(getterCookie);
    getter = null;

    senderCookie = core.login(getCopy(fundSenderFile), "password1".toCharArray());
    sender = (TestNoUser) core.getNoUser(getCopy(senderCookie));
    sender.setMoney(500);
    sender.addAction(new TestSendFunds(getterAddress, 500));
    fundSenderFile = core.save(getCopy(senderCookie), "password1".toCharArray());
    core.confirm(getCopy(senderCookie), "password1".toCharArray(), getCopy(fundSenderFile));

    getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    getter = (TestNoUser) core.getNoUser(getCopy(getterCookie));
    assertTrue(getter.getMoney() == 500);
    core.shred(getCopy(getterCookie));

    getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    getter = (TestNoUser) core.getNoUser(getCopy(getterCookie));
    assertTrue(getter.getMoney() == 500);
    fundGetterFile = core.save(getCopy(getterCookie), "password2".toCharArray());
    core.confirm(getCopy(getterCookie), "password2".toCharArray(), getCopy(fundGetterFile));

    getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    getter = (TestNoUser) core.getNoUser(getCopy(getterCookie));
    assertTrue(getter.getMoney() == 500);
    core.shred(getCopy(getterCookie));

    senderCookie = core.login(getCopy(fundSenderFile), "password1".toCharArray());
    sender = (TestNoUser) core.getNoUser(getCopy(senderCookie));
    assertTrue(sender.getMoney() == 500);
  }

  @Test
  public void testUserHandshakeAction() throws NoUserNotValidException,
      NoUserAlreadyOnlineException, NoSessionExpiredException, NoSessionConfirmedException,
      NoSessionNotAwaitingConfirmationException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException { // testing TestSendFundsReceipted
    byte[] fundSenderFile = registerAndConfirm(new TestNoUser("fund-sender"), "password1");
    byte[] fundGetterFile = registerAndConfirm(new TestNoUser("fund-getter"), "password2");
    NoCore core = new NoCore(new NoDefaultAdapter(), TestNoUser.class);

    byte[] senderCookie = core.login(getCopy(fundSenderFile), "password1".toCharArray());
    TestNoUser sender = (TestNoUser) core.getNoUser(senderCookie);
    sender.setMoney(1000);
    fundSenderFile = core.save(getCopy(senderCookie), "password1".toCharArray());
    core.confirm(getCopy(senderCookie), "password1".toCharArray(), getCopy(fundSenderFile));
    sender = null;

    byte[] getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    TestNoUser getter = (TestNoUser) core.getNoUser(getterCookie);
    PublicKey getterAddress = getter.getRsaPublicKey();
    core.shred(getterCookie);
    getter = null;

    senderCookie = core.login(getCopy(fundSenderFile), "password1".toCharArray());
    sender = (TestNoUser) core.getNoUser(getCopy(senderCookie));
    sender.setMoney(500);
    sender.addAction(new TestSendFundsReceipted(getterAddress, sender.getRsaPublicKey(), 500));
    fundSenderFile = core.save(getCopy(senderCookie), "password1".toCharArray());
    core.confirm(getCopy(senderCookie), "password1".toCharArray(), getCopy(fundSenderFile));

    getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    getter = (TestNoUser) core.getNoUser(getCopy(getterCookie));
    assertTrue(getter.getMoney() == 500);
    assertTrue(getter.getNoActions().size() == 1);
    core.shred(getCopy(getterCookie));

    getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    getter = (TestNoUser) core.getNoUser(getCopy(getterCookie));
    assertTrue(getter.getMoney() == 500);
    assertTrue(getter.getNoActions().size() == 1);
    fundGetterFile = core.save(getCopy(getterCookie), "password2".toCharArray());
    core.confirm(getCopy(getterCookie), "password2".toCharArray(), getCopy(fundGetterFile));

    getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    getter = (TestNoUser) core.getNoUser(getCopy(getterCookie));
    assertTrue(getter.getMoney() == 500);
    assertTrue(getter.getNoActions().size() == 0);
    assertTrue(getter.getReceipts().size() == 0);
    core.shred(getCopy(getterCookie));

    senderCookie = core.login(getCopy(fundSenderFile), "password1".toCharArray());
    sender = (TestNoUser) core.getNoUser(getCopy(senderCookie));
    assertTrue(sender.getMoney() == 500);
    assertTrue(sender.getReceipts().size() == 2);
    assertEquals(sender.getReceipts().get(0), "Sent funds to " + getterAddress.toString());
    assertEquals(sender.getReceipts().get(1), "Money received - fund-getter");
  }

  @Test
  public void testUserSourcedAction() throws NoSessionExpiredException,
      NoSessionConfirmedException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException, NoSessionNotAwaitingConfirmationException,
      NoUserNotValidException, NoUserAlreadyOnlineException { // testing TestSendFundsSourced
    byte[] fundSenderFile = registerAndConfirm(new TestNoUser("fund-sender"), "password1");
    byte[] fundGetterFile = registerAndConfirm(new TestNoUser("fund-getter"), "password2");
    NoCore core = new NoCore(new NoDefaultAdapter(), TestNoUser.class);

    byte[] senderCookie = core.login(getCopy(fundSenderFile), "password1".toCharArray());
    TestNoUser sender = (TestNoUser) core.getNoUser(senderCookie);
    sender.setMoney(1000);
    fundSenderFile = core.save(getCopy(senderCookie), "password1".toCharArray());
    core.confirm(getCopy(senderCookie), "password1".toCharArray(), getCopy(fundSenderFile));
    sender = null;

    byte[] getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    TestNoUser getter = (TestNoUser) core.getNoUser(getterCookie);
    PublicKey getterAddress = getter.getRsaPublicKey();
    core.shred(getterCookie);
    getter = null;

    senderCookie = core.login(getCopy(fundSenderFile), "password1".toCharArray());
    sender = (TestNoUser) core.getNoUser(getCopy(senderCookie));
    sender.setMoney(500);
    sender.addAction(new TestSendFundsSourced(getterAddress, sender.getRsaPublicKey(), 500));
    fundSenderFile = core.save(getCopy(senderCookie), "password1".toCharArray());
    core.confirm(getCopy(senderCookie), "password1".toCharArray(), getCopy(fundSenderFile));

    getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    getter = (TestNoUser) core.getNoUser(getCopy(getterCookie));
    assertTrue(getter.getMoney() == 500);
    assertTrue(getter.getReceipts().size() == 1);
    assertEquals(getter.getReceipts().get(0), "Given money by " + sender.getRsaPublicKey().toString());
    assertTrue(getter.getNoActions().size() == 0);
    core.shred(getCopy(getterCookie));

    getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    getter = (TestNoUser) core.getNoUser(getCopy(getterCookie));
    assertTrue(getter.getMoney() == 500);
    assertTrue(getter.getReceipts().size() == 1);
    assertEquals(getter.getReceipts().get(0), "Given money by " + sender.getRsaPublicKey().toString());
    assertTrue(getter.getNoActions().size() == 0);
    fundGetterFile = core.save(getCopy(getterCookie), "password2".toCharArray());
    core.confirm(getCopy(getterCookie), "password2".toCharArray(), getCopy(fundGetterFile));

    getterCookie = core.login(getCopy(fundGetterFile), "password2".toCharArray());
    getter = (TestNoUser) core.getNoUser(getCopy(getterCookie));
    assertTrue(getter.getMoney() == 500);
    assertTrue(getter.getNoActions().size() == 0);
    assertTrue(getter.getReceipts().size() == 1);
    core.shred(getCopy(getterCookie));

    senderCookie = core.login(getCopy(fundSenderFile), "password1".toCharArray());
    sender = (TestNoUser) core.getNoUser(getCopy(senderCookie));
    assertTrue(sender.getMoney() == 500);
    assertTrue(sender.getReceipts().size() == 0);
  }

}
