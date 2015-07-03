/*
 * Copyright 2014 David Horscroft
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * 
 * NoDashException is the base class for no- related exceptions.
 */

package nodash.exceptions;

public class NoDashException extends Exception {
  private static final long serialVersionUID = -8579497499272656543L;

  public NoDashException() {
    super();
  }

  public NoDashException(Throwable e) {
    super(e);
  }
  
  public NoDashException(String message, Throwable e) {
    super(message, e);
  }
}
