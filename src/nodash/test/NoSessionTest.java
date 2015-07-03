package nodash.test;

import static org.junit.Assert.*;
import nodash.exceptions.NoSessionConfirmedException;
import nodash.exceptions.NoSessionExpiredException;
import nodash.models.NoSession;

import org.junit.Test;

public class NoSessionTest {

  @Test
  public void testNoSession() throws NoSessionConfirmedException, NoSessionExpiredException {
    NoSession session = new NoSession();
    assertNotNull(session.getUuid());
    assertNull(session.getNoUser());
    
  }

  @Test
  public void testNoSessionNoUser() {
    fail("Not yet implemented");
  }

  @Test
  public void testNoSessionByteArrayCharArray() {
    fail("Not yet implemented");
  }

  @Test
  public void testCheck() {
    fail("Not yet implemented");
  }

  @Test
  public void testTouchState() {
    fail("Not yet implemented");
  }

  @Test
  public void testInitiateSaveAttempt() {
    fail("Not yet implemented");
  }

  @Test
  public void testConfirmSave() {
    fail("Not yet implemented");
  }

  @Test
  public void testDecryptUuid() {
    fail("Not yet implemented");
  }

  @Test
  public void testConsume() {
    fail("Not yet implemented");
  }

  @Test
  public void testClose() {
    fail("Not yet implemented");
  }

}
