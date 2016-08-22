package ltsa.lts.gui;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import ltsa.lts.automata.automaton.event.LTSEvent;

public class EventManager implements Runnable {
	Hashtable<EventClient, EventClient> clients = new Hashtable<>();
	Vector<LTSEvent> queue = new Vector<>(); // queued messages
	Thread athread;
	boolean stopped = false;

	public EventManager() {
		athread = new Thread(this);
		athread.start();
	}

	public synchronized void addClient(EventClient c) {
		clients.put(c, c);
	}

	public synchronized void removeClient(EventClient c) {
		clients.remove(c);
	}

	public synchronized void post(LTSEvent le) {
		queue.addElement(le);
		notifyAll();
	}

	public void stop() {
		stopped = true;
	}

	private synchronized void dopost() {
		while (queue.isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		LTSEvent le =  queue.firstElement();
		Enumeration<EventClient> e = clients.keys();
		while (e.hasMoreElements()) {
			EventClient c = (EventClient) e.nextElement();
			c.ltsAction(le);
		}
		queue.removeElement(le);
	}

	@Override
	public void run() {
		while (!stopped) {
			dopost();
		}
	}
}