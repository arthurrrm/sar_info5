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

import info5.sar.utils.CircularBuffer;

public class CChannel extends Channel {
  private final String remoteName;
  private final CircularBuffer writeBuffer; // buffer dans lequel on écrit localement
  private final CircularBuffer readBuffer; // buffer dont on lit localement
  private volatile boolean localDisconnected = false;
  private volatile boolean remoteDisconnected = false;
  private CChannel remote;

  protected CChannel(Broker broker, String remoteName,
      CircularBuffer writeBuffer, CircularBuffer readBuffer) {
    super(broker);
    this.remoteName = remoteName;
    this.writeBuffer = writeBuffer;
    this.readBuffer = readBuffer;
  }

  public void setRemote(CChannel other) {
    this.remote = other;
  }

  @Override
  public String getRemoteName() {
    return remoteName;
  }

  @Override
  public int read(byte[] bytes, int offset, int length) {
    if (localDisconnected)
      throw new IllegalStateException("Channel disconnected");

    int n = 0;
    synchronized (readBuffer) {
      while (n == 0 && !remoteDisconnected && readBuffer.empty()) {
        try {
          readBuffer.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return n;
        }
      }
      while (n < length && !readBuffer.empty()) {
        bytes[offset + n] = readBuffer.pull();
        n++;
      }
    }
    if (n == 0 && disconnected())
      throw new IllegalStateException("Channel disconnected");
    return n;
  }

  @Override
  public int write(byte[] bytes, int offset, int length) {
    if (localDisconnected)
      throw new IllegalStateException("Channel disconnected");

    int n = 0;
    synchronized (writeBuffer) {
      for (int i = 0; i < length; i++) {
        while (writeBuffer.full() && !remoteDisconnected) {
          try {
            writeBuffer.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return n;
          }
        }
        if (remoteDisconnected)
          break;
        writeBuffer.push(bytes[offset + i]);
        n++;
        writeBuffer.notifyAll();
      }
    }
    return n;
  }

  @Override
  public void disconnect() {
    if (!localDisconnected) {
      localDisconnected = true;
      if (remote != null) {
        remote.remoteDisconnected = true;
        synchronized (remote.readBuffer) {
          remote.readBuffer.notifyAll();
        }
        synchronized (remote.writeBuffer) {
          remote.writeBuffer.notifyAll();
        }
      }
    }
  }

  @Override
  public boolean disconnected() {
    return localDisconnected || remoteDisconnected;
  }
}