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

public final class NoCore {
  private NoAdapter adapter;

  public NoCore(NoAdapter adapter) {
    this.adapter = adapter;
  }

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

  public byte[] login(byte[] data, char[] password) throws NoUserNotValidException,
      NoUserAlreadyOnlineException, NoSessionExpiredException {
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
      try {
        session.consume(byteSet);
      } catch (NoByteSetBadDecryptionException e) {
        throw new NoDashFatalException("Bad byte sets on consumption.", e);
      } catch (NoSessionConfirmedException e) {
        throw new NoDashFatalException("NoSession is confirmed despite being newly created.");
      } finally {
        try {
          adapter.addNoByteSets(session.getIncomingSafe(), session.getNoUserSafe()
              .getRsaPublicKey());
        } catch (NoAdapterException e) {
          throw new NoDashFatalException("Could not return failed byte sets to pool.", e);
        }
      }
    }

    /* 6. Check the session to see if the incoming actions have modified it at all. */
    try {
      session.check();
    } catch (NoSessionConfirmedException e) {
      throw new NoDashFatalException("NoSession is confirmed despite being newly created.");
    }

    return session.getEncryptedUuid();
  }

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

  public NoState getSessionState(byte[] cookie) throws NoSessionConfirmedException,
      NoSessionExpiredException {
    return getNoSession(cookie).getNoState();
  }

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

  public void confirm(byte[] cookie, char[] password, byte[] data) throws NoSessionExpiredException, NoSessionConfirmedException, NoSessionNotAwaitingConfirmationException, NoUserNotValidException {
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
