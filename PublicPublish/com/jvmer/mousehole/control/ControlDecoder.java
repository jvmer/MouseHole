package com.jvmer.mousehole.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.jvmer.mousehole.util.CommandTools;
import com.jvmer.mousehole.util.Constant;

/**
 * 控制服务器与发布服务,代理服务间解码
 * @author zhangbo
 */
public class ControlDecoder extends ProtocolDecoderAdapter {
	protected final Logger logger = LogManager.getLogger(ControlDecoder.class);
	protected ControlServer controlServer = null;
	
	/**
	 * 控制服务器与发布服务,代理服务间解码
	 * @param controlServer
	 */
	public ControlDecoder(ControlServer controlServer) {
		this.controlServer = controlServer;
	}
	
	@Override
	public void decode(IoSession controlSession, IoBuffer buf, ProtocolDecoderOutput output)
			throws Exception {
		
		//是否在控制端注册
		Boolean regedit = (Boolean) controlSession.getAttribute(Constant.CMD_REGEDIT);
		
		//未注册
		if(regedit==null || !regedit){
			String[] msgs = CommandTools.parseCommand(controlSession, buf);
			if(msgs==null || msgs.length==0){
				return;
			}
			
			//注册发布服务成功
			if(msgs.length==2 && Constant.CMD_REGEDIT.equals(msgs[0])){
				regedit = true;
				//返回注册成功消息
				IoBuffer cmd = CommandTools.getCommand(Constant.CMD_OK, Constant.CMD_REGEDIT);
				controlSession.write(cmd);
				
				if(logger.isInfoEnabled())
					logger.info("[Control]"+controlSession.getLocalAddress()+"->"+controlSession.getRemoteAddress()+" send control command: "+Constant.CMD_OK+" "+Constant.CMD_REGEDIT+", session:"+controlSession.getId());
				
				//设置连接端
				if(Constant.CMD_DATA.equals(msgs[1])){
					controlSession.setAttribute(Constant.CONTROL_SESSION_TYPE, Constant.CMD_DATA);//数据连接
					controlServer.appendControlDataSession(controlSession);//添加到空闲的control data session队列中
				}else if(Constant.CMD_PROXY.equals(msgs[1])){
					controlSession.setAttribute(Constant.CONTROL_SESSION_TYPE, Constant.CMD_PROXY);//代理连接
					controlServer.setControlSession(controlSession);
				}
				
				//返回注册成功消息
				controlSession.setAttribute(Constant.CMD_REGEDIT, regedit);
				if(logger.isInfoEnabled())
					logger.info("[Control]"+controlSession.getLocalAddress()+"->"+controlSession.getRemoteAddress()+" regedit success, session:"+controlSession.getId());
			}else{
				return;
			}
		}
		
		String type = (String) controlSession.getAttribute(Constant.CONTROL_SESSION_TYPE);
		if(Constant.CMD_DATA.equals(type)){
			byte[] buff = (byte[]) controlSession.getAttribute(Constant.BUFFER_OF_COMMAND_BYTES);
			if(buff==null){
				buff = new byte[1024];
				controlSession.setAttribute(Constant.BUFFER_OF_COMMAND_BYTES, buff);
			}
			int num = 0, lmt = 0, start = 0;
			
			while(buf.hasRemaining() && num<buff.length){
				lmt = buf.limit()-buf.position();
				num = Math.min(buff.length-start, lmt);
				buf.get(buff, start, num);
				start += num;
			}
			
			//如果接收到数据
			if(start>0){
				byte[] data = new byte[start];
				System.arraycopy(buff, 0, data, 0, data.length);
				output.write(data);
			}
		}else{
			String[] msgs = CommandTools.parseCommand(controlSession, buf);
			if(msgs==null || msgs.length==0){
				return;
			}
			output.write(msgs);
		}
	}
}
