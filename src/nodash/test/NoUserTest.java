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

import java.io.IOException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import nodash.core.NoUtil;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoUser;
import nodash.test.functional.implementations.TestNoUser;

import org.junit.Test;

public class NoUserTest {

  @Test
  public void testNoUser() {
    NoUser user = new TestNoUser("Test");

    assertNotNull(user.getNoActions());
    assertEquals(user.getNoActions().size(), 0);
    assertNotNull(user.getRsaPublicKey());
    assertNotNull(user.getPublicExponent());
    assertNotNull(user.getModulus());
    assertEquals(user.getInfluences(), 0);
  }

  @Test
  public void testCreateFile() {
    NoUser user = new TestNoUser("Test");
    byte[] file = user.createFile("password".toCharArray());

    assertNotNull(file);
    assertTrue(file.length > 0);

    byte[] secondFile = user.createFile("password".toCharArray());

    assertFalse(Arrays.equals(file, secondFile));
  }

  @Test
  public void testCreateHash() {
    NoUser user = new TestNoUser("Test");
    byte[] hash = user.createHash();

    assertNotNull(hash);
    assertEquals(hash.length, 64);

    byte[] secondHash = user.createHash();

    assertTrue(Arrays.equals(hash, secondHash));
  }

  @Test
  public void testCreateHashString() {
    NoUser user = new TestNoUser("Test");
    byte[] hash = user.createHash();
    String hashString = user.createHashString();

    assertEquals(NoUtil.fromBytes(hash), hashString);
  }

  @Test
  public void testCreateUserFromFile()
 throws IllegalBlockSizeException, BadPaddingException,
      ClassNotFoundException, IOException, NoUserNotValidException {
    NoUser user = new TestNoUser("Test");
    final byte[] originalFile = user.createFile("password".toCharArray());
    byte[] file = Arrays.copyOf(originalFile, originalFile.length);
    byte[] hash = user.createHash();
    String hashString = user.createHashString();
    user = null;

    try {
      user = NoUser.createUserFromFile(file, "wrongpassword".toCharArray(), TestNoUser.class);
      fail("Should have thrown an error when given wrong password.");
    } catch (NoUserNotValidException e) {
      // Do nothing, correct
    }

    file = Arrays.copyOf(originalFile, originalFile.length);
    user = NoUser.createUserFromFile(file, "password".toCharArray(), TestNoUser.class);
    assertTrue(Arrays.equals(hash, user.createHash()));
    assertEquals(hashString, user.createHashString());

    file = Arrays.copyOf(originalFile, originalFile.length);
    try {
      NoUser.createUserFromFile(file, null, TestNoUser.class);
      fail("Should have thrown a NullPointerException.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }

    try {
      NoUser.createUserFromFile(null, "password".toCharArray(), TestNoUser.class);
      fail("Should have thrown a IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // Do nothing, correct
    }

    try {
      NoUser.createUserFromFile(null, null, TestNoUser.class);
      fail("Should have thrown a IllegalArgumentException.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

}
