package com.jvmer.mousehole.proxy;

/**
 * 守护线程
 * @version 1.00 
 * @since 2012-2-28
 * @author butnet
 */
public class DaemonThread extends Thread {
	protected ProxyServer proxyServer;
	public DaemonThread(ProxyServer proxyServer) {
		super("LocalProxyDaemonThread");
		this.proxyServer = proxyServer;
	}
	@Override
	public void run() {
		try {
			while(true){
				if(proxyServer.isConnectioned())
					Thread.sleep(2000);
				else{
					proxyServer.connection();
					Thread.sleep(1000);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
