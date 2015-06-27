package nodash.test;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;

import nodash.core.NoCore;
import nodash.core.NoRegister;
import nodash.exceptions.NoDashSessionBadUUIDException;
import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoUserAlreadyOnlineException;
import nodash.exceptions.NoUserNotValidException;
import nodash.models.NoSession.NoState;

public class NoCoreTest {
  private static final String[] CHANGE = new String[] {"first-string", "second-string",
      "third-string", "forth-string", "fifth-string", "sixth-string", "seventh-string",
      "eighth-string", "ninth-string", "tenth-string"};
  private static Iterator<String> CHANGES = Arrays.asList(CHANGE).iterator();

  private static final String PASSWORD = "password";
  private static final String BAD_PASSWORD = "bad-password";
  private static final Logger logger = Logger.getLogger("NoCoreTest");

  private static boolean silent = false;
  private static boolean printStackTraces = false;

  private static Object passoverData;

  public static void setPrintStackTraces(boolean toggle) {
    printStackTraces = toggle;
  }

  public static void setSilence(boolean toggle) {
    silent = toggle;
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
        printIf(getResultMessage());
      } else {
        logger.severe(getResultMessage());
      }
    }
  }

  private static void printIf(Exception e) {
    if (printStackTraces) {
      e.printStackTrace();
    }
  }

  private static void printIf(String s) {
    if (!silent) {
      logger.info(s);
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
    printIf("Testing registration failure with a bad cookie.");
    checkSetup();
    NoUserTest user = new NoUserTest(CHANGE[0]);
    NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
    byte[] cookie = modifyByteArray(register.cookie);
    try {
      NoCore.confirm(cookie, PASSWORD.toCharArray(), register.data);
      logger.severe("Registration with bad cookie throws no errors.");
    } catch (NoDashSessionBadUUIDException e) {
      printIf("NoDashSessionBadUUIDException thrown, passed.");
      return true;
    } catch (Exception e) {
      logger.severe("Wrong error thrown, should have been NoDashSessionBadUUIDException, was "
          + e.getClass().getSimpleName());
      printIf(e);
    }
    return false;
  }

  public static boolean testRegistrationFailureBadData() {
    printIf("Testing registration failure with a bad data stream.");
    checkSetup();
    NoUserTest user = new NoUserTest(CHANGE[0]);
    NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
    byte[] data = modifyByteArray(register.data);
    try {
      NoCore.confirm(register.cookie, PASSWORD.toCharArray(), data);
      logger.severe("Registration with bad d throws no errors.");
    } catch (NoUserNotValidException e) {
      printIf("NoUserNotValidException thrown, passed.");
      return true;
    } catch (Exception e) {
      logger.severe("Wrong error thrown, should have been NoUserNotValidException, was "
          + e.getClass().getSimpleName());
      printIf(e);
    }
    return false;
  }

  public static boolean testRegistrationFailureBadPassword() {
    printIf("Testing registration failure with a bad password.");
    checkSetup();
    NoUserTest user = new NoUserTest(CHANGE[0]);
    NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
    try {
      NoCore.confirm(register.cookie, BAD_PASSWORD.toCharArray(), register.data);
      logger.severe("Registration with bad d throws no errors.");
    } catch (NoUserNotValidException e) {
      printIf("NoUserNotValidException thrown, passed.");
      return true;
    } catch (Exception e) {
      logger.severe("Wrong error thrown, should have been NoUserNotValidException, was "
          + e.getClass().getSimpleName());
      printIf(e);
    }
    return false;
  }

  public static boolean testRegistrationFailure() {
    printIf("Testing registration failure.");
    checkSetup();

    TestTicker ticker = new TestTicker();

    ticker.test(testRegistrationFailureBadCookie());
    ticker.test(testRegistrationFailureBadData());
    ticker.test(testRegistrationFailureBadPassword());

    ticker.logResultMessage();
    return ticker.passed();
  }

  public static boolean testRegistrationSuccess() {
    printIf("Testing successful registration.");
    checkSetup();

    NoUserTest user = new NoUserTest(CHANGE[0]);
    NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
    try {
      NoCore.confirm(register.cookie, PASSWORD.toCharArray(), copy(register.data));
    } catch (Exception e) {
      logger.severe("Error thrown on confirm, of type " + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }

    printIf("Registration completed without errors. Attempting login to confirm.");
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
      logger
          .severe("Received user object from getUser has a different Public Exponent to the registered user.");
      return false;
    }

    printIf("Successfully registered, logged in and retrieved user information.");
    return true;
  }

  public static boolean testRegistration() {
    printIf("Testing registration paths.");
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
    printIf("Registering...");

    NoUserTest user = new NoUserTest(CHANGES.next());
    printIf("Generated user, changeableString: " + user.getChangableString());
    NoRegister register = NoCore.register(user, PASSWORD.toCharArray());
    byte[] userFile = copy(register.data);
    try {
      NoCore.confirm(register.cookie, PASSWORD.toCharArray(), register.data);
    } catch (Exception e) {
      logger.severe("Error encountered while trying to register, of type "
          + e.getClass().getSimpleName());
      printIf(e);
      throw new NoTestNotReadyException("Failed to set up user file.");
    }
    return userFile;
  }

  public static boolean testLoginFailBadPassword(byte[] data) {
    printIf("Testing login with bad password.");
    checkSetup();

    byte[] cookie = null;
    try {
      cookie = NoCore.login(copy(data), BAD_PASSWORD.toCharArray());
      logger.severe("Cookie (" + Base64.encodeBase64(cookie)
          + ") returned, even with bad password.");
    } catch (NoUserNotValidException e) {
      printIf("NoUserNotValidException thrown, passed.");
      return true;
    } catch (Exception e) {
      logger.severe("Wrong error thrown, should have been NoUserNotValidException, was "
          + e.getClass().getSimpleName());
      printIf(e);
    } finally {
      NoCore.shred(cookie);
    }

    return false;
  }

  public static boolean testLoginFailBadData(byte[] data) {
    printIf("Testing login with bad data.");
    checkSetup();

    byte[] dataCopy = copy(data);
    dataCopy = modifyByteArray(dataCopy);
    byte[] cookie = null;
    try {
      cookie = NoCore.login(dataCopy, PASSWORD.toCharArray());
      logger.severe("Cookie (" + Base64.encodeBase64(cookie) + ") returned, even with bad data.");
    } catch (NoUserNotValidException e) {
      printIf("NoUserNotValidException thrown, passed.");
      return true;
    } catch (Exception e) {
      logger.severe("Wrong error thrown, should have been NoUserNotValidException, was "
          + e.getClass().getSimpleName());
      printIf(e);
    } finally {
      NoCore.shred(cookie);
    }

    return false;
  }

  public static boolean testLoginFailMultipleSessions(byte[] data) {
    printIf("Testing that multiple sessions throw an error.");
    checkSetup();

    byte[] cookie;
    try {
      cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
      printIf("Received cookie (" + new String(cookie) + ")");
    } catch (Exception e) {
      logger.severe("Error thrown, should have logged in, was " + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }

    byte[] secondCookie = null;
    try {
      secondCookie = NoCore.login(copy(data), PASSWORD.toCharArray());
      logger.severe("Cookie (" + new String(secondCookie)
          + ") returned, even with concurrent session.");
    } catch (NoUserAlreadyOnlineException e) {
      printIf("NoUserAlreadyOnlineException thrown, passed.");
      return true;
    } catch (Exception e) {
      logger.severe("Wrong error thrown, should have been NoUserAlreadyOnlineException, was "
          + e.getClass().getSimpleName());
      printIf(e);
    } finally {
      NoCore.shred(secondCookie);
      NoCore.shred(cookie);
    }

    return false;
  }

  public static boolean testLoginFail() {
    return testLoginFail(registerAndGetBytes());
  }

  public static boolean testLoginFail(byte[] data) {
    printIf("Testing login failure methods.");
    checkSetup();

    TestTicker ticker = new TestTicker();
    ticker.test(testLoginFailBadPassword(data));
    ticker.test(testLoginFailBadData(data));
    ticker.test(testLoginFailMultipleSessions(data));

    ticker.logResultMessage();
    return ticker.passed();
  }

  public static boolean testLoginSuccess(byte[] data) {
    printIf("Testing successful login and user/state retrieval.");
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
      logger
          .severe("Error thrown, should have returned state, was " + e.getClass().getSimpleName());
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

    printIf("User login successful, changableString: " + user.getChangableString());
    NoCore.shred(cookie);
    return true;
  }

  public static boolean testLogin() {
    return testLogin(registerAndGetBytes());
  }

  public static boolean testLogin(byte[] data) {
    printIf("Testing all login methods.");
    checkSetup();

    TestTicker ticker = new TestTicker();
    ticker.test(testLoginFail(data));
    ticker.test(testLoginSuccess(data));

    ticker.logResultMessage();
    return ticker.passed();
  }

  /*
   * END Login methods BEGIN Login-Logout methods
   */

  public static boolean testLoginModifyLogout(byte[] data) {
    printIf("Testing login, change changableString, save-logout.");
    checkSetup();

    byte[] cookie = null;
    try {
      cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
    } catch (Exception e) {
      logger.severe("Error thrown, should have returned cookie, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }
    printIf("Cookie recieved.");

    NoUserTest user;
    try {
      user = (NoUserTest) NoCore.getUser(cookie);
    } catch (Exception e) {
      logger.severe("Error thrown, should have returned user, was " + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }
    printIf("User object received.");
    String original = user.getChangableString();
    user.setChangableString(CHANGES.next());

    NoState stateModified;
    try {
      stateModified = NoCore.getSessionState(cookie);
    } catch (Exception e) {
      logger
          .severe("Error thrown, should have returned state, was " + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }
    if (stateModified != NoState.MODIFIED) {
      logger.severe("State not MODIFIED.");
      NoCore.shred(cookie);
      return false;
    }
    printIf("State is MODIFIED.");

    byte[] newData;
    try {
      newData = NoCore.requestSave(cookie, PASSWORD.toCharArray());
    } catch (Exception e) {
      logger.severe("Error thrown, should have returned new byte array, was "
          + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }
    printIf("New data stream received.");

    NoState stateAwaiting;
    try {
      stateAwaiting = NoCore.getSessionState(cookie);
    } catch (Exception e) {
      logger
          .severe("Error thrown, should have returned state, was " + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }
    if (stateAwaiting != NoState.AWAITING_CONFIRMATION) {
      logger.severe("State not AWAITING_CONFIRMATION.");
      NoCore.shred(cookie);
      return false;
    }
    printIf("State is AWAITING_CONFIRMATION.");

    try {
      NoCore.confirm(cookie, PASSWORD.toCharArray(), copy(newData));
    } catch (Exception e) {
      logger.severe("Error thrown, should have confirmed, was " + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }
    printIf("Confirm raised no errors.");
    passoverData = copy(newData);
    data = copy(newData);

    try {
      NoCore.getSessionState(cookie);
      logger.severe("Get session state threw no errors after confirmation.");
      return false;
    } catch (NoSessionConfirmedException e) {
      printIf("NoSessionConfirmed exception thrown.");
    } catch (Exception e) {
      logger.severe("Error thrown, should have been NoSessionConfirmedException, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    } finally {
      user = null;
    }

    // Log in again to check changes
    try {
      cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
    } catch (Exception e) {
      logger.severe("Error thrown, should have returned cookie, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }
    printIf("Cookie recieved for second login.");

    try {
      user = (NoUserTest) NoCore.getUser(cookie);
    } catch (Exception e) {
      logger.severe("Error thrown on second login, should have returned user, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }
    printIf("User object received on second login.");

    if (!user.getChangableString().equals(original)) {
      printIf("Changable string has changed and saved.");
      NoCore.shred(cookie);
      return true;
    } else {
      logger.severe("Changable string has not changed.");
      NoCore.shred(cookie);
      return false;
    }
  }

  public static boolean testActionInfluenceLifecycle(byte[] data) {
    printIf("Testing an action-influence cycle between two users.");
    checkSetup();

    // First, log in, get the user Public Address and save the current name, log out
    PublicKey address;
    String currentString;
    byte[] cookie = null;
    try {
      cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
      NoUserTest user = (NoUserTest) NoCore.getUser(cookie);
      address = user.getRSAPublicKey();
      currentString = user.getChangableString();
    } catch (Exception e) {
      logger.severe("Error thrown on address-getting login, gotten address and string, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    } finally {
      NoCore.shred(cookie);
    }
    printIf("Got public address.");

    // Create a second user
    byte[] secondUserData = registerAndGetBytes();
    byte[] secondUserCookie;
    try {
      secondUserCookie = NoCore.login(copy(secondUserData), PASSWORD.toCharArray());
    } catch (Exception e) {
      logger.severe("Error thrown on second user login, should have returned cookie, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }

    NoUserTest user;
    try {
      user = (NoUserTest) NoCore.getUser(secondUserCookie);
    } catch (Exception e) {
      logger.severe("Error thrown on second user login, should have returned user, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }

    // Create outgoing action
    NoActionTest action = new NoActionTest(address, CHANGES.next());
    user.addAction(action);
    printIf("Action added to second user.");

    // Save-confirm user
    try {
      secondUserData = NoCore.requestSave(secondUserCookie, PASSWORD.toCharArray());
      NoCore.confirm(secondUserCookie, PASSWORD.toCharArray(), copy(secondUserData));
    } catch (Exception e) {
      logger.severe("Error thrown on second user confirm, should have returned user, was "
          + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(secondUserCookie);
      return false;
    }
    printIf("Logged out of second user.");

    // Log in as first user, should get changes
    printIf("Logging into first user again.");
    try {
      cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
    } catch (Exception e) {
      logger.severe("Error thrown on first user, second login, should have returned cookie, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }

    NoState state;
    try {
      state = NoCore.getSessionState(cookie);
    } catch (Exception e) {
      logger.severe("Error thrown on first user, second login, should have returned state, was "
          + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }
    if (state != NoState.MODIFIED) {
      logger.severe("Was expecting state to be MODIFIED, instead was " + state.toString());
      return false;
    }

    try {
      user = (NoUserTest) NoCore.getUser(cookie);
    } catch (Exception e) {
      logger.severe("Error thrown on first user, second login, should have returned user, was "
          + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }

    if (user.getChangableString().equals(currentString)) {
      logger.severe("User information has not changed (still " + user.getChangableString() + ").");
      return false;
    }
    printIf("User string changed on first return login,  (" + currentString + " to "
        + user.getChangableString() + ")!");

    // Test that the influence resets accordingly on hotpull
    NoCore.shred(cookie);

    try {
      cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
    } catch (Exception e) {
      logger.severe("Error thrown on first user, third login, should have returned cookie, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }

    try {
      state = NoCore.getSessionState(cookie);
    } catch (Exception e) {
      logger.severe("Error thrown on first user, third login, should have returned state, was "
          + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }
    if (state != NoState.MODIFIED) {
      logger.severe("Was expecting state to be MODIFIED, instead was " + state.toString());
      return false;
    }

    try {
      user = (NoUserTest) NoCore.getUser(cookie);
    } catch (Exception e) {
      logger.severe("Error thrown on first user, third login, should have returned user, was "
          + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }

    if (user.getChangableString().equals(currentString)) {
      logger.severe("User information has not changed (still " + user.getChangableString() + ").");
      return false;
    }
    printIf("User string changed on second return login,  (" + currentString + " to "
        + user.getChangableString() + ")!");

    // Save-confirm
    try {
      data = NoCore.requestSave(cookie, PASSWORD.toCharArray());
      NoCore.confirm(cookie, PASSWORD.toCharArray(), copy(data));
      passoverData = copy(data);
    } catch (Exception e) {
      logger
          .severe("Error thrown on first user, save-confirm, was " + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }

    // Final login, check that data has changed AND state is IDLE

    try {
      cookie = NoCore.login(copy(data), PASSWORD.toCharArray());
    } catch (Exception e) {
      logger.severe("Error thrown on first user, final login, should have returned cookie, was "
          + e.getClass().getSimpleName());
      printIf(e);
      return false;
    }

    try {
      state = NoCore.getSessionState(cookie);
    } catch (Exception e) {
      logger.severe("Error thrown on first user, final login, should have returned state, was "
          + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }
    if (state != NoState.IDLE) {
      logger.severe("Was expecting state to be IDLE, instead was " + state.toString());
      return false;
    }

    try {
      user = (NoUserTest) NoCore.getUser(cookie);
    } catch (Exception e) {
      logger.severe("Error thrown on first user, final login, should have returned user, was "
          + e.getClass().getSimpleName());
      printIf(e);
      NoCore.shred(cookie);
      return false;
    }

    if (user.getChangableString().equals(currentString)) {
      logger.severe("User information has not changed (still " + user.getChangableString() + ").");
      return false;
    }
    printIf("User string changed on final login (" + currentString + " to "
        + user.getChangableString() + "), while IDLE!");
    return true;
  }

  public static boolean testLifecycle(byte[] data) {
    printIf("Running life-cycle tests.");
    checkSetup();

    TestTicker ticker = new TestTicker();
    ticker.test(testLoginModifyLogout(data));
    if (passoverData != null && passoverData.getClass().equals(byte[].class)) {
      data = copy((byte[]) passoverData);
      passoverData = null;
    }
    ticker.test(testActionInfluenceLifecycle(data));
    if (passoverData != null && passoverData.getClass().equals(byte[].class)) {
      data = copy((byte[]) passoverData);
      passoverData = null;
    }

    ticker.logResultMessage();
    return ticker.passed();
  }

  /*
   * END Login-Logout methods
   */

  public static boolean testAll() {
    logger.info("Running all tests.");
    checkSetup();

    TestTicker ticker = new TestTicker();
    ticker.test(testRegistration());

    final byte[] data = registerAndGetBytes();
    ticker.test(testLogin(data));

    ticker.test(testLifecycle(data));

    ticker.logResultMessage();
    return ticker.passed();
  }

  public static void run() {
    if (testAll()) {
      logger.info("All tests passed.");
    }
  }

  public static void main(String[] args) {
    setSilence(false);
    setPrintStackTraces(true);
    if (!NoCore.isReady()) {
      NoCore.setup();
    }

    run();
  }
}
