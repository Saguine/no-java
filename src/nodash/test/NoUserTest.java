package nodash.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import nodash.core.NoCore;
import nodash.models.NoUser;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

public class NoUserTest {
  
  @Before
  public void setup() {
    NoCore.setup();
  }

  @Test
  public void testNoUser() {
    NoUser user = new NoUser();
    
    assertNotNull(user.getNoActions());
    assertEquals(user.getNoActions().size(), 0);
    assertNotNull(user.getRSAPublicKey());
    assertNotNull(user.getPublicExponent());
    assertNotNull(user.getModulus());
    assertEquals(user.getInfluences(), 0);
  }

  @Test
  public void testCreateFile() {
    NoUser user = new NoUser();
    byte[] file = user.createFile("password".toCharArray());
    
    assertNotNull(file);
    assertTrue(file.length > 0);
    
    byte[] secondFile = user.createFile("password".toCharArray());
    
    assertFalse(Arrays.equals(file, secondFile));
  }

  @Test
  public void testCreateHash() {
    NoUser user = new NoUser();
    byte[] hash = user.createHash();
    
    assertNotNull(hash);
    assertEquals(hash.length, 64);
    
    byte[] secondHash = user.createHash();
    
    assertTrue(Arrays.equals(hash, secondHash));
  }
  
  @Test
  public void testCreateHashString() {
    NoUser user = new NoUser();
    byte[] hash = user.createHash();
    String hashString = user.createHashString();
    
    assertEquals(Base64.encodeBase64String(hash), hashString);
  }

  @Test
  public void testCreateUserFromFile() throws IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException {
    NoUser user = new NoUser();
    final byte[] originalFile = user.createFile("password".toCharArray());
    byte[] file = Arrays.copyOf(originalFile, originalFile.length);
    byte[] hash = user.createHash();
    String hashString = user.createHashString();
    user = null;
    
    try {
      user = NoUser.createUserFromFile(file, "wrongpassword".toCharArray());
      fail("Should have thrown an error when given wrong password.");
    } catch (BadPaddingException e) {
      // Do nothing, correct
    }
    
    file = Arrays.copyOf(originalFile, originalFile.length);
    user = NoUser.createUserFromFile(file, "password".toCharArray());
    assertTrue(Arrays.equals(hash, user.createHash()));
    assertEquals(hashString, user.createHashString());
    
    file = Arrays.copyOf(originalFile, originalFile.length);
    try {
      NoUser.createUserFromFile(file, null);
      fail("Should have thrown a NullPointerException.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }

    try {
      NoUser.createUserFromFile(null, "password".toCharArray());
      fail("Should have thrown a IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // Do nothing, correct
    }

    try {
      NoUser.createUserFromFile(null, null);
      fail("Should have thrown a IllegalArgumentException.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

}
