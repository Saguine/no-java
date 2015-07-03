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
 * NoSourcedAction is a subclassing of NoTargetedAction which additionally has a source, allowing
 * for errors to be returned if a NoCannotGetInfluenceException is thrown.
 * 
 * It is not advised to make a habit of using these for all user-user interactions as they provide a
 * clear link between two users.
 */

package nodash.models.noactiontypes;

import java.security.PublicKey;

import nodash.core.NoAdapter;
import nodash.exceptions.NoAdapterException;
import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.exceptions.NoDashFatalException;
import nodash.models.NoByteSet;
import nodash.models.NoInfluence;

public abstract class NoSourcedAction extends NoTargetedAction {
  private static final long serialVersionUID = -2996690472537380062L;
  protected PublicKey source;

  protected abstract NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException;

  public NoSourcedAction(PublicKey target, PublicKey source) {
    super(target);
    this.source = source;
  }

  @Override
  public void execute(NoAdapter adapter) {
    this.process();
    try {
      NoInfluence influence = this.generateTargetInfluence();
      if (influence != null) {
        NoByteSet byteSet = influence.getByteSet(this.target);
        adapter.addNoByteSet(byteSet, this.target);
      }
    } catch (NoCannotGetInfluenceException e) {
      NoInfluence errorInfluence = e.getResponseInfluence();
      if (errorInfluence != null) {
        NoByteSet byteSet = errorInfluence.getByteSet(this.source);
        try {
          adapter.addNoByteSet(byteSet, this.source);
        } catch (NoAdapterException e1) {
          throw new NoDashFatalException("Could not add error byte set to the pool.", e);
        }
      }
    } catch (NoAdapterException e) {
      throw new NoDashFatalException("Could not add byte set to the pool.", e);
    }
  }

  @Override
  public void purge() {
    super.purge();
    this.source = null;
  }
}
