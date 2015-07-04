package nodash.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({NoCoreTest.class, NoSessionTest.class, NoUserTest.class, NoUtilTest.class})
public class NoDashBasicTests {

}
