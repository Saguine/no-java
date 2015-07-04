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

import nodash.exceptions.NoAdapterException;
import nodash.exceptions.NoByteSetBadDecryptionException;
import nodash.exceptions.NoDashFatalException;
import nodash.exceptions.NoSessionAlreadyAwaitingConfirmationException;
import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.exceptions.NoSessionNotAwaitingConfirmationException;
import nodash.exceptions.NoSessionNotChangedException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoByteSet;
import nodash.models.NoRegister;
import nodash.models.NoSession;
import nodash.models.NoUser;
import nodash.models.NoSession.NoState;

/**
 * The NoCore object should be the sole point of contact for the outside world. Through the life of
 * the application the NoCore class is used to get the User object, register, save and confirm in
 * the no-system.
 * 
 * @author horsey
 *
 */
public final class NoCore {
  private NoAdapter adapter;

  /**
   * Instantiates an instance of the NoCore, using the given adapter to interact with saved hashes,
   * byte sets and sessions.
   * 
   * @param adapter an object implementing the NoAdapter interface.
   */
  public NoCore(NoAdapter adapter) {
    this.adapter = adapter;
  }

  /**
   * Internal helper method to simplify getting a session or throwing the correct exception.
   * 
   * @param cookie the encrypted user cookie representing the NoSession UUID.
   * @return a NoSession object with a UUID matching the decrypted cookie.
   * @throws NoSessionExpiredException - if the session cannot be found.
   */
  private NoSession getNoSession(byte[] cookie) throws NoSessionExpiredException {
    boolean containsSession;
    try {
      containsSession = adapter.containsNoSession(cookie);
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not verify existence of session.", e);
    }

    if (containsSession) {
      try {
        return adapter.getNoSession(cookie);
      } catch (NoAdapterException e) {
        throw new NoDashFatalException("Could not get session.", e);
      }
    }
    throw new NoSessionExpiredException();
  }

  /**
   * Attempts to log a user in with a given data file and password.
   * 
   * @param data the user file as a byte array.
   * @param password the user's password as a char array.
   * @return a byte array representing the encrypted user cookie.
   * @throws NoUserNotValidException - if the given combination of data and password fails to render
   *         a user object, or if the user object's hash is not found by the internal NoAdapter.
   * @throws NoUserAlreadyOnlineException - if the discovered user's hash is already online, as
   *         determined by the internal NoAdapter's {@code isOnline} method.
   */
  public byte[] login(byte[] data, char[] password) throws NoUserNotValidException,
      NoUserAlreadyOnlineException {
    NoSession session = new NoSession(data, password);

    /* 1. Check that user is a valid user of the system based on their hash. */
    try {
      adapter.checkHash(session.getOriginalHash());
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Unable to verify user.");
    }

    /* 2. Attempt to set user to online (avoid two of the same account online at the same time) */
    try {
      adapter.goOnline(session.getOriginalHash());
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not mark user as online.", e);
    }

    /* 3. Add the session to the live session pool. */
    try {
      adapter.addNoSession(session);
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not add the session.", e);
    }

    /* 4. Transfer any incoming NoByteSets to session. */
    try {
      session.setIncoming(adapter.pollNoByteSets(session.getNoUserSafe().getRsaPublicKey()));
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not get incoming byte sets.", e);
    }

    /* 5. Apply any incoming ByteSets to the session. */
    for (NoByteSet byteSet : session.getIncomingSafe()) {
      boolean failed = true;
      try {
        session.consume(byteSet);
        failed = false;
      } catch (NoByteSetBadDecryptionException e) {
        throw new NoDashFatalException("Bad byte sets on consumption.", e);
      } catch (NoSessionConfirmedException e) {
        throw new NoDashFatalException("NoSession is confirmed despite being newly created.");
      } catch (NoSessionExpiredException e) {
        throw new NoDashFatalException("NoSession is expired despite being newly created.");
      } finally {
        if (failed) {
          try {
            adapter.addNoByteSets(session.getIncomingSafe(), session.getNoUserSafe()
                .getRsaPublicKey());
          } catch (NoAdapterException e) {
            throw new NoDashFatalException("Could not return failed byte sets to pool.", e);
          }
        }
      }
    }

    /* 6. Check the session to see if the incoming actions have modified it at all. */
    try {
      session.check();
    } catch (NoSessionConfirmedException e) {
      throw new NoDashFatalException("NoSession is confirmed despite being newly created.");
    } catch (NoSessionExpiredException e) {
      throw new NoDashFatalException("NoSession has expired despite being newly created.");
    }

    return session.getEncryptedUuid();
  }

  /**
   * Attempts to register the given NoUser object as a new user on the system.
   * 
   * @param user the NoUser object to be registered.
   * @param password the password given by the new user, as a char array.
   * @return a NoRegister object containing the encrypted session UUID and the user data file as a
   *         byte array.
   */
  public NoRegister register(NoUser user, char[] password) {
    NoSession session = new NoSession(user);
    try {
      adapter.addNoSession(session);
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Adapter could not save the session.", e);
    }

    byte[] cookie = session.getEncryptedUuid();
    byte[] userFile;
    try {
      userFile = save(cookie, password);
      adapter.goOnline(user.createHash());
    } catch (NoSessionExpiredException e) {
      throw new NoDashFatalException("Session expired despite just being created.");
    } catch (NoSessionConfirmedException e) {
      throw new NoDashFatalException("Session confirmed despite just being created.");
    } catch (NoSessionNotChangedException e) {
      throw new NoDashFatalException(
          "Session throwing NotChangedException despite being a new user");
    } catch (NoSessionAlreadyAwaitingConfirmationException e) {
      throw new NoDashFatalException(
          "Session already waiting confirmation despite just being created.");
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not go online.", e);
    } catch (NoUserAlreadyOnlineException e) {
      throw new NoDashFatalException("User with same hash is already online.");
    }

    return new NoRegister(cookie, userFile);
  }

