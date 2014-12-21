package nodash.core.spheres;

import java.io.IOException;

import nodash.models.NoUser;

public interface NoHashSphereInterface {
	public void setup();
	public void saveToFile() throws IOException;
	public void addNewNoUser(NoUser user) throws IOException;
	public void insertHash(String hash) throws IOException;
	public void removeHash(String hash) throws IOException;
	public boolean checkHash(String hash);
	public long size();
}
