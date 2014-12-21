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
 * NoErrorableAction is a subclassing of NoTargetedAction for user-server
 * interactions that require an error to be returned if a 
 * NoCannotGetInfluenceException is thrown. The target is not a destination user,
 * but is instead treated as the source.
 * 
 * As with all sourced actions it is advised to avoid their use unless the logic 
 * demands it, as it establishes a connection between a server action and a user address.
 */

package nodash.models.noactiontypes;

import java.security.PublicKey;

import nodash.core.NoCore;
import nodash.exceptions.NoCannotGetInfluenceException;
import nodash.models.NoByteSet;
import nodash.models.NoInfluence;

public abstract class NoErrorableAction extends NoTargetedAction {
	private static final long serialVersionUID = -6077150774349400823L;

	public NoErrorableAction(PublicKey source) {
		// Note that 
		super(source);
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
			NoInfluence errorInfluence = e.getResponseInfluence();
			if (errorInfluence != null) {
				NoByteSet byteSet = errorInfluence.getByteSet(this.target);
				NoCore.addByteSet(byteSet, this.target);
			}
		}
	}
}
