/*
 * Copyright 2014 David Horscroft
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * 
 * The NoCore class is the interface between which the wrapper application (wrapplication?) accesses
 * no- functionality.
 */

package nodash.test;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import nodash.core.NoAdapter;
import nodash.core.NoCore;
import nodash.core.NoDefaultAdapter;
import nodash.exceptions.NoAdapterException;
import nodash.exceptions.NoDashFatalException;
import nodash.exceptions.NoDashSessionBadUuidException;
import nodash.exceptions.NoSessionAlreadyAwaitingConfirmationException;
import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.exceptions.NoSessionNotAwaitingConfirmationException;
import nodash.exceptions.NoSessionNotChangedException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoRegister;
import nodash.models.NoUser;
import nodash.test.functional.implementations.TestNoUser;
import nodash.models.NoSession.NoState;

import org.junit.Test;

public class NoCoreTest {

  @Test
  public void testRegister() {
    NoCore core = new NoCore(new NoDefaultAdapter());

    NoUser user1 = new TestNoUser("Test");
    NoRegister registration1 = core.register(user1, "password".toCharArray());
    assertNotNull(registration1.cookie);
    assertNotNull(registration1.data);

    NoUser user2 = new TestNoUser("Test");
    NoRegister registration2 = core.register(user2, "password".toCharArray());

    assertFalse(Arrays.equals(registration1.cookie, registration2.cookie));
    assertFalse(Arrays.equals(registration1.data, registration2.data));

    NoUser user3 = new TestNoUser("Test");
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
      NoDashSessionBadUuidException, NoUserAlreadyOnlineException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException, NoAdapterException, NoSuchMethodException,
      SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    NoAdapter adapter = new NoDefaultAdapter();
    NoCore core = new NoCore(adapter);

    NoUser newUser = new TestNoUser("Test");
    NoRegister registration = core.register(newUser, "password".toCharArray());
    byte[] newUserFile = Arrays.copyOf(registration.data, registration.data.length);
    core.confirm(registration.cookie, "password".toCharArray(), newUserFile);
    byte[] newUserHash = newUser.createHash();
    adapter.checkHash(newUserHash);

    NoUser newUserBadPass = new TestNoUser("Test");
    registration = core.register(newUserBadPass, "password".toCharArray());
    byte[] newUserBadPassFile = Arrays.copyOf(registration.data, registration.data.length);
    try {
      core.confirm(registration.cookie, "badpassword".toCharArray(), newUserBadPassFile);
      fail("Confirmed with a bad password without throwing an exception.");
    } catch (NoUserNotValidException e) {
      // Do nothing, true
    }

    byte[] badCookie = Arrays.copyOf(registration.cookie, registration.cookie.length);
    badCookie[0] = (byte) (badCookie[0] == 'A' ? 'B' : 'A');
    try {
      core.confirm(badCookie, "password".toCharArray(), newUserBadPassFile);
      fail("Confirmed on bad cookie without throwing fatal exception.");
    } catch (NoSessionExpiredException e) {
      // Do nothing, correct
    }
    
    try {
      core.confirm(new byte[] {'b', 'a', 'd', 'c', 'o', 'o', 'k', 'i', 'e'},
          "password".toCharArray(), newUserBadPassFile);
      fail("Confirmed on bad cookie without throwing exception.");
    } catch (NoDashFatalException e) {
      // Do nothing, true
    }

    NoUser oldUser = new TestNoUser("Test");
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
    
    Method touchRandomizer = NoUser.class.getDeclaredMethod("touchRandomizer");
    touchRandomizer.setAccessible(true);
    touchRandomizer.invoke(adapter.getNoSession(oldUserCookie).getNoUser());
    
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
  public void testGetUser() throws NoSessionExpiredException, NoSessionConfirmedException,
      NoSessionNotAwaitingConfirmationException, NoUserNotValidException,
      NoUserAlreadyOnlineException {
    NoCore core = new NoCore(new NoDefaultAdapter());
    NoUser user = new TestNoUser("Test");
    NoRegister registration = core.register(user, "password".toCharArray());
    byte[] file = Arrays.copyOf(registration.data, registration.data.length);
    core.confirm(registration.cookie, "password".toCharArray(), file);

    file = Arrays.copyOf(registration.data, registration.data.length);
    byte[] cookie = core.login(file, "password".toCharArray());
    byte[] badCookie = Arrays.copyOf(cookie, cookie.length);
    badCookie[0] = (byte) (badCookie[0] == 'A' ? 'B' : 'A');
    NoUser user2 = core.getNoUser(cookie);
    assertNotNull(user2);
    assertEquals(user, user2);

    try {
      core.getNoUser(badCookie);
      fail("Did not fail when given a bad cookie.");
    } catch (NoSessionExpiredException e) {
      // Correct, do nothing.
    }
  }

  @Test
  public void testGetSessionState() throws NoSessionExpiredException, NoSessionConfirmedException,
      NoSessionNotAwaitingConfirmationException, NoUserNotValidException,
      NoUserAlreadyOnlineException, NoSessionNotChangedException, NoSessionAlreadyAwaitingConfirmationException {
    NoCore core = new NoCore(new NoDefaultAdapter());
    NoUser user = new TestNoUser("Test");
    NoRegister registration = core.register(user, "password".toCharArray());
    assertEquals(core.getSessionState(registration.cookie), NoState.AWAITING_CONFIRMATION);
    byte[] file = Arrays.copyOf(registration.data, registration.data.length);
    core.confirm(registration.cookie, "password".toCharArray(), file);

    file = Arrays.copyOf(registration.data, registration.data.length);
    byte[] cookie = core.login(file, "password".toCharArray());
    assertEquals(core.getSessionState(cookie), NoState.IDLE);
    user = core.getNoUser(cookie);
    user.createFile("password".toCharArray()); // touch randomizer
    assertEquals(core.getSessionState(cookie), NoState.MODIFIED);
    
    file = core.save(cookie, "password".toCharArray());
    assertEquals(core.getSessionState(cookie), NoState.AWAITING_CONFIRMATION);
    
    core.confirm(cookie, "password".toCharArray(), file);
    try {
      core.getSessionState(cookie);
      fail("Didn't fail on getting session after confirm.");
    } catch (NoSessionExpiredException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testShred() throws NoAdapterException, NoSessionConfirmedException, NoSessionExpiredException {
    NoAdapter adapter = new NoDefaultAdapter();
    NoCore core = new NoCore(adapter);
    NoUser user = new TestNoUser("Test");
    NoRegister registration = core.register(user, "password".toCharArray());
    assertTrue(adapter.isOnline(user.createHash()));
    assertEquals(core.getSessionState(registration.cookie), NoState.AWAITING_CONFIRMATION);
    
    core.shred(registration.cookie);
    
    assertFalse(adapter.isOnline(user.createHash()));
    try {
      core.getNoUser(registration.cookie);
      fail("Returned a user object after shredding.");
    } catch (NoSessionExpiredException e) {
      // Do nothing, correct
    }
  }

}
