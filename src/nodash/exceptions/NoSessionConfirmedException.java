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
 * NoSessionConfirmedException is triggered when an interaction attempt is made on a session that
 * has recently been confirmed and thus closed.
 */

package nodash.exceptions;

public class NoSessionConfirmedException extends NoDashException {
  private static final long serialVersionUID = -8065331145629402524L;
}
