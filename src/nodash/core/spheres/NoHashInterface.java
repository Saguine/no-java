package nodash.core.spheres;

import nodash.models.NoUser;

public interface NoHashInterface {
	public void setup();
	public void saveToFile();
	public void addNewNoUser(NoUser user);
	public void insertHash(String hash);
	public void removeHash(String hash);
	public boolean checkHash(String hash);
	public int size();
}
