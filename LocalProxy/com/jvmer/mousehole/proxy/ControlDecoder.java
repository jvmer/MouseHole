package com.jvmer.mousehole.proxy;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.jvmer.mousehole.util.CommandTools;
import com.jvmer.mousehole.util.Constant;

public class ControlDecoder extends ProtocolDecoderAdapter {
	protected final Logger logger = LogManager.getLogger(ControlDecoder.class);

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
			if(msgs.length==2 && Constant.CMD_OK.equals(msgs[0]) && Constant.CMD_REGEDIT.equals(msgs[1])){
				regedit = true;
				controlSession.setAttribute(Constant.CMD_REGEDIT, regedit);
				
				if(logger.isInfoEnabled())
					logger.info("[Proxy]"+controlSession.getLocalAddress()+"->"+controlSession.getRemoteAddress()+" regedit success in session:"+controlSession.getId());
			}else{
				return;
			}
		}
		
		Boolean isControlSession = (Boolean) controlSession.getAttribute(Constant.IS_CONTROL_SESSION);
		if(!isControlSession){
			byte[] buff = (byte[]) controlSession.getAttribute(Constant.BUFFER_OF_COMMAND_BYTES);
			if(buff==null){
				buff = new byte[1024];
				controlSession.setAttribute(Constant.BUFFER_OF_COMMAND_BYTES, buff);
			}
			int num = 0, lmt = 0, start = 0;
			
			while(buf.hasRemaining() && start<buff.length){
				lmt = buf.limit()-buf.position();
				num = Math.min(buff.length-start, lmt);
				buf.get(buff, start, num);
				start += num;
				if(start==buff.length){
					byte[] data = new byte[start];
					System.arraycopy(buff, 0, data, 0, start);
					output.write(data);
					start = 0;
				}
			}
			
			//如果接收到数据
			if(start>0){
				byte[] data = new byte[start];
				System.arraycopy(buff, 0, data, 0, data.length);
				output.write(data);
			}
		}else{
			//读取控制消息
			String[] msgs = null;
			while(buf.hasRemaining()){
				msgs = CommandTools.parseCommand(controlSession, buf);
				if(msgs!=null){
					output.write(msgs);
				}
			}
		}
	}
}
