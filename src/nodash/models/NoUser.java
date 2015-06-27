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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import sun.security.rsa.RSAPublicKeyImpl;
import nodash.core.NoUtil;
import nodash.exceptions.NoByteSetBadDecryptionException;
import nodash.exceptions.NoDashFatalException;

public class NoUser implements Serializable {
  private static final long serialVersionUID = 7132405837081692211L;
  private PublicKey publicKey;
  private PrivateKey privateKey;
  @SuppressWarnings("unused")
  private String randomized;

  public int influences;
  public int actions;

  private ArrayList<NoAction> outgoing = new ArrayList<NoAction>();

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
    this.publicKey = keyPair.getPublic();
    this.privateKey = keyPair.getPrivate();
    this.influences = 0;
    this.actions = 0;
    this.touchRandomizer();
  }

  private void touchRandomizer() {
    byte[] randomBytes = new byte[64];
    try {
      SecureRandom.getInstance(NoUtil.SECURERANDOM_ALGORITHM).nextBytes(randomBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for SECURERANDOM_ALGORITHM not valid.", e);
    }
    this.randomized = new String(randomBytes);
  }

  public final byte[] createFile(char[] password) {
    ArrayList<NoAction> temp = this.outgoing;
    try {
      this.touchRandomizer();
      this.outgoing = new ArrayList<NoAction>();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(this);
      byte[] encrypted = NoUtil.encrypt(baos.toByteArray(), password);
      oos.close();
      baos.close();
      return encrypted;
    } catch (IOException e) {
      throw new NoDashFatalException(
          "IO Exception encountered while generating encrypted user file byte stream.", e);
    } finally {
      this.outgoing = temp;
    }
  }

  public final byte[] createHash() {
    ArrayList<NoAction> temp = this.outgoing;
    try {
      this.outgoing = new ArrayList<NoAction>();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(this);
      byte[] userBytes = baos.toByteArray();
      return NoUtil.getHashFromByteArray(userBytes);
    } catch (IOException e) {
      throw new NoDashFatalException("IO Exception encountered while generating user hash.", e);
    } finally {
      this.outgoing = temp;
    }
  }

  public final String createHashString() {
    return new String(this.createHash());
  }

  public final void consume(NoByteSet byteSet) throws NoByteSetBadDecryptionException {
    try {
      SecretKey secretKey = new SecretKeySpec(decryptRSA(byteSet.key), NoUtil.CIPHER_KEY_SPEC);
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
    this.outgoing.add(action);
    this.actions++;
  }

  public final ArrayList<NoAction> getNoActions() {
    return this.outgoing;
  }

  public final BigInteger getPublicExponent() {
    return ((RSAPublicKeyImpl) publicKey).getPublicExponent();
  }

  public final BigInteger getModulus() {
    return ((RSAPublicKeyImpl) publicKey).getModulus();
  }

  public final PublicKey getRSAPublicKey() {
    try {
      return new RSAPublicKeyImpl(this.getModulus(), this.getPublicExponent());
    } catch (InvalidKeyException e) {
      throw new NoDashFatalException("Invalid key while re-generating a RSAPublicKey.", e);
    }
  }

  private final byte[] decryptRSA(byte[] data) throws InvalidKeyException,
      IllegalBlockSizeException, BadPaddingException {
    return NoUtil.decryptRSA(data, this.privateKey);
  }

  public static NoUser createUserFromFile(byte[] data, char[] password)
      throws IllegalBlockSizeException, BadPaddingException, IOException, ClassNotFoundException {
    byte[] decrypted = NoUtil.decrypt(data, password);
    ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);
    ObjectInputStream ois = new ObjectInputStream(bais);
    NoUser noUser = (NoUser) ois.readObject();
    ois.close();
    bais.close();
    return noUser;
  }

}
