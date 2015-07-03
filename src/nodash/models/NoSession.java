package nodash.models;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.codec.binary.Base64;

import nodash.core.NoAdapter;
import nodash.core.NoCore;
import nodash.core.NoUtil;
import nodash.exceptions.NoByteSetBadDecryptionException;
import nodash.exceptions.NoDashFatalException;
import nodash.exceptions.NoDashSessionBadUuidException;
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

  private Collection<NoByteSet> incoming;
  private NoUser current;
  private String uuid;

  public NoSession() {
    this.state = NoState.IDLE;
    this.expiry = System.currentTimeMillis() + NoSession.SESSION_DURATION;
    this.uuid = UUID.randomUUID().toString();
  }

  public NoSession(NoUser newUser) {
    this();
    this.state = NoState.MODIFIED;
    this.original = null;
    this.current = newUser;
    this.newUserSession = true;
  }

  public NoSession(NoAdapter adapter, byte[] data, char[] password) throws NoUserNotValidException {
    this();
    this.newUserSession = false;
    this.state = NoState.IDLE;
    try {
      this.original = NoUser.createUserFromFile(data, password);
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
    check();
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
    touchState();
    this.state = NoState.AWAITING_CONFIRMATION;
    byte[] file = this.current.createFile(password);
    NoUtil.wipeChars(password);
    return file;
  }

  public void confirmSave(NoAdapter adapter, byte[] confirmData, char[] password) throws NoSessionConfirmedException,
      NoSessionExpiredException, NoSessionNotAwaitingConfirmationException, NoUserNotValidException {
    check();
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
      /* 5.2.3: clear influences as they will not need to be re-applied */
      this.incoming = new ArrayList<NoByteSet>();
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
        action.execute(adapter);
        action.purge();
      }
    } else {
      throw new NoUserNotValidException();
    }
  }

  public NoState getNoState() throws NoSessionConfirmedException, NoSessionExpiredException {
    touchState();
    return this.state;
  }

  public NoUser getNoUser() throws NoSessionConfirmedException, NoSessionExpiredException {
    check();
    return this.current;
  }
  
  public NoUser getNoUserSafe() {
    return this.current;
  }
  
  public Collection<NoByteSet> getIncoming() throws NoSessionConfirmedException, NoSessionExpiredException {
    check();
    return this.incoming;
  }
  
  public Collection<NoByteSet> getIncomingSafe() {
    return this.incoming;
  }
  
  public String getUuid() {
    return this.uuid;
  }

  public byte[] getEncryptedUuid() {
    return NoUtil.encrypt(Base64.decodeBase64(getUuid()));
  }

  public byte[] getOriginalHash() {
    if (this.original != null) {
      return this.original.createHash();
    } else {
      return null;
    }
  }
  
  private static String decryptUuid(String data) throws NoDashSessionBadUuidException {
    return decryptUuid(Base64.decodeBase64(data));
  }

  private static String decryptUuid(byte[] data) throws NoDashSessionBadUuidException {
    if (data == null) {
      throw new NoDashSessionBadUuidException();
    }

    try {
      return Base64.encodeBase64String(NoUtil.decrypt(data));
    } catch (IllegalArgumentException e) {
      throw new NoDashSessionBadUuidException();
    } catch (IllegalBlockSizeException e) {
      throw new NoDashSessionBadUuidException();
    } catch (BadPaddingException e) {
      throw new NoDashSessionBadUuidException();
    }
  }
  
  public void setIncoming(Collection<NoByteSet> incoming) {
    this.incoming = incoming;
  }

  public void consume(NoByteSet byteSet) throws NoByteSetBadDecryptionException, NoSessionConfirmedException, NoSessionExpiredException {
    check();
    this.current.consume(byteSet);
  }

  public void close() {
    this.state = NoState.CLOSED;
  }
}
