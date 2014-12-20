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
 * The NoConfig is a means to store the server secret key, database address
 * and other configs.
 */

package nodash.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import nodash.exceptions.NoDashFatalException;

public class NoConfig implements Serializable {
	private static final long serialVersionUID = -8498303909736017075L;

	public static final String CONFIG_FILENAME = "noconfig.cfg";
	
	public SecretKey secretKey;
	
	public boolean saveDatabase = true;
	public String databaseFilename = "nodatabase.hash";
	public boolean saveByteSets = false;
	public String byteSetFilename = "";
	
	public NoConfig() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance(NoUtil.CIPHER_KEY_SPEC);
			keyGenerator.init(NoUtil.AES_STRENGTH);
			this.secretKey = keyGenerator.generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new NoDashFatalException("Value for CIPHER_KEY_SPEC not valid.");
		}
	}
	
	public void saveNoConfigToFile(File file) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
			byte[] data = baos.toByteArray();
		
			Files.write(file.toPath(), data, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			throw new NoDashFatalException("Unable to save config, including generated secret key.");
		}
	}
	
	public static NoConfig getNoConfigFromFile(File file) {
		try {
			byte[] data = Files.readAllBytes(file.toPath());
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ObjectInputStream ois = new ObjectInputStream(bais);
			NoConfig noConfig;
			try {
				noConfig = (NoConfig) ois.readObject();
			} catch (ClassNotFoundException e) {
				throw new NoDashFatalException("Given bytestream does not compile into a configuration object.");
			}
			return noConfig;
		} catch (IOException e) {
			throw new NoDashFatalException("Instructed to read config from file but unable to do so.");
		}
	}
}
