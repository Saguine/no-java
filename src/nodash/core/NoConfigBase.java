package nodash.core;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import nodash.exceptions.NoDashFatalException;

public abstract class NoConfigBase implements NoConfigInterface {
	protected SecretKey secretKey;
	protected boolean saveDatabase;
	protected boolean saveByteSets;
	
	protected final void generateSecretKey() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance(NoUtil.CIPHER_KEY_SPEC);
			keyGenerator.init(NoUtil.AES_STRENGTH);
			this.secretKey = keyGenerator.generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new NoDashFatalException("Value for CIPHER_KEY_SPEC not valid.", e);
		}
	}

	@Override
	public void construct() {
		this.generateSecretKey();
	}

	@Override
	public SecretKey getSecretKey() {
		return this.secretKey;
	}

	@Override
	public boolean saveDatabase() {
		return this.saveDatabase;
	}

	@Override
	public boolean saveByteSets() {
		return this.saveByteSets;
	}

	@Override
	public abstract void saveNoConfig();

	@Override
	public abstract NoConfigInterface loadNoConfig() throws IOException;

}
