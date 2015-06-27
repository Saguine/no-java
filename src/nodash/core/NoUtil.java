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
 * The NoUtil class encapsulates no- standard functions such as encryption/decryption and hashing
 * algorithms.
 */

package nodash.core;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import nodash.exceptions.NoDashFatalException;

public final class NoUtil {
  public static final String CIPHER_TYPE = "AES/ECB/PKCS5PADDING";
  public static final String CIPHER_KEY_SPEC = "AES";
  public static final String DIGEST_TYPE = "SHA-512";
  public static final String PBE_TYPE = "PBKDF2WithHmacSHA1";
  public static final String CIPHER_RSA_TYPE = "RSA/ECB/PKCS1PADDING";
  public static final String KEYPAIR_ALGORITHM = "RSA";
  public static final String SECURERANDOM_ALGORITHM = "SHA1PRNG";
  public static final String SECURERANDOM_PROVIDER = "SUN";
  public static final int RSA_STRENGTH = 4096;
  public static final int AES_STRENGTH = 256;
  public static final byte BLANK_BYTE = 'A';

  public static char[] bytesToChars(byte[] array) {
    char[] result = new char[array.length];
    for (int x = 0; x < array.length; x++) {
      result[x] = (char) array[x];
    }
    return result;
  }

  public static byte[] charsToBytes(char[] array) {
    byte[] result = new byte[array.length];
    for (int x = 0; x < array.length; x++) {
      result[x] = (byte) array[x];
    }
    return result;
  }

  public static void wipeBytes(byte[] array) {
    for (int x = 0; x < array.length; x++) {
      array[x] = NoUtil.BLANK_BYTE;
    }
  }

  public static void wipeChars(char[] array) {
    for (int x = 0; x < array.length; x++) {
      array[x] = NoUtil.BLANK_BYTE;
    }
  }

  private static byte[] getPBEKeyFromPassword(char[] password) {
    SecretKeyFactory skf;
    try {
      skf = SecretKeyFactory.getInstance(NoUtil.PBE_TYPE);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for PBE_TYPE is not valid.", e);
    }
    KeySpec spec = new PBEKeySpec(password, NoCore.config.getSecretKey().getEncoded(), 65536, 256);
    SecretKey key;
    try {
      key = skf.generateSecret(spec);
    } catch (InvalidKeySpecException e) {
      throw new NoDashFatalException("PBE manager unable to derive key from password.", e);
    }
    NoUtil.wipeChars(password);
    return key.getEncoded();
  }

  public static byte[] getHashFromByteArray(byte[] bytes) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance(NoUtil.DIGEST_TYPE);
      return messageDigest.digest(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for DIGEST_TYPE not valid.", e);
    }
  }

  public static byte[] decrypt(byte[] data, char[] password) throws IllegalBlockSizeException,
      BadPaddingException {
    byte[] passwordByte = NoUtil.getPBEKeyFromPassword(password);
    byte[] response = NoUtil.decrypt(NoUtil.decrypt(data), passwordByte);
    NoUtil.wipeBytes(passwordByte);
    return response;
  }

  public static byte[] encrypt(byte[] data, char[] password) {
    byte[] passwordByte = NoUtil.getPBEKeyFromPassword(password);
    byte[] response = NoUtil.encrypt(NoUtil.encrypt(data, passwordByte));
    NoUtil.wipeBytes(passwordByte);
    return response;
  }

  public static byte[] encrypt(byte[] data, byte[] key) {
    Cipher cipher;
    try {
      cipher = Cipher.getInstance(NoUtil.CIPHER_TYPE);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for CIPHER_TYPE is not valid (no such algorithm).", e);
    } catch (NoSuchPaddingException e) {
      throw new NoDashFatalException("Value for CIPHER_TYPE is not valid (no such padding).", e);
    }
    SecretKeySpec secretKey = new SecretKeySpec(key, NoUtil.CIPHER_KEY_SPEC);
    try {
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    } catch (InvalidKeyException e) {
      throw new NoDashFatalException("Secret key is invalid.", e);
    }

    try {
      return cipher.doFinal(data);
    } catch (IllegalBlockSizeException e) {
      throw new NoDashFatalException("Block size exception encountered during encryption.", e);
    } catch (BadPaddingException e) {
      throw new NoDashFatalException("Bad padding exception encountered during encryption.", e);
    }
  }

  public static byte[] encrypt(byte[] data) {
    return NoUtil.encrypt(data, NoCore.config.getSecretKey().getEncoded());
  }

  public static byte[] decrypt(byte[] data, byte[] key) throws IllegalBlockSizeException,
      BadPaddingException {
    Cipher cipher;
    try {
      cipher = Cipher.getInstance(NoUtil.CIPHER_TYPE);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for CIPHER_TYPE is not valid (no such algorithm).", e);
    } catch (NoSuchPaddingException e) {
      throw new NoDashFatalException("Value for CIPHER_TYPE is not valid (no such padding).", e);
    }
    SecretKeySpec secretKey = new SecretKeySpec(key, NoUtil.CIPHER_KEY_SPEC);
    try {
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
    } catch (InvalidKeyException e) {
      throw new NoDashFatalException("Secret key is invalid.", e);
    }

    return cipher.doFinal(data);
  }

  public static byte[] decrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
    return NoUtil.decrypt(data, NoCore.config.getSecretKey().getEncoded());
  }

  public static byte[] encryptRSA(byte[] data, PublicKey publicKey) {
    Cipher cipher;
    try {
      cipher = Cipher.getInstance(NoUtil.CIPHER_RSA_TYPE);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for CIPHER_RSA_TYPE is not valid (no such algorithm).",
          e);
    } catch (NoSuchPaddingException e) {
      throw new NoDashFatalException("Value for CIPHER_RSA_TYPE is not valid (no such padding).", e);
    }
    try {
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      return cipher.doFinal(data);
    } catch (InvalidKeyException e) {
      throw new NoDashFatalException("Public key invalid.", e);
    } catch (IllegalBlockSizeException e) {
      throw new NoDashFatalException("Unable to encrypt data stream with public key.", e);
    } catch (BadPaddingException e) {
      throw new NoDashFatalException("Unable to encrypt data stream with public key.", e);
    }
  }

  public static byte[] decryptRSA(byte[] data, PrivateKey privateKey) throws InvalidKeyException,
      IllegalBlockSizeException, BadPaddingException {
    Cipher cipher;
    try {
      cipher = Cipher.getInstance(NoUtil.CIPHER_RSA_TYPE);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for CIPHER_RSA_TYPE is not valid (no such algorithm).",
          e);
    } catch (NoSuchPaddingException e) {
      throw new NoDashFatalException("Value for CIPHER_RSA_TYPE is not valid (no such padding).", e);
    }
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    return cipher.doFinal(data);
  }

}
