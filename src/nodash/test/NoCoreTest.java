package nodash.test;

import java.util.Arrays;

import com.sun.istack.internal.logging.Logger;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import nodash.core.NoCore;
import nodash.core.NoRegister;
import nodash.exceptions.NoDashSessionBadUUIDException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoSession.NoState;

public class NoCoreTest {
	private static final String CHANGE = "change-string";
	private static final String CHANGE2 = "different-value";
	private static final String PASSWORD = "password";
	private static final String BAD_PASSWORD = "bad-password";
	private static final Logger logger = Logger.getLogger(NoCoreTest.class);
	
	private static boolean printStackTraces = false;
	
	public static void setPrintStackTraces(boolean toggle) {
		printStackTraces = toggle;
	}
	
	private static class TestTicker {
		private int run;
		private int passed;
		
		public void test(boolean result) {
			run++;
			if (result) {
				passed++;
			}
		}
		
		public boolean passed() {
			return run == passed;
		}
		
		public int getRun() {
			return run;
		}
		
		public int getPassed() {
			return passed;
		}
		
		public String getResultMessage() {
			return "Passed " + getPassed() + " out of " + getRun() + " tests.";
		}
		
		public void logResultMessage() {
			if (passed()) {
				logger.info(getResultMessage());
			} else {
				logger.severe(getResultMessage());
			}
		}
	}
	
	private static void printIf(Exception e) {
		if (printStackTraces) {
			logger.severe(e.getStackTrace().toString());
		}
	}
	
	private static void checkSetup() {
		if (!NoCore.isReady()) {
			throw new NoTestNotReadyException("NoCore is not ready to test.");
		}
	}
	
	private static byte[] modifyByteArray(byte[] array) {
		if (array.length > 0) {
			if (array[0] == 'A') {
				array[0] = 'B';
			} else {
				array[0] = 'A';
			}
		}
		return array;
	}
	
	private static byte[] copy(byte[] array) {
		return Arrays.copyOf(array, array.length);
	}
	
	/*
	 * BEGIN Registration Methods
	 */
	public static boolean testRegistrationFailureBadCookie() {
		logger.info("Testing registration failure with a bad cookie.");
		checkSetup();
		NoUserTest user = new NoUserTest(CHANGE);
		NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
		byte[] cookie = modifyByteArray(register.cookie);
		try {
			NoCore.confirm(cookie, PASSWORD.toCharArray(), register.data);
			logger.severe("Registration with bad cookie throws no errors.");
		} catch (NoDashSessionBadUUIDException e) {
			logger.info("NoDashSessionBadUUIDException thrown, passed.");
			return true;
		} catch (Exception e) {
			logger.severe("Wrong error thrown, should have been NoDashSessionBadUUIDException, was " + e.getClass().getSimpleName());
			printIf(e);
		}
		return false;
	}
	
	public static boolean testRegistrationFailureBadData() {
		logger.info("Testing registration failure with a bad data stream.");
		checkSetup();
		NoUserTest user = new NoUserTest(CHANGE);
		NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
		byte[] data = modifyByteArray(register.data);
		try {
			NoCore.confirm(register.cookie, PASSWORD.toCharArray(), data);
			logger.severe("Registration with bad d throws no errors.");
		} catch (NoUserNotValidException e) {
			logger.info("NoUserNotValidException thrown, passed.");
			return true;
		} catch (Exception e) {
			logger.severe("Wrong error thrown, should have been NoUserNotValidException, was " + e.getClass().getSimpleName());
			printIf(e);
		}
		return false;
	}
	
	public static boolean testRegistrationFailureBadPassword() {
		logger.info("Testing registration failure with a bad password.");
		checkSetup();
		NoUserTest user = new NoUserTest(CHANGE);
		NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
		try {
			NoCore.confirm(register.cookie, BAD_PASSWORD.toCharArray(), register.data);
			logger.severe("Registration with bad d throws no errors.");
		} catch (NoUserNotValidException e) {
			logger.info("NoUserNotValidException thrown, passed.");
			return true;
		} catch (Exception e) {
			logger.severe("Wrong error thrown, should have been NoUserNotValidException, was " + e.getClass().getSimpleName());
			printIf(e);
		}
		return false;
	}
	
