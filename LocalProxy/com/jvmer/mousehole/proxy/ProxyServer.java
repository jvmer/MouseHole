package com.jvmer.mousehole.proxy;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.jvmer.mousehole.util.ByteTranslateCodecFactory;
import com.jvmer.mousehole.util.Constant;

/**
 * 代理服务
 * 
 * @author butnet
 */
public class ProxyServer {
	/**
	 * 日志
	 */
	protected final Logger logger = LogManager.getLogger(ProxyServer.class);
	/**
	 * 本地目标服务地址
	 */
	protected String localAppDestHost;
	/**
	 * 本地目标服务端口
	 */
	protected int localAppDestPort;
	/**
	 * 控制服务地址
	 */
	protected String controlHost;
	/**
	 * 控制服务端口
	 */
	protected int controlPort;
	/**
	 * 控制服务连接地址
	 */
	protected SocketAddress controlServerAddress = null;
	/**
	 * 本地目标服务地址
	 */
	protected SocketAddress localAppServerAddress = null;
	/**
	 * 控制服务连接
	 */
	protected NioSocketConnector controlConnector = null;
	/**
	 * 本地目标服务连接
	 */
	protected NioSocketConnector localAppServerConnector = null;
	/**
	 * 判断控制session的锁
	 */
	protected ReentrantLock controlSessionCheckLock = new ReentrantLock();
	/**
	 * 控制session
	 */
	protected IoSession controlSession = null;
	/**
	 * 空闭的与control连接的data session
	 */
	protected ConcurrentLinkedQueue<IoSession> controlDataSessions = new ConcurrentLinkedQueue<IoSession>();
	/**
	 * 空闭的与local app连接的local app data session
	 */
	protected ConcurrentLinkedQueue<IoSession> localAppDataSessions = new ConcurrentLinkedQueue<IoSession>();
	/**
	 * 连接超时时间
	 */
	protected long connectTimeOut = -1;
	/**
	 * 服务名字
	 */
	protected final String name;
	
	public ProxyServer(String localAppDestHost, int localAppDestPort, String controlHost, int controlPort, long connectTimeOut) {
		name = "代理[local="+localAppDestHost+":"+localAppDestPort+" >> remote="+controlHost+":"+controlPort+"]";
		
		this.localAppDestHost = localAppDestHost;
		this.localAppDestPort = localAppDestPort;
		this.controlHost = controlHost;
		this.controlPort = controlPort;
		this.connectTimeOut = connectTimeOut;
	}
	
