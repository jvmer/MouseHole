package com.jvmer.mousehole.control;

import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.jvmer.mousehole.util.Constant;

/**
 * 控制端协议
 * @author butnet
 */
public class ControlProtocolHandler extends IoHandlerAdapter {
	protected static final Logger log = LogManager.getLogger(ControlProtocolHandler.class);
	protected ControlServer controlServer = null;
	protected final boolean logReceivesData;
	public ControlProtocolHandler(ControlServer controlServer, boolean logReceivesData) {
		this.controlServer = controlServer;
		this.logReceivesData = logReceivesData;
	}
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		log.info("[Control]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+" enter in Session:"+session.getId());
		
//		controlServer.isControlSession(session);//按照session中发来的注册信息设置是否是control session
			
		if(logReceivesData){
			FileOutputStream out = new FileOutputStream("receives/control/control_session"+session.getId()+"_"+System.currentTimeMillis()+".log");
			session.setAttribute(Constant.REQUEST_OUTPUT, out);
		}
	}
	
	@Override
	public void messageReceived(IoSession controlSession, Object message)
			throws Exception {
		String type = (String) controlSession.getAttribute(Constant.CONTROL_SESSION_TYPE);
		if(Constant.CMD_DATA.equals(type)){
			if(message instanceof byte[]){
				byte[] data = (byte[])message;
				
				if(logReceivesData){
					OutputStream out = (OutputStream) controlSession.getAttribute(Constant.REQUEST_OUTPUT);
					out.write(data);
					out.flush();
				}
				
				//将数据发送给发布服务器
				IoSession publishDataSession = (IoSession) controlSession.getAttribute(Constant.PUBLISH_DATA_SESSION);
				boolean setAttribute = publishDataSession==null;
				
				if(publishDataSession==null)
					publishDataSession = controlServer.getWaitPublishDataSession();
				
				while(publishDataSession==null && controlSession.isConnected() && !controlSession.isClosing()){
					Thread.sleep(100);
					publishDataSession = controlServer.getWaitPublishDataSession();
				}
				
				if(publishDataSession!=null){
					if(setAttribute){
						controlSession.setAttribute(Constant.PUBLISH_DATA_SESSION, publishDataSession);
						publishDataSession.setAttribute(Constant.CONTROL_SESSION, controlSession);
					}
					
					IoBuffer buf = IoBuffer.wrap(data);
					publishDataSession.write(buf);
				}
			}
		}else if(Constant.CMD_PROXY.equals(type)){
			if(message instanceof String[]){
				//TODO:处理消息
			}
		}
	}
	
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		super.messageSent(session, message);
		if(log.isDebugEnabled())
			log.debug("[Control]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+" session:"+session.getId()+", send message:"+message);
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		if(log.isInfoEnabled())
			log.info("[Control]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+" exit in Session:"+session.getId()+", connect time:"+(System.currentTimeMillis()-session.getCreationTime())+"ms");
		
		//从空闲的control data session列队中删除
		controlServer.removeControlSession(session);
		
		//关闭对应连接
		IoSession s = (IoSession) session.getAttribute(Constant.PUBLISH_DATA_SESSION);
		if(s!=null)s.close(true);
		
		if(logReceivesData){
			OutputStream out = (OutputStream) session.getAttribute(Constant.REQUEST_OUTPUT);
			out.close();
		}
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		if(log.isDebugEnabled())
			log.debug("[Control]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+" execption in Session:"+session.getId(), cause);
		else if(log.isInfoEnabled())
			log.info("[Control]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+" execption in Session:"+session.getId()+" ["+cause+"]");
		
		//从空闲的control data session列队中删除
		controlServer.removeControlSession(session);
		
		//关闭对应连接
		IoSession s = (IoSession) session.getAttribute(Constant.PUBLISH_DATA_SESSION);
		if(s!=null)s.close(true);
		
		if(logReceivesData){
			OutputStream out = (OutputStream) session.getAttribute(Constant.REQUEST_OUTPUT);
			out.close();
		}
	}
}
