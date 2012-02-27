package com.jvmer.mousehole.proxy;

import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.jvmer.mousehole.util.Constant;


/**
 * 代理服务器与应用服务器间数据处理
 * @author zhangbo
 */
public class ProxyApplicationIoHandler extends IoHandlerAdapter {
	protected final Logger logger = LogManager.getLogger(ProxyApplicationIoHandler.class);
	protected ProxyServer server = null;
	protected final boolean logReceivesData;
	
	public ProxyApplicationIoHandler(ProxyServer server, boolean logReceivesData) {
		this.server = server;
		this.logReceivesData = logReceivesData;
	}
	
	@Override
	public void sessionCreated(IoSession localAppDataSession) throws Exception {
		if(logReceivesData){
			FileOutputStream out = new FileOutputStream("receives/proxy/application_session"+localAppDataSession.getId()+"_"+System.currentTimeMillis()+".log");
			localAppDataSession.setAttribute(Constant.REQUEST_OUTPUT, out);
		}
		
		//向服务器添加空闲的本地服务数据session
		server.appendLocalAppDataSession(localAppDataSession);
		
		if(logger.isInfoEnabled())
			logger.info("[Proxy]"+localAppDataSession.getLocalAddress()+"->"+localAppDataSession.getRemoteAddress()+" enter session:"+localAppDataSession.getId());
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		if(logger.isInfoEnabled())
			logger.info("[Proxy]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+ " close session:"+session.getId()+", connect time:"+(System.currentTimeMillis()-session.getCreationTime())+"ms");
		
		try{
			//删除空闲的app data session
			server.removeAppDataSession(session);
			
			IoSession controlSession = (IoSession) session.getAttribute(Constant.CONTROL_SESSION);
			if(controlSession!=null && !controlSession.isClosing() && controlSession.isConnected())
				controlSession.close(true);
		}finally{
			if(logReceivesData){
				OutputStream out = (OutputStream) session.getAttribute(Constant.REQUEST_OUTPUT);
				if(out!=null){
					out.close();
					session.removeAttribute(Constant.REQUEST_OUTPUT);
				}
			}
		}
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		if(logger.isInfoEnabled())
			logger.info("[Proxy]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+ " exception in session:"+session.getId(), cause);
		
		try{
			//删除空闲的app data session
			server.removeAppDataSession(session);
			
			IoSession controlSession = (IoSession) session.getAttribute(Constant.CONTROL_SESSION);
			if(controlSession!=null && !controlSession.isClosing() && controlSession.isConnected())
				controlSession.close(true);
		}finally{
			if(logReceivesData){
				OutputStream out = (OutputStream) session.getAttribute(Constant.REQUEST_OUTPUT);
				if(out!=null){
					out.close();
					session.removeAttribute(Constant.REQUEST_OUTPUT);
				}
			}
		}
	}
	
	@Override
	public void messageReceived(IoSession proxySession, Object message)
			throws Exception {
		//控制服务连接session
		IoSession controlSession = (IoSession) proxySession.getAttribute(Constant.CONTROL_SESSION);
		while(controlSession==null && proxySession.isConnected() && !proxySession.isClosing()){
			Thread.sleep(100);
			controlSession = (IoSession) proxySession.getAttribute(Constant.CONTROL_SESSION);
		}
		
		//发送数据
		if(controlSession!=null) {
			byte[] data = (byte[]) message;
			
			IoBuffer buf = IoBuffer.wrap(data);
			controlSession.write(buf);
			
			if(logReceivesData){
				OutputStream out = (OutputStream) proxySession.getAttribute(Constant.REQUEST_OUTPUT);
				out.write(data);
				out.flush();
			}
		}
	}
}