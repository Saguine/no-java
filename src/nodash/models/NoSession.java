package nodash.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

import nodash.core.NoAdapter;
import nodash.core.NoUtil;
import nodash.exceptions.NoByteSetBadDecryptionException;
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

  private List<NoByteSet> incoming;
  private NoUser current;
  private String uuid;

  private NoSession() {
    this.state = NoState.IDLE;
    this.expiry = System.currentTimeMillis() + NoSession.SESSION_DURATION;
    this.uuid = UUID.randomUUID().toString();
  }

  public NoSession(NoUser newUser) {
    this();
    if (newUser == null) {
      throw new NullPointerException("Session cannot be created with null user.");
    }
    this.state = NoState.MODIFIED;
    this.original = null;
    this.current = newUser;
  }

  public NoSession(byte[] data, char[] password) throws NoUserNotValidException {
    this();
    this.state = NoState.IDLE;
    byte[] originalData = Arrays.copyOf(data, data.length);
    char[] originalPassword = Arrays.copyOf(password, password.length);
    this.original =
        NoUser.createUserFromFile(originalData, originalPassword, NoUtil.NO_USER_CLASS);
    this.current = NoUser.createUserFromFile(data, password, NoUtil.NO_USER_CLASS);
    NoUtil.wipeBytes(data);
    NoUtil.wipeChars(password);
    this.uuid = UUID.randomUUID().toString();
  }

  public void check() throws NoSessionConfirmedException, NoSessionExpiredException {
    if (state == NoState.CONFIRMED) {
      throw new NoSessionConfirmedException();
    } else if (state == NoState.CLOSED || System.currentTimeMillis() > expiry) {
      state = NoState.CLOSED;
      throw new NoSessionExpiredException();
    } else if (state == NoState.IDLE 
        && (current.getNoActions().size() > 0 
            || current.createHashString().equals(original.createHashString()))) {
      state = NoState.MODIFIED;
    }
  }

  public NoState touchState() throws NoSessionConfirmedException, NoSessionExpiredException {
    check();
    if (original == null) {
      if (state != NoState.AWAITING_CONFIRMATION) {
        state = NoState.MODIFIED;
      }
    } else {
      String originalHash = this.original.createHashString();
      String currentHash = this.current.createHashString();
      if (originalHash.equals(currentHash)) {
        state = NoState.IDLE;
      } else if (this.state != NoState.AWAITING_CONFIRMATION) {
        state = NoState.MODIFIED;
      }
    }
    return state;
  }

  public byte[] initiateSaveAttempt(char[] password)
      throws NoSessionConfirmedException, NoSessionExpiredException {
    touchState();
    this.state = NoState.AWAITING_CONFIRMATION;
    byte[] file = current.createFile(password);
    NoUtil.wipeChars(password);
    return file;
  }

  public void confirmSave(NoAdapter adapter, byte[] confirmData, char[] password)
      throws NoSessionConfirmedException, NoSessionExpiredException,
      NoSessionNotAwaitingConfirmationException, NoUserNotValidException {
    check();
    if (this.state != NoState.AWAITING_CONFIRMATION) {
      throw new NoSessionNotAwaitingConfirmationException();
    }

    NoUser confirmed = NoUser.createUserFromFile(confirmData, password, NoUtil.NO_USER_CLASS);

    NoUtil.wipeBytes(confirmData);
    NoUtil.wipeChars(password);
    
    if (confirmed.createHashString().equals(current.createHashString())) {
      this.state = NoState.CONFIRMED;
      /* 5.2.3: clear influences as they will not need to be re-applied */
      this.incoming = new ArrayList<NoByteSet>();
      List<NoAction> actions = current.getNoActions();
      this.incoming = null;
      /* 5.2.4: execute NoActions */
      for (NoAction action : actions) {
        /*
         * It is assumed that actions are not long-running tasks 
         * It is also assumed that actions have the information they need without the user objects
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
    return current;
  }

  public NoUser getNoUserSafe() {
    return this.current;
  }

  public Collection<NoByteSet> getIncoming()
      throws NoSessionConfirmedException, NoSessionExpiredException {
    check();
    return this.incoming;
  }

  public List<NoByteSet> getIncomingSafe() {
    return this.incoming;
  }

  public String getUuid() {
    return this.uuid;
  }

  public byte[] getEncryptedUuid() {
    return NoUtil.encrypt(Base64.decodeBase64(getUuid()));
  }

  public byte[] getOriginalHash() {
    if (!isNewUser()) {
      return this.original.createHash();
    } else {
      return null;
    }
  }

  public void setIncoming(List<NoByteSet> incoming) {
    this.incoming = incoming;
  }

  public void consume(NoByteSet byteSet) throws NoByteSetBadDecryptionException,
      NoSessionConfirmedException, NoSessionExpiredException {
    check();
    this.current.consume(byteSet);
  }

  public void close() {
    this.state = NoState.CLOSED;
  }

  public boolean isNewUser() {
    return this.original == null;
  }
}
