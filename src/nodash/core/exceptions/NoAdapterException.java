package nodash.core.exceptions;

import nodash.exceptions.NoDashException;

public class NoAdapterException extends NoDashException {
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public NoAdapterException(String message, Throwable e) {
    super(message, e);
  }
}
