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
 * NoInfluence is an abstract class allowing for the subclassing of user influences, generated by
 * both actions and the server. Upon login, the user consumes NoByteSets (generated by
 * .getByteSet()) into the primary NoInfluence, which is then applied to the user with
 * .applyTo(NoUser).
 * 
 * Examples include incoming messages, financial changes or charges or updates.
 */

package nodash.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import nodash.core.NoUtil;
import nodash.exceptions.NoDashFatalException;

public abstract class NoInfluence implements Serializable {
  private static final long serialVersionUID = -7509462039664862920L;

  public abstract void applyTo(NoUser user);

  public final NoByteSet getByteSet(PublicKey publicKey) {
    KeyGenerator keyGen;
    try {
      keyGen = KeyGenerator.getInstance(NoUtil.CIPHER_KEY_SPEC);
    } catch (NoSuchAlgorithmException e) {
      throw new NoDashFatalException("Value for CIPHER_KEY_SPEC is not valid.", e);
    }
    keyGen.init(NoUtil.AES_STRENGTH);
    SecretKey secretKey = keyGen.generateKey();
    byte[] key = secretKey.getEncoded();
    byte[] encryptedKey = NoUtil.encryptRSA(key, publicKey);
    byte[] data = this.getEncrypted(key);
    NoUtil.wipeBytes(key);
    return new NoByteSet(encryptedKey, data);
  }

  private final byte[] getEncrypted(byte[] key) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(this);
      byte[] encrypted = NoUtil.encrypt(baos.toByteArray(), key);
      oos.close();
      baos.close();
      return encrypted;
    } catch (IOException e) {
      throw new NoDashFatalException("Unable to write NoInfluence object to byte stream.", e);
    }
  }

  public static NoInfluence decrypt(byte[] data, byte[] key) throws IllegalBlockSizeException,
      BadPaddingException, ClassNotFoundException {
    byte[] decrypted = NoUtil.decrypt(data, key);
    ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);
    try {
      ObjectInputStream ois = new ObjectInputStream(bais);
      NoInfluence noInfluence = (NoInfluence) ois.readObject();
      ois.close();
      bais.close();
      return noInfluence;
    } catch (IOException e) {
      throw new NoDashFatalException("Unable to read out provided data stream.", e);
    }
  }

}
