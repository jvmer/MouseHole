package com.jvmer.mousehole.proxy;

import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.jvmer.mousehole.util.CommandTools;
import com.jvmer.mousehole.util.Constant;


/**
 * 代理服务器与控制服务器间数据处理
 * @author zhangbo
 */
public class ControlIoHandler extends IoHandlerAdapter {
	protected final Logger logger = LogManager.getLogger(ControlIoHandler.class);
	protected ProxyServer server = null;
	protected final boolean logReceivesData;
	
	public ControlIoHandler(ProxyServer server, boolean logReceivesData) {
		this.server = server;
		this.logReceivesData = logReceivesData;
	}
	
	@Override
	public void sessionCreated(IoSession controlSession) throws Exception {
		String fileName = null, cmdString = null;
		IoBuffer cmd = null;
		//判断是否是控制session, 如果是控制session则发送reg proxy, 如果是数据session则发送reg data
		if(!server.isControlSession(controlSession)){
			fileName = "receives/proxy/data_session";
			cmdString = Constant.CMD_DATA;
			//向控制服务器发送的注册代理服务器命令
			cmd = CommandTools.getCommand(Constant.CMD_REGEDIT, Constant.CMD_DATA);
			//向队列中添加data session
			server.appendControlDataSession(controlSession);
		}else{
			fileName = "receives/proxy/control_session";
			cmdString = Constant.CMD_PROXY;
			//向控制服务器发送的注册代理服务器命令
			cmd = CommandTools.getCommand(Constant.CMD_REGEDIT, Constant.CMD_PROXY);
		}
		
		//向控制服务器发送注册代理服务命令
		controlSession.write(cmd);
		
		//判断是否记录bin log
		if(logReceivesData){
			FileOutputStream out = new FileOutputStream(fileName+controlSession.getId()+"_"+System.currentTimeMillis()+".log");
			controlSession.setAttribute(Constant.REQUEST_OUTPUT, out);
		}
		
		if(logger.isInfoEnabled())
			logger.info("[Proxy]"+controlSession.getLocalAddress()+"->"+controlSession.getRemoteAddress()+" send control command: "+Constant.CMD_REGEDIT+CommandTools.SPACE+cmdString);
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		if(logger.isInfoEnabled())
			logger.info("[Proxy]"+session.getLocalAddress()+"->"+session.getRemoteAddress()+ " close session:"+session.getId()+", connect time:"+(System.currentTimeMillis()-session.getCreationTime())+"ms");
		
		try{
			//从空闲的control session队列中删除
			server.removeControlSession(session);
			
			Boolean isControlSession = (Boolean) session.getAttribute(Constant.IS_CONTROL_SESSION);
			if(isControlSession!=null){
				if(!isControlSession){
					IoSession localAppDataSession = (IoSession) session.getAttribute(Constant.LOCAL_APP_DATA_SESSION);
					if(localAppDataSession!=null)
						localAppDataSession.close(true);
				}else{
					//如果是控制连接断开,则尝试新的连接
					server.createControlConnection();
				}
			}
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
			//从空闲的control session队列中删除
			server.removeControlSession(session);
			
			Boolean isControlSession = (Boolean) session.getAttribute(Constant.IS_CONTROL_SESSION);
			if(isControlSession!=null){
				if(!isControlSession){
					IoSession localAppDataSession = (IoSession) session.getAttribute(Constant.LOCAL_APP_DATA_SESSION);
					if(localAppDataSession!=null)
						localAppDataSession.close(true);
				}else{
					//如果是控制连接断开,则尝试新的连接
					server.createControlConnection();
				}
			}
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
	public void messageReceived(IoSession controlSession, Object message)
			throws Exception {
		Boolean isControlSession = (Boolean) controlSession.getAttribute(Constant.IS_CONTROL_SESSION);
		if(!isControlSession){
			if(message instanceof byte[]){
				byte[] data = (byte[]) message;
				//取代理连接session
				IoSession localAppDataSession = (IoSession) controlSession.getAttribute(Constant.LOCAL_APP_DATA_SESSION);
				
				//如果不存在代理连接则,要求服务器创建新的连接
				if(localAppDataSession==null){
					//获取应用服务器session
					localAppDataSession = server.getWaitLocalAppDataSession();
					
					if(localAppDataSession==null){
						if(!server.createLocalAppDataConnection()){
							controlSession.close(true);
							return;
						}
					}
				}
				
				
				while(localAppDataSession==null && controlSession.isConnected() && !controlSession.isClosing()){
					Thread.sleep(100);
					localAppDataSession = server.getWaitLocalAppDataSession();
				}
				
				//发送数据
				if(localAppDataSession!=null) {
					localAppDataSession.setAttribute(Constant.CONTROL_SESSION, controlSession);
					controlSession.setAttribute(Constant.LOCAL_APP_DATA_SESSION, localAppDataSession);
					
					IoBuffer buf = IoBuffer.wrap(data);
					localAppDataSession.write(buf);
					
					if(logReceivesData){
						OutputStream out = (OutputStream) controlSession.getAttribute(Constant.REQUEST_OUTPUT);
						out.write(data);
						out.flush();
					}
				}
			}
		}else{
			if(message instanceof String[]){
				String[] msg = (String[])message;
				if(msg!=null && msg.length==2){
					if(Constant.CMD_CREATE.equals(msg[0]) && Constant.CMD_DATA.equals(msg[1])){
						//创建与control和localapp的连接
						IoBuffer cmd = null;
						if(server.createDataAndLocalConnections()){
							cmd = CommandTools.getCommand(Constant.CMD_OK, Constant.CMD_CREATE);
						}else{
							cmd = CommandTools.getCommand(Constant.CMD_ERROR, Constant.CMD_CREATE);
						}
						//返回响应
						controlSession.write(cmd);
					}
				}
			}
		}
	}
}