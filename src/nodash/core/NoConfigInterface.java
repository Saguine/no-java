package nodash.core;

import java.io.IOException;

import javax.crypto.SecretKey;

public interface NoConfigInterface {
	public void construct();
	public SecretKey getSecretKey();
	public boolean saveDatabase();
	public boolean saveByteSets();
	public void saveNoConfig();
	public NoConfigInterface loadNoConfig() throws IOException;
}
