package com.jvmer.mousehole.control;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.jvmer.mousehole.proxy.ProxyServer;
import com.jvmer.mousehole.util.CommandTools;
import com.jvmer.mousehole.util.Constant;

/**
 * 控制服务(向代理服务发送命令,要求代理服务器建立新的连接)
 * @author butnet
 */
public class ControlServer {
	/**
	 * 日志
	 */
	protected final Logger logger = LogManager.getLogger(ProxyServer.class);
	/**
	 * 控制服务地址
	 */
	protected String controlHost;
	/**
	 * 控制服务端口
	 */
	protected int controlPort;
	/**
	 * 控制服务绑定地址
	 */
	protected SocketAddress address = null;
	/**
	 * 空闭的control data session队列
	 */
	protected ConcurrentLinkedQueue<IoSession> controlDataSessions = new ConcurrentLinkedQueue<IoSession>();
	/**
	 * 空闭的publish data session队列
	 */
	protected ConcurrentLinkedQueue<IoSession> publishDataSessions = new ConcurrentLinkedQueue<IoSession>();
	/**
	 * socket监听
	 */
	protected NioSocketAcceptor acceptor = null;
	/**
	 * 检查是否是control session的锁
	 */
	protected ReentrantLock controlSessionCheckLock = new ReentrantLock();
	/**
	 * 控制session
	 */
	protected IoSession controlSession;
	
	/**
	 * @param controlHost 控制端内网地址(默认0.0.0.0)
	 * @param controlPort 控制端内网端口
	 */
	public ControlServer(String controlHost, int controlPort){
		if(controlHost==null)
			controlHost = Constant.ANY_HOST;
		
		this.controlHost = controlHost;
		this.controlPort = controlPort;
	}
	
	/**
	 * 启动控制服务
	 */
	public void start(){
		if(acceptor==null){
			acceptor = new NioSocketAcceptor();
			// 设置绑定套接字前启用 SO_REUSEADDR 可允许上一个连接处于超时状态时绑定套接字。
			acceptor.setReuseAddress(true);
			// 添加编解码器
			acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ControlCodecFactory(this)));
			// 设置消息处理器
			acceptor.setHandler(new ControlProtocolHandler(this, true));
			try {
				address = new InetSocketAddress(controlHost, controlPort);
				//绑定端口
				acceptor.bind(address);
				logger.info("启动控制服务成功 ["+controlHost+":"+controlPort+"]");
			} catch (IOException e) {
				logger.error("启动控制服务监听失败", e);
			}
		}
	}
	
	/**
	 * @param session
	 * @return
	 */
	public boolean isControlSession(IoSession session) {
		//检查是否已经设置过
		Boolean value = (Boolean) session.getAttribute(Constant.IS_CONTROL_SESSION);
		if(value!=null)
			return value;
		
		//未设置时检查当前的controlSession
		if(this.controlSession!=null && !this.controlSession.isClosing() && this.controlSession.isConnected()){
			value = this.controlSession.equals(session);
			session.setAttribute(Constant.IS_CONTROL_SESSION, value);
			return value;
		}
		
		//当前controlSession为空或连接断开时,使用新的连接做为control session
		controlSessionCheckLock.lock();
		try{
			if(this.controlSession!=null && !this.controlSession.isClosing() && this.controlSession.isConnected()){
				value = this.controlSession.equals(session);
				session.setAttribute(Constant.IS_CONTROL_SESSION, value);
				return value;
			}
			
			IoSession old = this.controlSession;
			this.controlSession = session;
			
			session.setAttribute(Constant.IS_CONTROL_SESSION, true);
			
			if(old!=null){
				old.close(true);
			}
			return true;
		}finally{
			controlSessionCheckLock.unlock();
		}
	}
	
	/**
	 * 返回监听地址
	 * @return
	 */
	public SocketAddress getAddress() {
		return address;
	}
	
	/**
	 * 停止控制服务
	 */
	public void stop(){
		if(acceptor!=null && acceptor.isActive()){
			acceptor.dispose(true);
			acceptor = null;
		}
	}
	
//	/**
//	 * 取得空闲的代理服务器session
//	 * @param publishSession 
//	 * @return
//	 */
//	public IoSession getProxySession(IoSession publishSession){
//		publishSessions.remove(publishSession);
//		return proxySessions.poll();
//	}
//	
//	/**
//	 * 取得空闭的发布服务session
//	 * @param proxySession
//	 * @return
//	 */
//	public IoSession getPublishSession(IoSession proxySession){
//		proxySessions.remove(proxySession);
//		return publishSessions.poll();
//	}
//
	public void appendControlDataSession(IoSession controlDataSession) {
		//TODO:判断是否存在空闲的publish data session, 如果有则进行传输, 没有则添加到空闲的control data session队列中
		IoSession publishDataSession = publishDataSessions.poll();
		if(publishDataSession!=null){
			publishDataSession.setAttribute(Constant.CONTROL_SESSION, controlDataSession);
			controlDataSession.setAttribute(Constant.PUBLISH_DATA_SESSION, publishDataSession);
			return;
		}
		controlDataSessions.add(controlDataSession);
	}
//
//	public void removeSession(IoSession session) {
//		String type = (String) session.getAttribute(Constant.CONTROL_SESSION_TYPE);
//		if(Constant.CMD_PROXY.equals(type)){
//			proxySessions.remove(session);
//		}else if(Constant.CMD_DATA.equals(type)){
//			publishSessions.remove(session);
//		}
//	}

	/**
	 * 设置当前的control session
	 * @param controlSession
	 */
	public void setControlSession(IoSession controlSession) {
		IoSession old = this.controlSession;
		this.controlSession = controlSession;
		if(old!=null)
			old.close(true);
	}

	/**
	 * 获取空闲的publish data session
	 * @return
	 */
	public IoSession getWaitPublishDataSession() {
		return publishDataSessions.poll();
	}

	/**
	 * 从空闲的control data session列队中删除
	 * @param session
	 */
	public void removeControlSession(IoSession session) {
		controlDataSessions.remove(session);
	}

	/**
	 * 添加到空闲的publish data session队列中
	 * @param session
	 */
	public void appendPublishDataSession(IoSession session) {
		//没有空闭的连接, 发送建立连接命令给control session
		if(controlDataSessions.size()==0){
			IoBuffer buff = CommandTools.getCommand(Constant.CMD_CREATE, Constant.CMD_DATA);
			if(controlSession!=null && controlSession.isConnected() && !controlSession.isClosing()){
				controlSession.write(buff);
			}else{
				//发出告警信息,没有可用的control session,不能创建新的代理连接
				if(logger.isErrorEnabled())
					logger.error("无可用control session");
				IoBuffer buf = CommandTools.getCommand(Constant.CMD_ERROR, Constant.CONTROL_SESSION);
				session.write(buf);
				session.close(true);
				return;
			}
		}
		publishDataSessions.add(session);
	}

	/**
	 * 从空闲的publish data session中删除
	 * @param session
	 */
	public void removePublishDataSession(IoSession session) {
		publishDataSessions.remove(session);
	}
}
