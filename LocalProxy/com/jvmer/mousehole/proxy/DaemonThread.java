package com.jvmer.mousehole.proxy;

public class DaemonThread extends Thread {
	Object obj = new Object();
	public DaemonThread() {
		super("LocalProxyDaemonThread");
//		setDaemon(true);
	}
	@Override
	public void run() {
		try {
			while(true){
				Thread.sleep(2000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
