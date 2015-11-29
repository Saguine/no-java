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

import java.util.Arrays;

import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoSession;
import nodash.models.NoSession.NoState;
import nodash.models.NoUser;
import nodash.test.functional.implementations.TestNoUser;

import org.junit.Test;

public class NoSessionTest {

  @Test
  public void testNoSessionNoUser() throws NoSessionConfirmedException, NoSessionExpiredException {
    NoUser user = new TestNoUser("Test");
    NoSession session = new NoSession(user);
    assertNotNull(session.getNoUser());
    assertNotNull(session.getUuid());
    assertNotNull(session.getEncryptedUuid());
    assertNull(session.getIncoming());
    assertNull(session.getOriginalHash());
    assertEquals(session.getNoUser(), user);
    assertEquals(session.getNoState(), NoState.MODIFIED);
    
    try {
      new NoSession(null);
      fail("Did not throw NullPointerException when given a null user.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }
  @Test
  public void testNoSessionByteArrayCharArray() throws NoUserNotValidException,
      NoSessionExpiredException, NoSessionConfirmedException {
    NoUser user = new TestNoUser("Test");
    final byte[] userFile1 = user.createFile("password".toCharArray());
    byte[] userFile2 = Arrays.copyOf(userFile1, userFile1.length);
    char[] userPassword = "password".toCharArray();
    NoSession session = new NoSession(userFile2, userPassword);
    assertFalse(Arrays.equals(userFile1, userFile2));
    assertFalse(Arrays.equals("password".toCharArray(), userPassword));
    assertNotNull(session.getNoUser());
    assertNotNull(session.getOriginalHash());
    assertNotNull(session.getUuid());
    assertNull(session.getIncoming());
    assertEquals(session.getNoUser(), user);
    assertEquals(session.getNoState(), NoState.IDLE);
    
    byte[] badUserFile = Arrays.copyOf(userFile1, userFile1.length);
    badUserFile[0] = (byte) (badUserFile[0] == 'A' ? 'B' : 'A');
    try {
      new NoSession(badUserFile, "password".toCharArray());
      fail("Did not throw NoUserNotValidException when given bad file.");
    } catch (NoUserNotValidException e) {
      // Do nothing, correct
    }
    
    try {
      new NoSession(Arrays.copyOf(userFile2, userFile2.length), "badpassword".toCharArray());
      fail("Did not throw NoUserNotValidException when given bad password.");
    } catch (NoUserNotValidException e) {
      // Do nothing, correct
    }
  }

}