	public void start() {
		//本地应用服务连接
		localAppServerConnector = new NioSocketConnector();
		//连接本地应用服务地址
		localAppServerAddress = new InetSocketAddress(localAppDestHost, localAppDestPort);
		// 添加编解码器
		localAppServerConnector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ByteTranslateCodecFactory()));
		localAppServerConnector.setHandler(new ProxyApplicationIoHandler(this, true));
		
		if(connectTimeOut>-1)
			localAppServerConnector.setConnectTimeoutMillis(connectTimeOut);
		
		//控制服务连接
		controlConnector = new NioSocketConnector();
		//添加编解码器
		controlConnector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ControlCodecFactory()));
		controlConnector.setHandler(new ControlIoHandler(this, true));
		
		//连接控制服务器
		controlServerAddress = new InetSocketAddress(controlHost, controlPort);
		
		if(connectTimeOut>-1)
			controlConnector.setConnectTimeoutMillis(connectTimeOut);

		//启用成功
		if(logger.isInfoEnabled())
			logger.info("启动"+name+"中...");
				
		ConnectFuture future = controlConnector.connect(controlServerAddress);
		
		try {
			future.await();
		} catch (InterruptedException e) {
			//启用成功
			if(logger.isDebugEnabled())
				logger.debug("启动"+name+"发生异常", e);
		}
		
		//启用成功
		if(logger.isInfoEnabled())
			logger.info("启动"+name+(future.isConnected()?"成功":"失败"));
	}
	/**
	 * 获取控制服务器地址
	 * @return
	 */
	public SocketAddress getControlServerAddress() {
		return controlServerAddress;
	}

	/**
	 * 获取本代理服务名称
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param proxySession
	 */
	public void appendLocalAppDataSession(IoSession proxySession) {
		IoSession controlDataSession = controlDataSessions.poll();
		if(controlDataSession!=null){
			controlDataSession.setAttribute(Constant.LOCAL_APP_DATA_SESSION, proxySession);
			proxySession.setAttribute(Constant.CONTROL_SESSION, controlDataSession);
		}else{
			localAppDataSessions.add(proxySession);
		}
	}

	/**
	 * @param controlSession
	 * @return
	 */
	public boolean isControlSession(IoSession controlSession) {
		//检查是否已经设置过
		Boolean value = (Boolean) controlSession.getAttribute(Constant.IS_CONTROL_SESSION);
		if(value!=null)
			return value;
		
		//未设置时检查当前的controlSession
		if(this.controlSession!=null && !this.controlSession.isClosing() && this.controlSession.isConnected()){
			value = this.controlSession.equals(controlSession);
			controlSession.setAttribute(Constant.IS_CONTROL_SESSION, value);
			return value;
		}
		
		//当前controlSession为空或连接断开时,使用新的连接做为control session
		controlSessionCheckLock.lock();
		try{
			if(this.controlSession!=null && !this.controlSession.isClosing() && this.controlSession.isConnected()){
				value = this.controlSession.equals(controlSession);
				controlSession.setAttribute(Constant.IS_CONTROL_SESSION, value);
				return value;
			}
			
			IoSession old = this.controlSession;
			this.controlSession = controlSession;
			
			controlSession.setAttribute(Constant.IS_CONTROL_SESSION, true);
			
			if(old!=null){
				old.close(true);
			}
			return true;
		}finally{
			controlSessionCheckLock.unlock();
		}
	}

	/**
	 * 创建与control和localapp的连接
	 * @return
	 */
	public boolean createDataAndLocalConnections() {
		try{
			controlConnector.connect(controlServerAddress);
		}catch(Exception e){
			if(logger.isErrorEnabled())
				logger.error("[Proxy]connect "+localAppServerAddress+" error:", e);
			return false;
		}
		
		//检查是否存在空闭的local app data session
		if(localAppDataSessions.size()>0)
			return true;
		
		try{
			localAppServerConnector.connect(localAppServerAddress);
		}catch(Exception e){
			if(logger.isErrorEnabled())
				logger.error("[Proxy]connect "+localAppServerAddress+" error:", e);
			return false;
		}
		return true;
	}

	/**
	 * 创建与控制服务器间的连接
	 */
	public boolean createControlConnection() {
		try{
			controlConnector.connect(controlServerAddress);
		}catch(Exception e){
			if(logger.isErrorEnabled())
				logger.error("[Proxy]connect "+localAppServerAddress+" error:", e);
			return false;
		}
		return true;
	}

	/**
	 * 创建与本地服务间的连接
	 * @return
	 */
	public boolean createLocalAppDataConnection() {
		//检查是否存在空闭的local app data session
		if(localAppDataSessions.size()>0)
			return true;
		
		try{
			localAppServerConnector.connect(localAppServerAddress);
		}catch(Exception e){
			if(logger.isErrorEnabled())
				logger.error("[Proxy]connect "+localAppServerAddress+" error:", e);
			return false;
		}
		return true;
	}

	/**
	 * 获取空间的本地服务连接
	 * @return
	 */
	public IoSession getWaitLocalAppDataSession() {
		return localAppDataSessions.poll();
	}

	/**
	 * 添加与control连接的data session
	 * @param controlSession
	 */
	public void appendControlDataSession(IoSession controlSession) {
		controlDataSessions.add(controlSession);
	}

	/**
	 * 从空闲的control session队列中删除
	 * @param session
	 */
	public void removeControlSession(IoSession session) {
		controlDataSessions.remove(session);
	}

	/**
	 * 删除空闲的app data session
	 * @param session
	 */
	public void removeAppDataSession(IoSession session) {
		localAppDataSessions.remove(session);
	}
}
