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
 * NoCannotGetInfluenceException is returned when an action is unable to render a successful
 * influence for the target, instead returning an influence to be returned to the sender if
 * possible.
 */

package nodash.exceptions;

import nodash.models.NoInfluence;

public class NoCannotGetInfluenceException extends Exception {
  private static final long serialVersionUID = 4581361079067540974L;

  private NoInfluence returnable;

  public NoCannotGetInfluenceException(NoInfluence returnable) {
    super();
    this.returnable = returnable;
  }

  public NoInfluence getResponseInfluence() {
    return returnable;
  }
}
