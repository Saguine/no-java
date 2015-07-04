package nodash.test.functional;

import static org.junit.Assert.*;

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
import nodash.models.NoUser;

import org.junit.Test;

public class NoRoutineTest {

  private byte[] getCopy(byte[] data) {
    return Arrays.copyOf(data, data.length);
  }

  @Test
  public void testUserChangeOwnData() throws NoSessionExpiredException,
      NoSessionConfirmedException, NoSessionNotAwaitingConfirmationException,
      NoUserNotValidException, NoUserAlreadyOnlineException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException, NoAdapterException {
    NoAdapter adapter = new NoDefaultAdapter();
    NoCore core = new NoCore(adapter);
    TestNoUser user = new TestNoUser("username");

    NoRegister registration = core.register(user, "password".toCharArray());
    core.confirm(getCopy(registration.cookie), "password".toCharArray(), getCopy(registration.data));

    byte[] cookie = core.login(getCopy(registration.data), "password".toCharArray());
    TestNoUser userRegistered = (TestNoUser) core.getNoUser(Arrays.copyOf(cookie, cookie.length));
    assertEquals(user, userRegistered);

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
  public static void testUserAction() {
    fail("Not yet implemented.");
  }
  
  @Test
  public static void testUserSourcedAction() {
    fail("Not yet implemented.");
  }
  
  @Test
  public static void testUserTargetedAction() {
    fail("Not yet implemented.");
  }
  
  @Test
  public static void testUserHandshakeAction() {
    fail("Not yet implemented.");
  }
  
  @Test
  public static void testUserErrorableAction() {
    fail("Not yet implemented.");
  }

}
