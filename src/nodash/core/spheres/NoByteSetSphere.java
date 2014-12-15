package nodash.core.spheres;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import nodash.models.NoByteSet;
import nodash.models.NoUser;

public final class NoByteSetSphere {
	private static final ArrayList<NoByteSet> EMPTY_BYTESET_LIST = new ArrayList<NoByteSet>(0);
	
	private static ConcurrentHashMap<PublicKey, ArrayList<NoByteSet>> byteSets = new ConcurrentHashMap<PublicKey, ArrayList<NoByteSet>>();
	
	public static void add(NoByteSet byteSet, PublicKey publicKey) {
		if (!NoByteSetSphere.byteSets.containsKey(publicKey)) {
			NoByteSetSphere.byteSets.put(publicKey, new ArrayList<NoByteSet>());
		}
		NoByteSetSphere.byteSets.get(publicKey).add(byteSet);
	}
	
	public static void addList(ArrayList<NoByteSet> byteSetList, PublicKey publicKey) {
		if (!NoByteSetSphere.byteSets.containsKey(publicKey)) {
			NoByteSetSphere.byteSets.put(publicKey, new ArrayList<NoByteSet>());
		}
		NoByteSetSphere.byteSets.get(publicKey).addAll(byteSetList);
	}
	
	public static ArrayList<NoByteSet> consume(NoUser user) {
		if (NoByteSetSphere.byteSets.containsKey(user.getRSAPublicKey())) {
			ArrayList<NoByteSet> result = NoByteSetSphere.byteSets.get(user.getRSAPublicKey());
			NoByteSetSphere.byteSets.remove(user.getRSAPublicKey());
			return result;
		} else {
			return NoByteSetSphere.EMPTY_BYTESET_LIST;
		}
	}
}
