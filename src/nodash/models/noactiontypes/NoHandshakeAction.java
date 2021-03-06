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
 * NoHandshakeAction is a subclass of the NoSourcedAction with logic to generate an applying
 * influence as well as a returned influence, whilst also being capable of returned an error
 * influence if the NoCannotGetInfluenceException is thrown.
 * 
 * As with all two-way or sourced actions, their use should be sparing as it establishes a
 * connection between two user addresses.
 */

package nodash.models.noactiontypes;

import java.security.PublicKey;

import nodash.core.NoAdapter;
import nodash.exceptions.NoAdapterException;
import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.exceptions.NoDashFatalException;
import nodash.models.NoByteSet;
import nodash.models.NoInfluence;

public abstract class NoHandshakeAction extends NoSourcedAction {
  private static final long serialVersionUID = 3195466136587475680L;

  protected abstract NoInfluence generateReturnedInfluence();

  public NoHandshakeAction(PublicKey target, PublicKey source) {
    super(target, source);
  }

  @Override
  public void execute(NoAdapter adapter) {
    this.process();
    try {
      NoInfluence influence = generateTargetInfluence();
      if (influence != null) {
        NoByteSet byteSet = influence.getByteSet(this.target);
        adapter.addNoByteSet(byteSet, this.target);
      }

      NoInfluence result = generateReturnedInfluence();
      if (result != null) {
        NoByteSet byteSet = result.getByteSet(this.source);
        adapter.addNoByteSet(byteSet, this.source);
      }
    } catch (NoCannotGetInfluenceException e) {
      NoInfluence errorInfluence = e.getResponseInfluence();
      if (errorInfluence != null) {
        NoByteSet byteSet = errorInfluence.getByteSet(this.source);
        try {
          adapter.addNoByteSet(byteSet, this.source);
        } catch (NoAdapterException e1) {
          throw new NoDashFatalException("Could not add error byte set to the pool.");
        }
      }
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not add byte sets to the pool.", e);
    }
  }
}
