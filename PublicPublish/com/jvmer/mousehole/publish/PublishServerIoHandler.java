package com.jvmer.mousehole.publish;

import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.jvmer.mousehole.control.ControlServer;
import com.jvmer.mousehole.util.Constant;

/**
 * 发布服务处理
 * @author Administrator
 */
public class PublishServerIoHandler extends IoHandlerAdapter {
	protected final Logger logger = LogManager.getLogger(PublishServerIoHandler.class);
	protected ControlServer controlServer = null;
	protected long connectTimeOut = -1;
	protected IoConnector connector;
	protected final boolean logReceivesData;
	
	/**
	 * 发布服务处理
	 * @param address
	 * @param connectTimeOut
	 * @param logReceivesData
	 */
	public PublishServerIoHandler(ControlServer controlServer, long connectTimeOut, boolean logReceivesData) {
		this.controlServer = controlServer;
		this.connectTimeOut = connectTimeOut;
		this.logReceivesData = logReceivesData;
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		if(logReceivesData){
			OutputStream out = (OutputStream) session.getAttribute(Constant.REQUEST_OUTPUT);
			out.close();
		}
		
		//从空闲的publish data session中删除
		controlServer.removePublishDataSession(session);
		
		if(logger.isInfoEnabled())
			logger.info("[Publish]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+ " close session:"+session.getId()+", connect time:"+(System.currentTimeMillis()-session.getCreationTime())+"ms");
		IoSession controlSession = (IoSession) session.getAttribute(Constant.CONTROL_SESSION);
		if(controlSession!=null)
			controlSession.close(true);
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		if(logReceivesData){
			OutputStream out = (OutputStream) session.getAttribute(Constant.REQUEST_OUTPUT);
			out.close();
		}
		
		//从空闲的publish data session中删除
		controlServer.removePublishDataSession(session);
		
		if(logger.isInfoEnabled())
			logger.info("[Publish]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+ " exception in session:"+session.getId(), cause);
		
		IoSession controlSession = (IoSession) session.getAttribute(Constant.CONTROL_SESSION);
		if(controlSession!=null){
			controlSession.close(true);
		}
	}
	
	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		IoSession controlSession = (IoSession) session.getAttribute(Constant.CONTROL_SESSION);
		
		//等待控制服务器连接
		while(controlSession==null && !session.isClosing() && session.isConnected()){
			Thread.sleep(100);
			controlSession = (IoSession) session.getAttribute(Constant.CONTROL_SESSION);
		}
		
		//向控制服务发送真实数据
		if(controlSession!=null){
			byte[] data = (byte[]) message;
			IoBuffer buffer = IoBuffer.wrap(data);
			controlSession.write(buffer);
			
			if(logReceivesData){
				OutputStream out = (OutputStream) session.getAttribute(Constant.REQUEST_OUTPUT);
				out.write(data);
				out.flush();
			}
		}
	}

	@Override
	public void sessionOpened(final IoSession session) throws Exception {
		try{
			if(logReceivesData){
				FileOutputStream out = new FileOutputStream("receives/publish/publish_session"+session.getId()+"_"+System.currentTimeMillis()+".log");
				session.setAttribute(Constant.REQUEST_OUTPUT, out);
			}
			
			//添加到空闲的publish data session队列中
			controlServer.appendPublishDataSession(session);
			
			if(logger.isInfoEnabled())
				logger.info("[Publish]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+" enter in session:"+session.getId());
			
		}catch(Exception e){
			if(logger.isErrorEnabled())
				logger.error("连接控制服务器发生错误", e);
			
			session.close(true);//关闭当前连接
		}
	}
}
