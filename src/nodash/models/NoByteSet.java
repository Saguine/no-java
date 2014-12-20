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
 * NoByteSet stores an AES key which has been RSA-4096 encrypted and a data 
 * stream which has been encrypted by this key.
 */

package nodash.models;

public final class NoByteSet {
	public byte[] key;
	public byte[] data;
	
	public NoByteSet(byte[] key, byte[] data) {
		this.key = key;
		this.data = data;
	}
	
}