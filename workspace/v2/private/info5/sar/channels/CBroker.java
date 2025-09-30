/*
 * Copyright (C) 2023 Pr. Olivier Gruber                                    
 *                                                                       
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU General Public License as published by  
 * the Free Software Foundation, either version 3 of the License, or     
 * (at your option) any later version.                                   
 *                                                                       
 * This program is distributed in the hope that it will be useful,       
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
 * GNU General Public License for more details.                          
 *                                                                       
 * You should have received a copy of the GNU General Public License     
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package info5.sar.channels;

import java.util.concurrent.*;

import info5.sar.utils.CircularBuffer;

public class CBroker extends Broker {
  private static final ConcurrentHashMap<String, CBroker> registry = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, SynchronousQueue<CChannel>> rdvs = new ConcurrentHashMap<>();

  public CBroker(String name) {
    super(name);
    CBroker key = registry.put(name, this);
    if (key != null) {
      throw new IllegalArgumentException("Broker name must be unique: " + name);
    }
  }

  @Override
  public Channel accept(int port) {
    SynchronousQueue<CChannel> q = rdvs.computeIfAbsent(port, p -> new SynchronousQueue<>());
    try {
      return q.take(); // bloque jusqu'à ce qu'un connect donne un channel
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  @Override
  public Channel connect(String remoteName, int port) {
    CBroker remote = registry.get(remoteName);
    if (remote == null) {
      return null; // spec : broker inexistant => null
    }
    SynchronousQueue<CChannel> q = remote.rdvs.computeIfAbsent(port, p -> new SynchronousQueue<>());

    // créer les buffers partagés
    CircularBuffer bufAB = new CircularBuffer(4096);
    CircularBuffer bufBA = new CircularBuffer(4096);

    // créer deux channels liés
    CChannel local = new CChannel(this, remoteName, bufAB, bufBA);
    CChannel remoteChannel = new CChannel(remote, this.getName(), bufBA, bufAB);
    local.setRemote(remoteChannel);
    remoteChannel.setRemote(local);

    try {
      if (!q.offer(remoteChannel, 5, TimeUnit.SECONDS)) {
        return null; // timeout simplifié
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
    return local;
  }
}
