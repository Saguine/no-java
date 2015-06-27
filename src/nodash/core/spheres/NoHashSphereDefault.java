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
 * The NoHashSpehre stores database hashes for user verification.
 * 
 * Must be used with something that uses NoConfigDefault or an extension.
 */

package nodash.core.spheres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nodash.core.NoCore;
import nodash.core.NoConfigDefault;
import nodash.exceptions.NoDashFatalException;
import nodash.models.NoUser;

public final class NoHashSphereDefault implements NoHashSphereInterface {
  private Set<String> database = Collections
      .newSetFromMap(new ConcurrentHashMap<String, Boolean>());
  private String fileName;
  private boolean ready = false;

  public NoHashSphereDefault(String fileName) {
    this.fileName = fileName;
  }

  public NoHashSphereDefault() {
    this.fileName = ((NoConfigDefault) NoCore.config).getDatabaseName();
  }

  @SuppressWarnings("unchecked")
  public void setup() {
    if (NoCore.config.saveDatabase()) {
      File file = new File(this.fileName);
      if (file.exists()) {
        try {
          byte[] data = Files.readAllBytes(file.toPath());
          ByteArrayInputStream bais = new ByteArrayInputStream(data);
          ObjectInputStream ois = new ObjectInputStream(bais);
          this.database = (Set<String>) ois.readObject();
          ois.close();
          bais.close();
        } catch (IOException e) {
          throw new NoDashFatalException("Unable to load up given database file.", e);
        } catch (ClassNotFoundException e) {
          throw new NoDashFatalException("Database file not in a verifiable format.", e);
        }
      }
    }
    this.ready = true;
  }

  public synchronized void saveToFile() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(this.database);
    byte[] data = baos.toByteArray();
    oos.close();
    baos.close();
    File file = new File(((NoConfigDefault) NoCore.config).getDatabaseName());
    Files.write(file.toPath(), data, StandardOpenOption.CREATE);
  }

  public synchronized void addNewNoUser(NoUser user) throws IOException {
    String hash = user.createHashString();
    this.database.add(hash);
    this.saveToFile();
  }

  public synchronized void insertHash(String hash) throws IOException {
    this.database.add(hash);
    this.saveToFile();
  }

  public synchronized void removeHash(String hash) throws IOException {
    this.database.remove(hash);
    this.saveToFile();
  }

  public synchronized boolean checkHash(String hash) {
    return this.database.contains(hash);
  }

  public synchronized long size() {
    return this.database.size();
  }

  @Override
  public boolean isReady() {
    return this.ready;
  }
}