  public NoUser getNoUser(byte[] cookie) throws NoSessionExpiredException,
      NoSessionConfirmedException {
    boolean containsSession;
    try {
      containsSession = adapter.containsNoSession(cookie);
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not verify existence of session.", e);
    }

    if (containsSession) {
      try {
        return adapter.getNoSession(cookie).getNoUser();
      } catch (NoAdapterException e) {
        throw new NoDashFatalException("Could not get session.", e);
      }
    } else {
      throw new NoSessionExpiredException();
    }
  }

  /**
   * Returns the state of the session identified by the encrypted cookie.
   * 
   * @param cookie the byte array representation of the encrypted cookie.
   * @return a NoState enum representing the state of the associated NoSession.
   * @throws NoSessionExpiredException - if the associated NoSession cannot be found.
   */
  public NoState getSessionState(byte[] cookie) throws NoSessionExpiredException {
    try {
      return getNoSession(cookie).getNoState();
    } catch (NoSessionConfirmedException e) {
      return NoState.CONFIRMED;
    }
  }

  /**
   * Initiates a save attempt on the NoSession associated to the encrypted UUID, with the provided
   * password.
   * 
   * @param cookie the byte array representation of the encrypted cookie.
   * @param password the password given by the user, as a char array.
   * @return a byte array representing the user's new file.
   * @throws NoSessionExpiredException - if the associated NoSession cannot be found or has exceeded
   *         the session time limits.
   * @throws NoSessionConfirmedException - if the associated NoSession has already been confirmed
   *         and therefore cannot be saved.
   * @throws NoSessionNotChangedException - if the NoUser associated with the session has not been
   *         changed (specifically: if the hashes of the current and original NoUser objects held by
   *         the NoSession are equal)
   * @throws NoSessionAlreadyAwaitingConfirmationException - if the associated NoSession is already
   *         awaiting confirmation.
   */
  public byte[] save(byte[] cookie, char[] password) throws NoSessionExpiredException,
      NoSessionConfirmedException, NoSessionNotChangedException,
      NoSessionAlreadyAwaitingConfirmationException {
    NoSession session = getNoSession(cookie);
    session.check();
    if (session.getNoState().equals(NoState.IDLE)) {
      throw new NoSessionNotChangedException();
    } else if (session.getNoState().equals(NoState.AWAITING_CONFIRMATION)) {
      throw new NoSessionAlreadyAwaitingConfirmationException();
    }
    return session.initiateSaveAttempt(password);
  }

  /**
   * Attempts to confirm a save attempt by re-supplying the password and byte array, along with the
   * session cookie
   * 
   * @param cookie the byte array representation of the encrypted cookie.
   * @param password the password originally given at the save attempt, as a char array.
   * @param data the byte array given by the save attempt.
   * @throws NoSessionExpiredException - if the NoSession cannot be found or has exceeded the
   *         session expiry time.
   * @throws NoSessionConfirmedException - if the NoSession has already been confirmed.
   * @throws NoSessionNotAwaitingConfirmationException - if the NoSession is not awaiting a save
   *         confirmation.
   * @throws NoUserNotValidException - if the given file and password do not generate a valid user
   *         file.
   */
  public void confirm(byte[] cookie, char[] password, byte[] data)
      throws NoSessionExpiredException, NoSessionConfirmedException,
      NoSessionNotAwaitingConfirmationException, NoUserNotValidException {
    NoSession session = getNoSession(cookie);
    byte[] oldHash = session.getOriginalHash();
    byte[] newHash = session.getNoUserSafe().createHash();

    session.confirmSave(adapter, data, password);

    try {
      adapter.insertHash(newHash);
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not insert confirmed hash.", e);
    }

    try {
      adapter.shredNoSession(cookie);
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not shred session.", e);
    }

    try {
      adapter.goOffline(session.getNoUserSafe().createHash());
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not go offline.", e);
    }

    if (!session.isNewUser()) {
      try {
        adapter.removeHash(oldHash);
      } catch (NoAdapterException e) {
        throw new NoDashFatalException("Could not remove old hash.", e);
      }
    }
  }

  /**
   * Destroys the NoSession, returning ByteSets to the pool if necessary and takes the user hash out
   * of the online pool through the adapter.
   * 
   * @param cookie the byte set representation of the encrypted NoSession UUID.
   * @throws NoSessionExpiredException - if the NoSession cannot be found or has exceeded the
   *         session expiry time.
   */
  public void shred(byte[] cookie) throws NoSessionExpiredException {
    NoSession session = getNoSession(cookie);

    try {
      adapter.shredNoSession(cookie);
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not shred session.", e);
    }

    try {
      adapter.addNoByteSets(session.getIncomingSafe(), session.getNoUserSafe().getRsaPublicKey());
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not add bytesets back into pool.");
    }
    try {
      adapter.goOffline(session.getNoUserSafe().createHash());
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not go offline.", e);
    }
  }

}
