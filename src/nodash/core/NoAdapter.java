package nodash.core;

import java.security.PublicKey;
import java.util.Collection;

import nodash.core.exceptions.NoAdapterException;
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

  public NoSession getNoSession(byte[] encryptedUuid) throws NoAdapterException;

  public Collection<NoByteSet> pollNoByteSets(PublicKey address) throws NoAdapterException;

  public void addNoByteSets(Collection<NoByteSet> noByteSets, PublicKey address)
      throws NoAdapterException;
}
