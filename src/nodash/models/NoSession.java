package nodash.models;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.codec.binary.Base64;

import nodash.core.NoCore;
import nodash.core.NoUtil;
import nodash.exceptions.NoByteSetBadDecryptionException;
import nodash.exceptions.NoDashFatalException;
import nodash.exceptions.NoDashSessionBadUUIDException;
import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.exceptions.NoSessionNotAwaitingConfirmationException;
import nodash.exceptions.NoUserNotValidException;

public final class NoSession implements Serializable {
  private static final long serialVersionUID = 1814807373427948931L;

  public static final long SESSION_DURATION = 1000 * 60 * 30; // 30 minute sessions

  public static enum NoState {
    IDLE, MODIFIED, AWAITING_CONFIRMATION, CONFIRMED, CLOSED;
  };

  private NoUser original;
  private NoState state;
  private final long expiry;
  private boolean newUserSession;

  public List<NoByteSet> incoming;
  public NoUser current;
  public UUID uuid;

  public NoSession() {
    this.state = NoState.IDLE;
    this.expiry = System.currentTimeMillis() + NoSession.SESSION_DURATION;
    this.uuid = UUID.randomUUID();
  }

  public NoSession(NoUser newUser) {
    this();
    this.state = NoState.MODIFIED;
    this.original = null;
    this.current = newUser;
    this.newUserSession = true;
  }

  public NoSession(byte[] data, char[] password) throws NoUserNotValidException {
    this();
    this.newUserSession = false;
    this.state = NoState.IDLE;
    char[] passwordDupe = password.clone();
    try {
      this.original = NoUser.createUserFromFile(data, password);
      if (NoCore.hashSphere.checkHash(this.original.createHashString())) {
        this.current = NoUser.createUserFromFile(data, passwordDupe);
        this.uuid = UUID.randomUUID();
        NoUtil.wipeBytes(data);
      } else {
        throw new NoUserNotValidException();
      }
    } catch (IOException e) {
      throw new NoUserNotValidException();
    } catch (IllegalBlockSizeException e) {
      throw new NoUserNotValidException();
    } catch (BadPaddingException e) {
      throw new NoUserNotValidException();
    } catch (ClassNotFoundException e) {
      throw new NoUserNotValidException();
    }
  }

  public void check() throws NoSessionConfirmedException, NoSessionExpiredException {
    if (this.state == NoState.CONFIRMED) {
      throw new NoSessionConfirmedException();
    } else if (this.state == NoState.CLOSED || System.currentTimeMillis() > this.expiry) {
      this.state = NoState.CLOSED;
      throw new NoSessionExpiredException();
    }
  }

  public NoState touchState() throws NoSessionConfirmedException, NoSessionExpiredException {
    this.check();
    if (this.newUserSession) {
      if (this.state != NoState.AWAITING_CONFIRMATION) {
        this.state = NoState.MODIFIED;
      }
    } else {
      String originalHash = this.original.createHashString();
      String currentHash = this.current.createHashString();
      if (originalHash.equals(currentHash)) {
        this.state = NoState.IDLE;
      } else if (this.state != NoState.AWAITING_CONFIRMATION) {
        this.state = NoState.MODIFIED;
      }
    }
    return this.state;
  }

  public byte[] initiateSaveAttempt(char[] password) throws NoSessionConfirmedException,
      NoSessionExpiredException {
    this.touchState();
    this.state = NoState.AWAITING_CONFIRMATION;
    byte[] file = this.current.createFile(password);
    NoUtil.wipeChars(password);
    return file;
  }

  public void confirmSave(byte[] confirmData, char[] password) throws NoSessionConfirmedException,
      NoSessionExpiredException, NoSessionNotAwaitingConfirmationException, NoUserNotValidException {
    this.check();
    if (this.state != NoState.AWAITING_CONFIRMATION) {
      throw new NoSessionNotAwaitingConfirmationException();
    }

    NoUser confirmed;
    try {
      confirmed = NoUser.createUserFromFile(confirmData, password);
    } catch (IOException e) {
      throw new NoUserNotValidException();
    } catch (IllegalBlockSizeException e) {
      throw new NoUserNotValidException();
    } catch (BadPaddingException e) {
      throw new NoUserNotValidException();
    } catch (ClassNotFoundException e) {
      throw new NoUserNotValidException();
    }

    NoUtil.wipeBytes(confirmData);
    NoUtil.wipeChars(password);
    if (confirmed.createHashString().equals(this.current.createHashString())) {
      this.state = NoState.CONFIRMED;
      /* 5.2: confirmed! */
      if (!this.newUserSession) {
        /* 5.2.1: remove old hash from array */
        try {
          NoCore.hashSphere.removeHash(this.original.createHashString());
        } catch (IOException e) {
          throw new NoDashFatalException("Unable to remove hash on confirm.", e);
        }
      }
      /* 5.2.2: add new hash to array */
      try {
        NoCore.hashSphere.insertHash(this.current.createHashString());
      } catch (IOException e) {
        throw new NoDashFatalException("Unable to remove hash on confirm.", e);
      }

      /* 5.2.3: clear influences as they will not need to be re-applied */
      List<NoAction> actions = this.current.getNoActions();
      this.incoming = null;
      this.original = null;
      this.current = null;
      /* 5.2.4: execute NoActions */
      for (NoAction action : actions) {
        /*
         * It is assumed that actions are not long-running tasks It is also assumed that actions
         * have the information they need without the user objects
         */
        action.execute();
        action.purge();
      }
    } else {
      throw new NoUserNotValidException();
    }
  }

  public NoState getNoState() throws NoSessionConfirmedException, NoSessionExpiredException {
    this.touchState();
    return this.state;
  }

  public NoUser getNoUser() throws NoSessionConfirmedException, NoSessionExpiredException {
    this.check();
    return this.current;
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public String getUuidAsString() {
    return this.uuid.toString();
  }

  public byte[] getEncryptedUuid() {
    return NoUtil.encrypt(Base64.encodeBase64(this.uuid.toString().getBytes()));
  }

  public String getEncryptedUuidAsString() {
    return new String(this.getEncryptedUuid());
  }

  public byte[] getOriginalHash() {
    if (this.original != null) {
      return this.original.createHash();
    } else {
      return null;
    }
  }

  public static UUID decryptUuid(byte[] data) throws NoDashSessionBadUUIDException {
    if (data == null) {
      throw new NoDashSessionBadUUIDException();
    }

    try {
      return UUID.fromString(new String(Base64.decodeBase64(NoUtil.decrypt(data))));
    } catch (IllegalArgumentException e) {
      throw new NoDashSessionBadUUIDException();
    } catch (IllegalBlockSizeException e) {
      throw new NoDashSessionBadUUIDException();
    } catch (BadPaddingException e) {
      throw new NoDashSessionBadUUIDException();
    }
  }

  public void consume(NoByteSet byteSet) throws NoByteSetBadDecryptionException {
    this.current.consume(byteSet);
  }

  public void close() {
    this.state = NoState.CLOSED;
  }
}
