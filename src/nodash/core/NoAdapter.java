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

import java.security.PublicKey;
import java.util.Collection;

import nodash.exceptions.NoAdapterException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoByteSet;
import nodash.models.NoSession;

public interface NoAdapter {
  public void insertHash(byte[] hash) throws NoAdapterException;

  public void removeHash(byte[] hash) throws NoAdapterException;

  public void checkHash(byte[] hash) throws NoAdapterException, NoUserNotValidException;

  public byte[][] exportHashes() throws NoAdapterException;

  public long hashCount() throws NoAdapterException;

  public void goOnline(byte[] hash) throws NoAdapterException, NoUserAlreadyOnlineException;

  public boolean isOnline(byte[] hash) throws NoAdapterException;

  public void goOffline(byte[] hash) throws NoAdapterException;

  public void addNoSession(NoSession session) throws NoAdapterException;

  public boolean containsNoSession(byte[] encryptedUuid) throws NoAdapterException;

  public void shredNoSession(byte[] encryptedUuid) throws NoAdapterException;

  public NoSession getNoSession(byte[] encryptedUuid) throws NoAdapterException,
      NoSessionExpiredException;

  public Collection<NoByteSet> pollNoByteSets(PublicKey address) throws NoAdapterException;

  public void addNoByteSet(NoByteSet byteSet, PublicKey address) throws NoAdapterException;

  public void addNoByteSets(Collection<NoByteSet> byteSets, PublicKey address)
      throws NoAdapterException;
}
