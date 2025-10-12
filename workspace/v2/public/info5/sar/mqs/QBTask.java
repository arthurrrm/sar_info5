package info5.sar.mqs;

import info5.sar.channels.Task;

public class QBTask extends Task {
  QueueBroker Qbroker;
  
  
  public QBTask(QueueBroker broker, String name) {
    super(name, broker.getBroker());
    this.Qbroker = broker; 
  }

  public QueueBroker getQueueBroker() {
    return Qbroker;
  }

}
