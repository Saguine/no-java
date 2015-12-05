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
 * NoUser allows the subclassing of custom user objects whilst keeping the core requirements of a
 * NoUser: the public and private keys. It also supports the serialization, decryption and NoByteSet
 * consumption.
 */

package nodash.models;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import sun.security.rsa.RSAPrivateCrtKeyImpl;
import sun.security.rsa.RSAPublicKeyImpl;
import nodash.core.NoUtil;
import nodash.exceptions.NoByteSetBadDecryptionException;
import nodash.exceptions.NoDashFatalException;
import nodash.exceptions.NoUserNotValidException;

public abstract class NoUser implements Serializable {
  private static final long serialVersionUID = 7132405837081692211L;
  @NoHash
  private RSAPublicKeyImpl publicKey;
  @NoHash
  private RSAPrivateCrtKeyImpl privateKey;
  @NoHash
  private String randomized;

  @NoHash
  private int influences;

  @NoHash
  private int actions;

  private List<NoAction> outgoing = new ArrayList<NoAction>();

  public NoUser() {
    KeyPairGenerator kpg;
    try {
      kpg = KeyPairGenerator.getInstance(NoUtil.KEYPAIR_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for KEYPAIR_ALGORITHM is not valid.", e);
    }

    try {
      kpg.initialize(NoUtil.RSA_STRENGTH,
          SecureRandom.getInstance(NoUtil.SECURERANDOM_ALGORITHM, NoUtil.SECURERANDOM_PROVIDER));
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for SECURERANDOM_ALGORITHM not valid.", e);
    } catch (NoSuchProviderException e) {
      throw new NoDashFatalException("Value for SECURERANDOM_PROVIDER not valid.", e);
    }

    KeyPair keyPair = kpg.generateKeyPair();
    publicKey = (RSAPublicKeyImpl) keyPair.getPublic();
    privateKey = (RSAPrivateCrtKeyImpl) keyPair.getPrivate();
    influences = 0;
    actions = 0;
    touchRandomizer();
  }

  private void touchRandomizer() {
    byte[] randomBytes = new byte[64];
    try {
      SecureRandom.getInstance(NoUtil.SECURERANDOM_ALGORITHM).nextBytes(randomBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for SECURERANDOM_ALGORITHM not valid.", e);
    }
    randomized = new String(randomBytes);
  }

  public final byte[] createFile(char[] password) {
    List<NoAction> tempActions = outgoing;

    touchRandomizer();
    outgoing = new ArrayList<NoAction>();
    actions = 0;

    Gson gson = new Gson();
    byte[] json = NoUtil.toBytes(gson.toJson(this));
    byte[] encrypted = NoUtil.encrypt(json, password);

    outgoing = tempActions;
    return encrypted;
  }

  @SuppressWarnings("unchecked")
  public final byte[] createHash() {
    try {
      Comparator<Field> fieldComp = new Comparator<Field>() {
        @Override
        public int compare(Field o1, Field o2) {
          return o1.getName().compareTo(o2.getName());
        }
      };

      Class<? extends NoUser> userClass = getClass();
      StringBuilder toString = new StringBuilder();

      while (userClass != null) {
        Field[] noHashFields = userClass.getDeclaredFields();

        Arrays.sort(noHashFields, fieldComp);

        for (Field field : noHashFields) {
          if (field.isAnnotationPresent(NoHash.class)) {
            field.setAccessible(true);
            toString.append("|");
            Object item = field.get(this);
            if (item != null) {
              toString.append(field.get(this).toString());
            }
          }
        }

        if (userClass == NoUser.class) {
          userClass = null;
        } else {
          userClass = (Class<? extends NoUser>) userClass.getSuperclass();
        }
      }

      byte[] itemBytes = toString.toString().getBytes();

      return NoUtil.getHashFromByteArray(itemBytes);
    } catch (IllegalArgumentException e) {
      throw new NoDashFatalException(
          "IllegalArgument Exception encountered while generating user hash.", e);
    } catch (IllegalAccessException e) {
      throw new NoDashFatalException(
          "IllegalAccess Exception encountered while generating user hash.", e);
    }
  }

  public final void consume(NoByteSet byteSet) throws NoByteSetBadDecryptionException {
    try {
      SecretKey secretKey = new SecretKeySpec(decryptRsa(byteSet.key), NoUtil.CIPHER_KEY_SPEC);
      byte[] key = secretKey.getEncoded();
      secretKey = null;
      NoInfluence influence = NoInfluence.decrypt(byteSet.data, key);
      NoUtil.wipeBytes(key);

      influence.applyTo(this);
      this.influences++;
    } catch (BadPaddingException e) {
      throw new NoByteSetBadDecryptionException(e);
    } catch (IllegalBlockSizeException e) {
      throw new NoByteSetBadDecryptionException(e);
    } catch (ClassNotFoundException e) {
      throw new NoByteSetBadDecryptionException(e);
    } catch (InvalidKeyException e) {
      throw new NoByteSetBadDecryptionException(e);
    }
  }

  public final void addAction(NoAction action) {
    outgoing.add(action);
    actions++;
  }

  public final List<NoAction> getNoActions() {
    return outgoing;
  }

  public final BigInteger getPublicExponent() {
    return ((RSAPublicKeyImpl) publicKey).getPublicExponent();
  }

  public final BigInteger getModulus() {
    return ((RSAPublicKeyImpl) publicKey).getModulus();
  }

  public final PublicKey getRsaPublicKey() {
    try {
      return new RSAPublicKeyImpl(this.getModulus(), this.getPublicExponent());
    } catch (InvalidKeyException e) {
      throw new NoDashFatalException("Invalid key while re-generating a RSAPublicKey.", e);
    }
  }

  public int getInfluences() {
    return influences;
  }

  private final byte[] decryptRsa(byte[] data)
      throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    return NoUtil.decryptRsa(data, privateKey);
  }

  public static NoUser createUserFromFile(byte[] data, char[] password,
      Class<? extends NoUser> clazz) throws NoUserNotValidException {
    byte[] decrypted;
    try {
      decrypted = NoUtil.decrypt(data, password);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new NoUserNotValidException(e);
    }

    Gson gson = new Gson();
    String json = NoUtil.fromBytes(decrypted);
    try {
      NoUser noUser = gson.fromJson(json, clazz);
      return noUser;
    } catch (JsonSyntaxException e) {
      throw new NoUserNotValidException(e);
    }
  }

  public final String createHashString() {
    return NoUtil.fromBytes(createHash());
  }

  @Override
  public final boolean equals(Object otherUser) {
    if (otherUser == null) {
      return false;
    }

    if (!NoUser.class.isAssignableFrom(otherUser.getClass())) {
      return false;
    }

    return privateKey.getModulus().equals(((NoUser) otherUser).privateKey.getModulus());
  }

}
