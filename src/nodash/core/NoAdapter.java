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

/**
 * The NoAdapter interface defines the contract for establishing a connection between the no-system
 * and the chosen storage system of the application (file system, database, Memcache).
 * 
 * @author horsey
 *
 */
public interface NoAdapter {
  /**
   * Saves a hash into the list of user hashes.
   * 
   * @param hash the byte array representing a NoUser object.
   * @throws NoAdapterException - if the adapter is unable to insert the hash.
   */
  public void insertHash(byte[] hash) throws NoAdapterException;

  /**
   * Removes the hash from the list of user hashes.
   * 
   * @param hash the byte array representing a NoUser object.
   * @throws NoAdapterException - if the adapter is unable to remove the hash.
   */
  public void removeHash(byte[] hash) throws NoAdapterException;

  /**
   * Checks if the given hash is in the list of user hashes. Throws an exception if not.
   * 
   * @param hash the byte array representing a NoUser object.
   * @throws NoAdapterException - if the adapter is unable to verify the hash.
   * @throws NoUserNotValidException - if the hash is not valid.
   */
  public void checkHash(byte[] hash) throws NoAdapterException, NoUserNotValidException;

  /**
   * Returns a byte array of byte arrays representing all user hashes in the system.
   * 
   * @return a two dimensional array of the user hashes, delimited by the first index.
   * @throws NoAdapterException - if the adapter cannot return the user hashes.
   */
  public byte[][] exportHashes() throws NoAdapterException;

  /**
   * Returns the number of user hashes saved in the system.
   * 
   * @return the number of user hashes.
   * @throws NoAdapterException - if the adapter is unable to get the list of user hashes to count.
   */
  public long hashCount() throws NoAdapterException;

  /**
   * Marks the given has as online. Note that the hash does not have to represent an actual user.
   * 
   * @param hash the byte array hash to go online.
   * @throws NoAdapterException - if the adapter is unable to mark the hash as online.
   * @throws NoUserAlreadyOnlineException - if the provided hash is already marked as online.
   */
  public void goOnline(byte[] hash) throws NoAdapterException, NoUserAlreadyOnlineException;

  /**
   * Checks whether or not the given hash is marked as online.
   * 
   * @param hash the byte array to check.
   * @return true if the given hash is marked as online, esle false.
   * @throws NoAdapterException - if the adapter is unable to check the online hashes.
   */
  public boolean isOnline(byte[] hash) throws NoAdapterException;

  /**
   * Marks the given hash as offline. This does not throw an exception if the hash is not marked as
   * online.
   * 
   * @param hash the byte array to mark as offline.
   * @throws NoAdapterException - if the adapter is unable to mark the given hash as offline.
   */
  public void goOffline(byte[] hash) throws NoAdapterException;

  /**
   * Adds the given NoSession to an accessible pool, ideally indexed by the UUID.
   * 
   * @param session the NoSession object to add.
   * @throws NoAdapterException - if the adapter is unable to add the NoSession object.
   */
  public void addNoSession(NoSession session) throws NoAdapterException;

  /**
   * Checks if the pool of sessions contains a NoSession object with the UUID represented by the
   * encrypted byte array.
   * 
   * @param encryptedUuid the encrypted byte array representing the NoSession UUID.
   * @return true if the session pool contains a session with the requested UUID.
   * @throws NoAdapterException
   */
  public boolean containsNoSession(byte[] encryptedUuid) throws NoAdapterException;

  /**
   * Removes a session from the pool through the UUID represented by the encrypted byte array.
   * 
   * @param encryptedUuid the encrypted byte array representing the NoSession UUID.
   * @throws NoAdapterException - if the adapter is unable to shred the session.
   */
  public void shredNoSession(byte[] encryptedUuid) throws NoAdapterException;

  /**
   * Returns the NoSession represented by the given encrypted UUID.
   * 
   * @param encryptedUuid the encrypted byte array representing the NoSession UUID.
   * @return the NoSession object with the UUID equal to the decrypted byte array provided.
   * @throws NoAdapterException - if the adapter is unable to get the NoSession.
   * @throws NoSessionExpiredException - if the NoSession object cannot be found.
   */
  public NoSession getNoSession(byte[] encryptedUuid) throws NoAdapterException,
      NoSessionExpiredException;

  /**
   * Returns a Collection of NoByteSet objects tied to the given PublicKey address.
   * 
   * @param address the PublicKey address the NoByteSets are addressed to.
   * @return a Collection of NoByteSets. If none are associated with the PublicKey address, it
   *         should return an empty collection, not a null object.
   * @throws NoAdapterException
   */
  public Collection<NoByteSet> pollNoByteSets(PublicKey address) throws NoAdapterException;

  /**
   * Adds a single NoByteSet to the addressed PublicKey address.
   * 
   * @param byteSet the NoByteSet to add to the byte set pool
   * @param address the address to queue the NoByteSet object for
   * @throws NoAdapterException - if the adapter is unable to add the NoByteSet object to the pool
   */
  public void addNoByteSet(NoByteSet byteSet, PublicKey address) throws NoAdapterException;

  /**
   * Adds a list of NoByteSet objects to the addressed PublicKey address.
   * 
   * @param byteSets the NoByteSet list to add to the byte set pool
   * @param address the address to queue the NoByteSets for
   * @throws NoAdapterException - if the adapter is unable to add the list of NoByteSet objects to
   *         the pool.
   */
  public void addNoByteSets(Collection<NoByteSet> byteSets, PublicKey address)
      throws NoAdapterException;
}
