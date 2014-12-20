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
 * The NoCore class is the interface between which the wrapper application 
 * (wrapplication?) accesses no- functionality.
 */

package nodash.core;

import java.io.File;
import java.security.PublicKey;

import nodash.core.spheres.NoByteSetSphere;
import nodash.core.spheres.NoHashSphere;
import nodash.core.spheres.NoSessionSphere;
import nodash.exceptions.NoDashSessionBadUUIDException;
import nodash.exceptions.NoSessionAlreadyAwaitingConfirmationException;
import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.exceptions.NoSessionNotAwaitingConfirmationException;
import nodash.exceptions.NoSessionNotChangedException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoByteSet;
import nodash.models.NoUser;
import nodash.models.NoSession.NoState;

public final class NoCore {
	public static NoConfig config;
	
	public static void setup() {
		File configFile = new File(NoConfig.CONFIG_FILENAME);
		if (configFile.exists()) {
			config = NoConfig.getNoConfigFromFile(configFile);
		} else {
			config = new NoConfig();
			config.saveNoConfigToFile(configFile);
		}
		
		NoHashSphere.setup();
	}
	
	public static byte[] login(byte[] data, char[] password) throws NoUserNotValidException, NoUserAlreadyOnlineException, NoSessionExpiredException {
		/* steps 1 through to pre-3 */
		return NoSessionSphere.login(data, password);
	}
	
	public static NoRegister register(NoUser user, char[] password)  {
		/* Straight to step 4 */
		return NoSessionSphere.registerUser(user, password);
	}
	
	public static NoUser getUser(byte[] cookie) throws NoSessionExpiredException, NoSessionConfirmedException, NoDashSessionBadUUIDException  {
		/* Facilitates step 3
		 * allow website-side modifications to the NoUser or NoUser inheritant */
		return NoSessionSphere.getUser(cookie);
	}
	
	public static NoState getSessionState(byte[] cookie) throws NoSessionExpiredException, NoSessionConfirmedException, NoDashSessionBadUUIDException {
		/* Facilitates step 3
		 * allow front-side to keep track of session state */
		return NoSessionSphere.getState(cookie);
	}
	
	public static byte[] requestSave(byte[] cookie, char[] password) throws NoSessionExpiredException, NoSessionConfirmedException, NoSessionNotChangedException, NoSessionAlreadyAwaitingConfirmationException, NoDashSessionBadUUIDException {
		/* Step 4. Provides a user with the new binary file */
		return NoSessionSphere.save(cookie, password);
	}
	
	public static void confirm(byte[] cookie, char[] password, byte[] data) throws NoSessionExpiredException, NoSessionConfirmedException, NoSessionNotAwaitingConfirmationException, NoUserNotValidException, NoDashSessionBadUUIDException {
		/* Step 5. Assumes the user has re-uploaded the file along with providing the same password. 
		 * Further attempts of getUser or getSessionState will fail with a NoSessionExpiredException*/
		NoSessionSphere.confirm(cookie, password, data);
	}

	public static void addByteSet(NoByteSet byteSet, PublicKey publicKey) {
		NoByteSetSphere.add(byteSet, publicKey);
	}
	
	public static void shred(byte[] cookie) {
		/* 3.2 Hot pull */
		NoSessionSphere.shred(cookie);
	}
	
	public static void triggerPrune() {
		NoSessionSphere.prune();
	}
	
}