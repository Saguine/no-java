package nodash.test;

import nodash.test.functional.NoRoutineTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({NoDashBasicTests.class, NoRoutineTest.class})
public class NoSystemTest {

}