	public static boolean testRegistrationFailure() {
		logger.info("Testing registration failure.");
		checkSetup();
		
		TestTicker ticker = new TestTicker();
		
		ticker.test(testRegistrationFailureBadCookie());
		ticker.test(testRegistrationFailureBadData());
		ticker.test(testRegistrationFailureBadPassword());
		
		ticker.logResultMessage();
		return ticker.passed();
	}
	
	public static boolean testRegistrationSuccess() {
		logger.info("Testing successful registration.");
		checkSetup();

		NoUserTest user = new NoUserTest(CHANGE);
		NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
		try {
			NoCore.confirm(register.cookie, PASSWORD.toCharArray(), register.data);
		} catch (Exception e) {
			logger.severe("Error thrown on confirm, of type " + e.getClass().getSimpleName());
			printIf(e);
			return false;
		}
		
		logger.info("Registration completed without errors. Attempting login to confirm.");
		byte[] cookie;
		try {
			cookie = NoCore.login(register.data, PASSWORD.toCharArray());
		} catch (Exception e) {
			logger.severe("Error thrown on login, of type " + e.getClass().getSimpleName());
			printIf(e);
			return false;
		}
		
		NoUserTest accessed;
		try {
			accessed = (NoUserTest) NoCore.getUser(cookie);
		} catch (Exception e) {
			logger.severe("Error thrown on getUser, of type " + e.getClass().getSimpleName());
			printIf(e);
			return false;
		}
		
		if (!accessed.getPublicExponent().equals(user.getPublicExponent())) {
			logger.severe("Received user object from getUser has a different Public Exponent to the registered user.");
			return false;
		}
		
		logger.info("Successfully registered, logged in and retrieved user information.");
		return true;
	}
	
	public static boolean testRegistration() {
		logger.info("Testing registration paths.");
		checkSetup();
		
		TestTicker ticker = new TestTicker();
		
		ticker.test(testRegistrationFailure());
		ticker.test(testRegistrationSuccess());
		
		ticker.logResultMessage();
		return ticker.passed();
	}
	
	/*
	 * END Registration Methods
	 * 
	 * BEGIN Login methods
	 */
	
	private static byte[] registerAndGetBytes() {
		logger.info("Registering...");

		NoUserTest user = new NoUserTest(CHANGE);
		logger.info("Generated user, changeableString: " + user.getChangableString());
		NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
		try {
			NoCore.confirm(register.cookie, PASSWORD.toCharArray(), register.data);
		} catch (Exception e) {
			logger.severe("Error encountered while trying to register, of type " + e.getClass().getSimpleName());
			printIf(e);
			throw new NoTestNotReadyException("Failed to set up user file.");
		}
		return register.data;
	}
	
	public static boolean testLoginFailBadPassword(byte[] data) {
		logger.info("Testing login with bad password.");
		checkSetup();
		
		byte[] cookie = null;
		try {
			cookie = NoCore.login(copy(data), BAD_PASSWORD.toCharArray());
			logger.severe("Cookie (" + Base64.encode(cookie) + ") returned, even with bad password.");
		} catch (NoUserNotValidException e) {
			logger.info("NoUserNotValidException thrown, passed.");
			return true;
		} catch (Exception e) {
			logger.severe("Wrong error thrown, should have been NoUserNotValidException, was " + e.getClass().getSimpleName());
			printIf(e);
		} finally {
			NoCore.shred(cookie);
		}
		
		return false;
	}
	
	public static boolean testLoginFailBadData(byte[] data) {
		logger.info("Testing login with bad data.");
		checkSetup();
		
		byte[] dataCopy = copy(data);
		dataCopy = modifyByteArray(dataCopy);
		byte[] cookie = null;
		try {
			cookie = NoCore.login(dataCopy, PASSWORD.toCharArray());
			logger.severe("Cookie (" + Base64.encode(cookie) + ") returned, even with bad data.");
		} catch (NoUserNotValidException e) {
			logger.info("NoUserNotValidException thrown, passed.");
			return true;
		} catch (Exception e) {
			logger.severe("Wrong error thrown, should have been NoUserNotValidException, was " + e.getClass().getSimpleName());
			printIf(e);
		} finally {
			NoCore.shred(cookie);
		}
		
		return false;
	}
	
