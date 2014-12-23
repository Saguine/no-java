/*
 * Copyright 2014 David Horscroft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * The NoSessionSphere stores user sessions and allows their access and 
 * manipulation with the use of their UUID.
 */

package nodash.core.spheres;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import nodash.core.NoRegister;
import nodash.exceptions.NoByteSetBadDecryptionException;
import nodash.exceptions.NoDashFatalException;
import nodash.exceptions.NoDashSessionBadUUIDException;
import nodash.exceptions.NoSessionAlreadyAwaitingConfirmationException;
import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.exceptions.NoSessionNotAwaitingConfirmationException;
import nodash.exceptions.NoSessionNotChangedException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoByteSet;
import nodash.models.NoSession;
import nodash.models.NoUser;
import nodash.models.NoSession.NoState;

public final class NoSessionSphere {
	private static ConcurrentHashMap<UUID, NoSession> sessions = new ConcurrentHashMap<UUID, NoSession>();
	private static Set<byte[]> originalHashesOnline = Collections.newSetFromMap(new ConcurrentHashMap<byte[], Boolean>());
	
	public static synchronized void prune() {
		for (UUID uuid : NoSessionSphere.sessions.keySet()) {
			pruneSingle(uuid);
		}
	}

	public static void shred(byte[] encryptedUUID) {
		try {
			UUID uuid = NoSession.decryptUUID(encryptedUUID);
			if (NoSessionSphere.sessions.containsKey(uuid)) {
				NoSession session = NoSessionSphere.sessions.get(uuid);
				NoByteSetSphere.addList(session.incoming, session.current.getRSAPublicKey());
				NoSessionSphere.originalHashesOnline.remove(session.getOriginalHash());
				NoSessionSphere.sessions.remove(uuid);
				session = null;
			}
		} catch (NoDashSessionBadUUIDException e) {
			// Suppress, doesn't matter
		}
	}
	
	public static synchronized void pruneSingle(UUID uuid) {
		NoSession session = NoSessionSphere.sessions.get(uuid);
		try {
			session.check();
		} catch (NoSessionExpiredException e) { 
			/* Resultant from 3.1 and 3.2 */
			NoByteSetSphere.addList(session.incoming, session.current.getRSAPublicKey());
			NoSessionSphere.originalHashesOnline.remove(session.getOriginalHash());
			NoSessionSphere.sessions.remove(uuid);
			session = null;
		} catch (NoSessionConfirmedException e) {
			/* Should be cleaned up at 5.2 */
		}
	}
	
	public static synchronized byte[] login(byte[] data, char[] password) throws NoUserNotValidException, NoUserAlreadyOnlineException, NoSessionExpiredException {
		/* 1. Login with byte[] data and byte[] password */
		NoSession session = new NoSession(data, password);
		/* 1.1. User currently has an online session, must wait for it to expire. */
		if (originalHashesOnline.contains(session.getOriginalHash())) {
			throw new NoUserAlreadyOnlineException();
		}
		/* 1.2. User successfully logged in: set up session records. */
		NoSessionSphere.originalHashesOnline.add(session.getOriginalHash());
		NoSessionSphere.sessions.put(session.uuid, session);
		
		/* 2. Check NoByteSetSphere for incoming Influences */
		session.incoming = NoByteSetSphere.consume(session.current);
		for (NoByteSet nbs : session.incoming) {
			/* 2.1 Decrypt NoInfluence from NoByteSet, let the current user consume them */
			try {
				session.consume(nbs);
			} catch (NoByteSetBadDecryptionException e) {
				e.printStackTrace();
			}
		}	/* 2.2 Alternatively, no NoByteSets to consume */
		
		try {
			session.check();
		} catch (NoSessionConfirmedException e) {
			/* Should be impossible to reach */
			throw new NoDashFatalException(e);
		}
		
		/* Will set to 2.1[MODIFIED] or 2.2[IDLE] */
		
		/* Precursor to 3.; allow website to associate user session with a cookie. */
		return session.getEncryptedUUID();
	}
	
	public static NoUser getUser(byte[] encryptedUUID) throws NoDashSessionBadUUIDException, NoSessionExpiredException, NoSessionConfirmedException  {
		UUID uuid = NoSession.decryptUUID(encryptedUUID);
		if (NoSessionSphere.sessions.containsKey(uuid)) {
			NoSessionSphere.pruneSingle(uuid);
			try {
				return NoSessionSphere.sessions.get(uuid).getNoUser();
			} catch (NullPointerException e) {
				throw new NoSessionExpiredException();
			}
		}
		throw new NoSessionExpiredException();
	}
	
	public static NoState getState(byte[] encryptedUUID) throws NoDashSessionBadUUIDException, NoSessionExpiredException, NoSessionConfirmedException {
		UUID uuid = NoSession.decryptUUID(encryptedUUID);
		if (NoSessionSphere.sessions.containsKey(uuid)) {
			NoSessionSphere.pruneSingle(uuid);
			NoSession session = NoSessionSphere.sessions.get(uuid);
			return session.getNoState();
		}
		throw new NoSessionExpiredException();
	}
	
	public static synchronized byte[] save(byte[] encryptedUUID, char[] password) throws NoDashSessionBadUUIDException, NoSessionExpiredException, NoSessionConfirmedException, NoSessionNotChangedException, NoSessionAlreadyAwaitingConfirmationException {
		UUID uuid = NoSession.decryptUUID(encryptedUUID);
		if (NoSessionSphere.sessions.containsKey(uuid)) {
			NoSessionSphere.pruneSingle(uuid);
			NoSession session = NoSessionSphere.sessions.get(uuid);
			
			if (session.getNoState().equals(NoState.IDLE)) {
				throw new NoSessionNotChangedException();
			} else if (session.getNoState().equals(NoState.AWAITING_CONFIRMATION)) {
				throw new NoSessionAlreadyAwaitingConfirmationException();
			}
			return session.initiateSaveAttempt(password);
		}
		throw new NoSessionExpiredException();
	}
	
	public static synchronized void confirm(byte[] encryptedUUID, char[] password, byte[] data) throws NoDashSessionBadUUIDException, NoSessionExpiredException, NoSessionConfirmedException, NoSessionNotAwaitingConfirmationException, NoUserNotValidException {
		UUID uuid = NoSession.decryptUUID(encryptedUUID);
		if (NoSessionSphere.sessions.containsKey(uuid)) {
			NoSessionSphere.pruneSingle(uuid);
			NoSession session = NoSessionSphere.sessions.get(uuid);
			session.confirmSave(data, password);
			return;
		}
		throw new NoSessionExpiredException();
	}

	public static synchronized NoRegister registerUser(NoUser user, char[] password) {
		NoRegister result = new NoRegister();
		NoSession session = new NoSession(user);
		NoSessionSphere.sessions.put(session.uuid, session);
		result.cookie = session.getEncryptedUUID();
		try {
			result.data = NoSessionSphere.save(result.cookie, password);
		} catch (NoDashSessionBadUUIDException e) {
			throw new NoDashFatalException("Immediately generated cookie throwing bad cookie error.");
		} catch (NoSessionExpiredException e) {
			throw new NoDashFatalException("Session expired before it was even returned to client.");
		} catch (NoSessionConfirmedException e) {
			throw new NoDashFatalException("Session is in confirmed state before it was returned to client.");
		} catch (NoSessionNotChangedException e) {
			throw new NoDashFatalException("Session claims to be unchanged but user is newly registered.");
		} catch (NoSessionAlreadyAwaitingConfirmationException e) {
			throw new NoDashFatalException("Session claims to be awaiting confirmation before returning data to the user.");
		}
		return result;
	}
}