package nodash.test;

import static org.junit.Assert.*;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import nodash.core.NoCore;
import nodash.core.NoUtil;
import nodash.exceptions.NoDashFatalException;

import org.junit.Before;
import org.junit.Test;

public class NoUtilTest {
  
  @Before
  public void setup() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
    NoCore.setup();
  }
  
  @Test
  public void testAllowedKeySize() throws NoSuchAlgorithmException {
    if (Cipher.getMaxAllowedKeyLength(NoUtil.CIPHER_KEY_SPEC) < NoUtil.AES_STRENGTH) {
      fail("Max allowed key length for CIPHER_TYPE (AES) less than required.");
    }
    
  }

  @Test
  public void testBytesToChars() {
    byte[] bytes = {'s', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};
    char[] chars = NoUtil.bytesToChars(bytes);
    assertTrue(Arrays.equals(bytes, NoUtil.charsToBytes(chars)));

    try {
      NoUtil.bytesToChars(null);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testCharsToBytes() {
    char[] chars = {'s', 'o', 'm', 'e', 'c', 'h', 'a', 'r', 's'};
    byte[] bytes = NoUtil.charsToBytes(chars);
    assertTrue(Arrays.equals(chars, NoUtil.bytesToChars(bytes)));

    try {
      NoUtil.charsToBytes(null);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testWipeBytes() {
    final byte[] originalBytes = {'s', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};
    final byte[] expectedBlankBytes = {'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A'};
    byte[] bytes = Arrays.copyOf(originalBytes, originalBytes.length);

    NoUtil.wipeBytes(bytes);
    assertFalse(Arrays.equals(originalBytes, bytes));
    assertTrue(Arrays.equals(bytes, expectedBlankBytes));

    try {
      NoUtil.wipeBytes(null);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testWipeChars() {
    final char[] originalChars = {'s', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};
    final char[] expectedBlankChars = {'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A'};
    char[] chars = Arrays.copyOf(originalChars, originalChars.length);

    NoUtil.wipeChars(chars);
    assertFalse(Arrays.equals(originalChars, chars));
    assertTrue(Arrays.equals(chars, expectedBlankChars));

    try {
      NoUtil.wipeChars(null);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testGetHashFromByteArray() {
    byte[] bytes = {'s', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};
    byte[] hash = NoUtil.getHashFromByteArray(bytes);

    assertNotNull(hash);
    assertEquals(hash.length, 64);

    byte[] bytesEqual = {'s', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};
    byte[] bytesNotEqual = {'S', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};

    assertTrue(Arrays.equals(hash, NoUtil.getHashFromByteArray(bytesEqual)));
    assertFalse(Arrays.equals(hash, NoUtil.getHashFromByteArray(bytesNotEqual)));

    try {
      NoUtil.getHashFromByteArray(null);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testByteKeyEncryptionDecryptionAES() throws IllegalBlockSizeException, BadPaddingException {
    final byte[] originalBytes = {'s', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};
    final byte[] originalByteKey = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};

    byte[] bytes = Arrays.copyOf(originalBytes, originalBytes.length);
    byte[] byteKey = Arrays.copyOf(originalByteKey, originalByteKey.length);

    byte[] encryptedByByteKey = NoUtil.encrypt(bytes, byteKey);

    try {
      NoUtil.decrypt(encryptedByByteKey, new byte[] {'b', 'a', 'd', 'k', 'e', 'y'});
      fail("Did not throw BadPaddingException while decrypting with bad key.");
    } catch (BadPaddingException e) {
      // Do nothing, correct
    }
    
    byte[] decryptedByByteKey = NoUtil.decrypt(encryptedByByteKey, byteKey);
    assertTrue(Arrays.equals(originalBytes, decryptedByByteKey));
    
    byte[] nullByte = null;
    try {
      NoUtil.decrypt(nullByte, nullByte);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    } catch (IllegalBlockSizeException e) {
      fail("Allowed null parameter without thrown exception.");
    } catch (BadPaddingException e) {
      fail("Allowed null parameter without thrown exception.");
    }

    try {
      NoUtil.decrypt(bytes, nullByte);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }

    try {
      NoUtil.decrypt(nullByte, bytes);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testCharKeyEncryptionDecryptionAES() throws IllegalBlockSizeException, BadPaddingException {    
    final byte[] originalBytes = {'s', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};
    final char[] originalCharKey = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};

    byte[] bytes = Arrays.copyOf(originalBytes, originalBytes.length);
    char[] charKey = Arrays.copyOf(originalCharKey, originalCharKey.length);

    byte[] encryptedByCharKey = NoUtil.encrypt(bytes, charKey);

    try {
      NoUtil.decrypt(encryptedByCharKey, new byte[] {'b', 'a', 'd', 'k', 'e', 'y'});
      fail("Did not throw BadPaddingException while decrypting with bad key.");
    } catch (BadPaddingException e) {
      // Do nothing, correct
    }
    
    byte[] decryptedByCharKey = NoUtil.decrypt(encryptedByCharKey, charKey);
    assertTrue(Arrays.equals(originalBytes, decryptedByCharKey));
    
    byte[] nullByte = null;
    byte[] nullChar = null;
    try {
      NoUtil.decrypt(nullByte, nullChar);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }

    try {
      NoUtil.decrypt(bytes, nullChar);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }

    try {
      NoUtil.decrypt(nullByte, new char[] {'c', 'h', 'a', 'r'});
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testNoKeyEncryptionDecryptionAES() throws IllegalBlockSizeException, BadPaddingException {
    final byte[] originalBytes = {'s', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};
    byte[] bytes = Arrays.copyOf(originalBytes, originalBytes.length);

    byte[] encrypted = NoUtil.encrypt(bytes);
    byte[] decrypted = NoUtil.decrypt(encrypted);
    assertTrue(Arrays.equals(originalBytes, decrypted));
    
    try {
      NoUtil.decrypt(null);
      fail("Allowed null parameter without thrown exception.");
    } catch (NullPointerException e) {
      // Do nothing, correct
    }
  }

  @Test
  public void testEncryptionDecryptionRSA() throws NoSuchAlgorithmException,
      NoSuchProviderException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    KeyPairGenerator kpg;
    try {
      kpg = KeyPairGenerator.getInstance(NoUtil.KEYPAIR_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for KEYPAIR_ALGORITHM is not valid.", e);
    }

    kpg.initialize(NoUtil.RSA_STRENGTH,
        SecureRandom.getInstance(NoUtil.SECURERANDOM_ALGORITHM, NoUtil.SECURERANDOM_PROVIDER));
    

    KeyPair keyPair = kpg.generateKeyPair();
    KeyPair keyPair2 = kpg.generateKeyPair();
    
    final byte[] originalBytes = {'s', 'o', 'm', 'e', 'b', 'y', 't', 'e', 's'};
    byte[] bytes = Arrays.copyOf(originalBytes, originalBytes.length);
    
    byte[] encrypted = NoUtil.encryptRSA(bytes, keyPair.getPublic());
    try {
      NoUtil.decryptRSA(encrypted, keyPair2.getPrivate());
      fail("Did not throw exception with incorrect private key.");
    } catch (BadPaddingException e) {
      // Do nothing, correct
    }
    
    byte[] decrypted = NoUtil.decryptRSA(encrypted, keyPair.getPrivate());
    assertTrue(Arrays.equals(originalBytes, decrypted));
  }

}