	public static boolean testLoginFailMultipleSessions(byte[] data) {
		logger.info("Testing that multiple sessions throw an error.");
		checkSetup();
		
		byte[] cookie;
		try {
			cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
		} catch (Exception e) {
			logger.severe("Error thrown, should have logged in, was " + e.getClass().getSimpleName());
			printIf(e);
			return false;
		}
		
		byte[] secondCookie = null;
		try {
			secondCookie = NoCore.login(copy(data), PASSWORD.toCharArray());
			logger.severe("Cookie (" + Base64.encode(secondCookie) + ") returned, even with concurrent session.");
		} catch (NoUserAlreadyOnlineException e) {
			logger.info("NoUserAlreadyOnlineException thrown, passed.");
			return true;
		} catch (Exception e) {
			logger.severe("Wrong error thrown, should have been NoUserNotValidException, was " + e.getClass().getSimpleName());
			printIf(e);
		} finally {
			NoCore.shred(secondCookie);
		}
		
		NoCore.shred(cookie);
		return false;
	}
	
	public static boolean testLoginFail() {
		return testLoginFail(registerAndGetBytes());
	}
	
	public static boolean testLoginFail(byte[] data) {
		logger.info("Testing login failure methods.");
		checkSetup();
		
		TestTicker ticker = new TestTicker();
		ticker.test(testLoginFailBadPassword(data));
		ticker.test(testLoginFailBadData(data));
		ticker.test(testLoginFailMultipleSessions(data));
		
		ticker.logResultMessage();
		return ticker.passed();
	}
	
	public static boolean testLoginSuccess(byte[] data) {
		logger.info("Testing successful login and user/state retrieval.");
		checkSetup();
		
		byte[] cookie = null;
		try {
			cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
		} catch (Exception e) {
			logger.severe("Error thrown, should have logged in, was " + e.getClass().getSimpleName());
			printIf(e);
			return false;
		}
		
		try {
			NoState state = NoCore.getSessionState(cookie);
			if (state != NoState.IDLE) {
				logger.severe("Returned state is not IDLE, instead '" + state.toString() + "'");
				return false;
			}
		} catch (Exception e) {
			logger.severe("Error thrown, should have returned state, was " + e.getClass().getSimpleName());
			printIf(e);
			return false;
		}
		
		NoUserTest user = null;
		try {
			user = (NoUserTest) NoCore.getUser(cookie);
		} catch (Exception e) {
			logger.severe("Error thrown, should have returned user, was " + e.getClass().getSimpleName());
			printIf(e);
			return false;
		}

		logger.info("User login successful, changableString: " + user.getChangableString());
		NoCore.shred(cookie);
		return true;
	}
	
	public static boolean testLogin() {
		return testLogin(registerAndGetBytes());
	}
	
	public static boolean testLogin(byte[] data) {
		logger.info("Testing all login methods.");
		checkSetup();
		
		TestTicker ticker = new TestTicker();
		ticker.test(testLoginFail(data));
		ticker.test(testLoginSuccess(data));
		
		ticker.logResultMessage();
		return ticker.passed();
	}
	
	/*
	 * END Login methods
	 * BEGIN Login-Logout methods
	 */
	
	public static boolean testLoginModifyLogoutFail(byte[] data) {
		logger.info("Testing login, change changableString, save-logout.");
		checkSetup();
		
		byte[] cookie = null;
		try {
			cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
		} catch (Exception e) {
			logger.severe("Error thrown, should have returned cookie, was " + e.getClass().getSimpleName());
			printIf(e);
			return false;
		}
		
		return true;
	}
	
	/*
	 * END Login-Logout methods
	 */
	
	public static void testAll() {
		logger.info("Running all tests.");
		checkSetup();
		
		byte[] data = registerAndGetBytes();
		
		TestTicker ticker = new TestTicker();
		ticker.test(testRegistration());
		ticker.test(testLogin(data));
		
		ticker.logResultMessage();
	}
	
	public static void main(String[] args) {
		if (NoCore.isReady()) {
			testAll();
		} else {
			NoCore.setup();
			testAll();
		}
	}
}
