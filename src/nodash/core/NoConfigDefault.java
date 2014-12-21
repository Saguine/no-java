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

import nodash.exceptions.NoDashFatalException;

public final class NoConfigDefault extends NoConfigBase implements Serializable {
	private static final long serialVersionUID = -8498303909736017075L;

	private static final String CONFIG_FILENAME = "noconfig.cfg";
	
	private String databaseFileName = "nodatabase.hash";

	@Override
	public boolean saveDatabase() {
		return saveDatabase;
	}
	
	public String getDatabaseName() {
		return databaseFileName;
	}

	@Override
	public boolean saveByteSets() {
		return saveByteSets;
	}

	@Override
	public void saveNoConfig() {
		try {
			File file = new File(NoConfigDefault.CONFIG_FILENAME);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
			byte[] data = baos.toByteArray();
		
			Files.write(file.toPath(), data, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			throw new NoDashFatalException("Unable to save config, including generated secret key.");
		}
	}

	@Override
	public NoConfigInterface loadNoConfig() throws IOException {
		File file = new File(NoConfigDefault.CONFIG_FILENAME);
		byte[] data = Files.readAllBytes(file.toPath());
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		ObjectInputStream ois = new ObjectInputStream(bais);
		NoConfigInterface noConfig;
		try {
			noConfig = (NoConfigDefault) ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new NoDashFatalException("Given bytestream does not compile into a configuration object.");
		}
		return noConfig;
	}
}
