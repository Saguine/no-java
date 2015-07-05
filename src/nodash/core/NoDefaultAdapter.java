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

package nodash.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.codec.binary.Base64;

import nodash.exceptions.NoAdapterException;
import nodash.exceptions.NoDashFatalException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoByteSet;
import nodash.models.NoSession;

public class NoDefaultAdapter implements NoAdapter {
  private static Map<PublicKey, List<NoByteSet>> byteSets =
      new ConcurrentHashMap<PublicKey, List<NoByteSet>>();
  private static Map<String, NoSession> sessions = new ConcurrentHashMap<String, NoSession>();
  private static Set<String> online = Collections
      .newSetFromMap(new ConcurrentHashMap<String, Boolean>());

  private static final String HASH_FILE = "nosystem.hash";

  private static byte[] getFile() throws NoAdapterException {
    try {
      return alterFile(null, null);
    } catch (IOException e) {
      throw new NoAdapterException("Could not get hash file.", e);
    }
  }

  private static synchronized byte[] alterFile(byte[] hashToAdd, byte[] hashToRemove)
      throws IOException {
    File file = new File(HASH_FILE);
    if (!file.exists()) {
      Files.createFile(file.toPath());
    }
    byte[] originalFileBytes = Files.readAllBytes(file.toPath());
    if (hashToAdd == null && hashToRemove == null) {
      return originalFileBytes;
    }

    if (!(hashToAdd == null ^ hashToRemove == null) && Arrays.equals(hashToAdd, hashToRemove)) {
      throw new IllegalArgumentException("Hashes to add and remove cannot be the same.");
    }
    int hashes = originalFileBytes.length / 64;
    List<Byte> newFile = new ArrayList<Byte>();
    
    for (int x = 0; x < hashes; x++) {
      byte[] hash = Arrays.copyOfRange(originalFileBytes, x * 64, x * 64 + 64);

      if (hashToRemove == null || !Arrays.equals(hash, hashToRemove)) {
        for (byte hashByte : hash) {
          newFile.add(hashByte);
        }
      }

      if (hashToAdd != null && Arrays.equals(hash, hashToAdd)) {
        hashToAdd = null;
      }
    }

    if (hashToAdd != null) {
      for (byte hashByte : hashToAdd) {
        newFile.add(hashByte);
      }
    }

    byte[] newFileAsPrimitive = new byte[newFile.size()];
    for (int x = 0; x < newFile.size(); x++) {
      newFileAsPrimitive[x] = newFile.get(x).byteValue();
    }

    Files.write(file.toPath(), newFileAsPrimitive);

    return newFileAsPrimitive;
  }

  public NoDefaultAdapter() {

  }

  @Override
  public void insertHash(byte[] hash) throws NoAdapterException {
    try {
      alterFile(hash, null);
    } catch (IOException e) {
      throw new NoAdapterException("Trouble while inserting hash.", e);
    }
  }

  @Override
  public void removeHash(byte[] hash) throws NoAdapterException {
    try {
      alterFile(null, hash);
    } catch (IOException e) {
      throw new NoAdapterException("Trouble removing hash.", e);
    }
  }

  @Override
  public void checkHash(byte[] hash) throws NoAdapterException, NoUserNotValidException {
    byte[] hashFile = getFile();
    int hashes = hashFile.length / 64;
    for (int x = 0; x < hashes; x++) {
      byte[] examine = Arrays.copyOfRange(hashFile, x * 64, x * 64 + 64);
      if (Arrays.equals(hash, examine)) {
        return;
      }
    }
    throw new NoUserNotValidException();
  }

  @Override
  public byte[][] exportHashes() throws NoAdapterException {
    byte[] hashFile = getFile();
    int hashes = hashFile.length / 64;
    byte[][] export = new byte[hashes][64];
    for (int x = 0; x < hashes; x++) {
      byte[] hash = Arrays.copyOfRange(hashFile, x * 64, x * 64 + 64);
      export[x] = hash;
    }
    return export;
  }

  @Override
  public long hashCount() throws NoAdapterException {
    byte[] hashFile = getFile();
    return hashFile.length / 64;
  }

  @Override
  public void addNoSession(NoSession session) {
    if (containsNoSession(session.getEncryptedUuid())) {
      throw new NoDashFatalException("No such session exists.");
    }
    sessions.put(session.getUuid(), session);
  }

  @Override
  public boolean containsNoSession(byte[] encryptedUuid) {
    String uuid;
    try {
      uuid = Base64.encodeBase64URLSafeString(NoUtil.decrypt(encryptedUuid));
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new NoDashFatalException("Could not decrypt given UUID.", e);
    }

    return sessions.containsKey(uuid);
  }

  @Override
  public void shredNoSession(byte[] encryptedUuid) {
    NoSession session = getNoSession(encryptedUuid);
    sessions.remove(session.getUuid());
  }

  @Override
  public NoSession getNoSession(byte[] encryptedUuid) {
    if (containsNoSession(encryptedUuid)) {
      String uuid;
      try {
        uuid = Base64.encodeBase64URLSafeString(NoUtil.decrypt(encryptedUuid));
      } catch (IllegalBlockSizeException | BadPaddingException e) {
        throw new NoDashFatalException("Could not decrypt given UUID.", e);
      }
      return sessions.get(uuid);
    }
    throw new NoDashFatalException("No such session exists.");
  }

  @Override
  public List<NoByteSet> pollNoByteSets(PublicKey address) {
    if (byteSets.containsKey(address)) {
      List<NoByteSet> storedByteSets = byteSets.get(address);
      List<NoByteSet> result = new ArrayList<NoByteSet>();
      int iterations = storedByteSets.size();
      for (int x = 0; x < iterations; x++) {
        result.add(storedByteSets.get(0));
        storedByteSets.remove(0);
      }
      return result;
    } else {
      return new ArrayList<NoByteSet>();
    }
  }
  
  @Override
  public void addNoByteSet(NoByteSet byteSet, PublicKey address) {
    if (!byteSets.containsKey(address)) {
      byteSets.put(address, new ArrayList<NoByteSet>());
    }
    byteSets.get(address).add(byteSet);
  }

  @Override
  public void addNoByteSets(List<NoByteSet> addedByteSets, PublicKey address) {
    if (addedByteSets == null) {
      return;
    }
    
    if (address == null) {
      throw new NullPointerException("Address cannot be null.");
    }
    
    if (byteSets.containsKey(address)) {
      byteSets.get(address).addAll(addedByteSets);
    } else {
      byteSets.put(address, addedByteSets);
    }
  }

  @Override
  public void goOnline(byte[] hash) throws NoUserAlreadyOnlineException {
    String hashString = Base64.encodeBase64URLSafeString(hash);
    if (online.contains(hashString)) {
      throw new NoUserAlreadyOnlineException();
    }
    online.add(hashString);
  }

  @Override
  public boolean isOnline(byte[] hash) {
    String hashString = Base64.encodeBase64URLSafeString(hash);
    return online.contains(hashString);
  }

  @Override
  public void goOffline(byte[] hash) {
    String hashString = Base64.encodeBase64URLSafeString(hash);
    online.remove(hashString);
  }

}
