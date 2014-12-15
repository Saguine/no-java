package nodash.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import sun.security.rsa.RSAPublicKeyImpl;
import nodash.core.NoUtil;
import nodash.exceptions.NoByteSetBadDecryptionException;

public class NoUser implements Serializable {
	private static final long serialVersionUID = 7132405837081692211L;
	private PublicKey publicKey;
	private PrivateKey privateKey;
	
	public int influences;
	public int actions;
	
	private ArrayList<NoAction> outgoing = new ArrayList<NoAction>();
	
	public NoUser()  {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(NoUtil.KEYPAIR_ALGORITHM);
			kpg.initialize(NoUtil.RSA_STRENGTH, SecureRandom.getInstance(NoUtil.SECURERANDOM_ALGORITHM, NoUtil.SECURERANDOM_PROVIDER));
			KeyPair keyPair = kpg.generateKeyPair();
			this.publicKey = keyPair.getPublic();
			this.privateKey = keyPair.getPrivate();
			this.influences = 0;
			this.actions = 0;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
	}

	public final byte[] createFile(char[] password) {
		ArrayList<NoAction> temp = this.outgoing;
		try {
			this.outgoing = new ArrayList<NoAction>();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
			byte[] encrypted = NoUtil.encryptByteArray(baos.toByteArray(), password);
			oos.close();
			baos.close();
			return encrypted;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			this.outgoing = temp;
		}
		return null;
	}
	
	public final byte[] createHash() {
		ArrayList<NoAction> temp = this.outgoing;
		try {
			this.outgoing = new ArrayList<NoAction>();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
			byte[] userBytes = baos.toByteArray();
			return NoUtil.getHashFromByteArray(userBytes);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			this.outgoing = temp;
		}
		return null;
	} 
	
	public final String createHashString() {
		return new String(this.createHash());
	}

	public final void consume(NoByteSet byteSet) throws NoByteSetBadDecryptionException {
		try {
			SecretKey secretKey = new SecretKeySpec(decryptRSA(byteSet.key), NoUtil.CIPHER_KEY_SPEC);
			byte[] key = secretKey.getEncoded();
			secretKey = null;
			NoInfluence influence = NoInfluence.decrypt(byteSet.data, key);
			NoUtil.wipeBytes(key);
			
			influence.applyTo(this);
			this.influences++;
		} catch (BadPaddingException e) {
			throw new NoByteSetBadDecryptionException(e);
		} catch (IllegalBlockSizeException e) {
			throw new NoByteSetBadDecryptionException(e);
		} catch (ClassNotFoundException e) {
			throw new NoByteSetBadDecryptionException(e);
		} catch (InvalidKeyException e) {
			throw new NoByteSetBadDecryptionException(e);
		}
	}
	
	public final void addAction(NoAction action) {
		this.outgoing.add(action);
		this.actions++;
	}

	public final ArrayList<NoAction> getNoActions() {
		return this.outgoing;
	}
	
	public final BigInteger getPublicExponent() {
		return ((RSAPublicKeyImpl) publicKey).getPublicExponent();
	}
	
	public final BigInteger getModulus() {
		return ((RSAPublicKeyImpl) publicKey).getModulus();
	}
	
	public final PublicKey getRSAPublicKey() {
		return (RSAPublicKeyImpl) this.publicKey;
	}
	
	private final byte[] decryptRSA(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		return NoUtil.decryptRSA(data, this.privateKey);
	}
	
	public static NoUser createUserFromFile(byte[] data, char[] password) throws IllegalBlockSizeException, BadPaddingException, IOException, ClassNotFoundException {
		byte[] decrypted = NoUtil.decryptByteArray(data, password);
		ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);
		ObjectInputStream ois = new ObjectInputStream(bais);
		NoUser noUser = (NoUser) ois.readObject();
		ois.close();
		bais.close();
		return noUser;
	}
	
}