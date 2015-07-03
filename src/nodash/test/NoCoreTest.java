package nodash.test;

import static org.junit.Assert.*;

import java.util.Arrays;

import nodash.core.NoAdapter;
import nodash.core.NoCore;
import nodash.core.NoDefaultAdapter;
import nodash.core.NoRegister;
import nodash.exceptions.NoAdapterException;
import nodash.exceptions.NoDashSessionBadUuidException;
import nodash.exceptions.NoSessionAlreadyAwaitingConfirmationException;
import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.exceptions.NoSessionNotAwaitingConfirmationException;
import nodash.exceptions.NoSessionNotChangedException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoUser;

import org.junit.Test;

public class NoCoreTest {

  @Test
  public void testRegister() {
    NoCore core = new NoCore(new NoDefaultAdapter());

    NoUser user1 = new NoUser();
    NoRegister registration1 = core.register(user1, "password".toCharArray());
    assertNotNull(registration1.cookie);
    assertNotNull(registration1.data);

    NoUser user2 = new NoUser();
    NoRegister registration2 = core.register(user2, "password".toCharArray());

    assertFalse(Arrays.equals(registration1.cookie, registration2.cookie));
    assertFalse(Arrays.equals(registration2.data, registration2.data));

    NoUser user3 = new NoUser();
    try {
      core.register(null, "password".toCharArray());
      fail("Did not throw NullPointerException.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }

    try {
      core.register(user3, null);
      fail("Did not throw NullPointerException.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }

    try {
      core.register(null, null);
      fail("Did not throw NullPointerException.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testSaveAndConfirm() throws NoSessionExpiredException, NoSessionConfirmedException,
      NoSessionNotAwaitingConfirmationException, NoUserNotValidException,
      NoDashSessionBadUuidException, NoUserAlreadyOnlineException, NoSessionNotChangedException, NoSessionAlreadyAwaitingConfirmationException, NoAdapterException {
    NoAdapter adapter = new NoDefaultAdapter();
    NoCore core = new NoCore(adapter);

    NoUser newUser = new NoUser();
    NoRegister registration = core.register(newUser, "password".toCharArray());
    byte[] newUserFile = Arrays.copyOf(registration.data, registration.data.length);
    core.confirm(registration.cookie, "password".toCharArray(), newUserFile);
    byte[] newUserHash = newUser.createHash();
    adapter.checkHash(newUserHash);
    
    NoUser newUserBadPass = new NoUser();
    registration = core.register(newUserBadPass, "password".toCharArray());
    byte[] newUserBadPassFile = Arrays.copyOf(registration.data, registration.data.length);
    try {
      core.confirm(registration.cookie, "badpassword".toCharArray(), newUserBadPassFile);
      fail("Confirmed with a bad password without throwing an exception.");
    } catch (NoUserNotValidException e) {
      // Do nothing, true 
    }
    
    try {
      core.confirm(new byte[] {'b', 'a', 'd', 'c', 'o', 'o', 'k', 'i', 'e'}, "password".toCharArray(), 
          newUserBadPassFile);
      fail("Confirmed on bad cookie without throwing exception.");
    } catch (NoSessionExpiredException e) {
      // Do nothing, true
    }
    
    NoUser oldUser = new NoUser();
    registration = core.register(oldUser, "password".toCharArray());
    byte[] oldUserFile = Arrays.copyOf(registration.data, registration.data.length);
    core.confirm(registration.cookie, "password".toCharArray(), oldUserFile);
    
    oldUserFile = Arrays.copyOf(registration.data, registration.data.length);
    byte[] oldUserCookie = core.login(oldUserFile, "password".toCharArray());
    assertNotNull(adapter.getNoSession(oldUserCookie));
    oldUser.createFile("password".toCharArray()); // Touch the randomizer
    
    NoUser oldUserRevisited = core.getNoUser(oldUserCookie);
    byte[] currentHash = oldUserRevisited.createHash();
    oldUserRevisited.createFile("password".toCharArray());
    byte[] oldCreatedFile = core.save(oldUserCookie, "new-password".toCharArray());
    byte[] oldUserHash = oldUserRevisited.createHash();
    core.confirm(oldUserCookie, "new-password".toCharArray(), oldCreatedFile);
    assertFalse(adapter.containsNoSession(oldUserCookie));
    adapter.checkHash(oldUserHash);
    try {
      adapter.checkHash(currentHash);
      fail("Did not fail on checkhash.");
    } catch (NoUserNotValidException e) {
      // Correct, do nothing
    }
  }


  @Test
  public void testGetUser() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetSessionState() {
    fail("Not yet implemented");
  }

  @Test
  public void testShred() {
    fail("Not yet implemented");
  }

}
