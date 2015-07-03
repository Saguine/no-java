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
 * The NoRegister class is a simple model used to return both cookies and download data upon user
 * registration.
 */

package nodash.core;

public final class NoRegister {
  public final byte[] cookie;
  public final byte[] data;
  
  public NoRegister(byte[] cookie, byte[] data) {
    this.cookie = cookie;
    this.data = data;
  }
}
