package nodash.test.functional.implementations;

import nodash.core.NoAdapter;
import nodash.models.NoAction;

public class TestJustTouchStaticField extends NoAction {
  private static final long serialVersionUID = 1L;
  public static int touchMe = 0;

  @Override
  public void process() {}

  @Override
  public void execute(NoAdapter adapter) {
    TestJustTouchStaticField.touchMe++;
  }

  @Override
  public void purge() {}

}
