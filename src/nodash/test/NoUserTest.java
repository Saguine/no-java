package nodash.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import nodash.models.NoUser;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

public class NoUserTest {

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
  public void testCreateUserFromFile() {
    NoUser user = new NoUser();
    final byte[] originalFile = user.createFile("password".toCharArray());
    byte[] file = originalFile;
    byte[] hash = user.createHash();
    String hashString = user.createHashString();
    user = null;
    
    try {
      user = NoUser.createUserFromFile(file, "wrongpassword".toCharArray());
      fail("Should have thrown an error when given wrong password.");
    } catch (IllegalBlockSizeException e) {
      fail("IllegalBlockSizeException encountered.");
    } catch (BadPaddingException e) {
      fail("BadPaddingException encountered.");
    } catch (ClassNotFoundException e) {
      fail("ClassNotFoundException encountered.");
    } catch (IOException e) {
      fail("IOException encountered.");
    }
    
    try {
      user = NoUser.createUserFromFile(file, "password".toCharArray());
    } catch (IllegalBlockSizeException | BadPaddingException | ClassNotFoundException | IOException e) {
      fail("Encountered an error of type " + e.getClass().getSimpleName());
    }
    
    assertTrue(Arrays.equals(hash, user.createHash()));
    assertEquals(hashString, user.createHashString());
  }

}
