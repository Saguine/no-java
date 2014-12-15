package nodash.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import nodash.core.NoUtil;
import nodash.exceptions.NoDashFatalException;

public abstract class NoInfluence implements Serializable {	
	private static final long serialVersionUID = -7509462039664862920L;

	public abstract void applyTo(NoUser user);
	
	public final NoByteSet getByteSet(PublicKey publicKey)  {
		KeyGenerator keyGen;
		try {
			keyGen = KeyGenerator.getInstance(NoUtil.CIPHER_KEY_SPEC);
		} catch (NoSuchAlgorithmException e) {
			throw new NoDashFatalException("Value for CIPHER_KEY_SPEC is not valid.");
		}
		keyGen.init(NoUtil.AES_STRENGTH);
		SecretKey secretKey = keyGen.generateKey();
		System.out.println(secretKey.getClass().toString());
		byte[] key = secretKey.getEncoded();
		byte[] encryptedKey = NoUtil.encryptRSA(key, publicKey);
		byte[] data = this.getEncrypted(key);
		NoUtil.wipeBytes(key);
		return new NoByteSet(encryptedKey, data);
	}
	
	private final byte[] getEncrypted(byte[] key) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
			byte[] encrypted = NoUtil.encrypt(baos.toByteArray(), key);
			oos.close();
			baos.close();
			return encrypted;
		} catch (IOException e) {
			throw new NoDashFatalException("Unable to write NoInfluence object to byte stream.");
		}
	}
	
	public static NoInfluence decrypt(byte[] data, byte[] key) throws IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
		byte[] decrypted = NoUtil.decrypt(data, key);
		ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);
		try {
			ObjectInputStream ois = new ObjectInputStream(bais);
			NoInfluence noInfluence = (NoInfluence) ois.readObject();
			ois.close();
			bais.close();
			return noInfluence;
		} catch (IOException e) {
			throw new NoDashFatalException("Unable to read out provided data stream.");
		}
	}
	
}
