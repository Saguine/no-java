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
