/*
 * Copyright 2014 David Horscroft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * NoTargetedAction is an abstract subclassing of NoAction aimed at providing the 
 * basis for an action that has a user target in mind.
 */

package nodash.models.noactiontypes;

import java.security.PublicKey;

import nodash.core.NoCore;
import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.exceptions.NoDashFatalException;
import nodash.models.NoAction;
import nodash.models.NoByteSet;
import nodash.models.NoInfluence;

public abstract class NoTargetedAction extends NoAction {
	private static final long serialVersionUID = -8893381130155149646L;
	protected PublicKey target;
	
	protected abstract NoInfluence generateTargetInfluence() throws NoCannotGetInfluenceException;
	
	public NoTargetedAction(PublicKey target) {
		this.target = target;
	}
	
	public void execute() {
		this.process();
		try {
			NoInfluence influence = this.generateTargetInfluence();
			if (influence != null) {
				NoByteSet byteSet = influence.getByteSet(this.target);
				NoCore.addByteSet(byteSet, this.target);
			}
		} catch (NoCannotGetInfluenceException e) {
			if (e.getResponseInfluence() != null) {
				throw new NoDashFatalException("Unsourced action has generated an error with an undeliverable influence.");
			}
		}
	}
	
	public void purge() {
		this.target = null;
	}
}
