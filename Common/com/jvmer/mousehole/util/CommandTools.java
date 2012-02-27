package com.jvmer.mousehole.util;

import java.io.UnsupportedEncodingException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

/**
 * 控制协议命令工具
 * @author zhangbo
 */
public class CommandTools {
	/**
	 * 空字符
	 */
	public static final char SPACE = ' ';
	
	/**
	 * 将消息转换为命令消息
	 * @param msgs
	 * @return
	 */
	public static IoBuffer getCommand(String ... msgs) {
		StringBuilder sb = new StringBuilder();
		for(String msg:msgs){
			sb.append(msg).append(SPACE);
		}
		sb.delete(sb.length()-1, sb.length());
		
		IoBuffer buf = IoBuffer.allocate(sb.length()+4);
		buf.setAutoExpand(true);
		byte[] data;
		try {
			data = sb.toString().getBytes("utf-8");
			buf.putInt(data.length);
			buf.put(data);
			buf.flip();
			return buf;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**解析命令
	 * @param controlSession
	 * @param buf
	 * @return
	 */
	public static String[] parseCommand(IoSession controlSession, IoBuffer buf) {
		Integer commandCount = (Integer) controlSession.getAttribute(Constant.LENGTH_OF_COMMAND);
		Integer readCount = (Integer) controlSession.getAttribute(Constant.READ_COMMAND_LEN);
		byte[] sb = (byte[]) controlSession.getAttribute(Constant.BUFFER_OF_COMMAND_STRING);
		
		if(readCount==null)
			readCount = 0;
		
		if(commandCount==null){
			if(buf.hasRemaining() && buf.limit()>4){
				commandCount = buf.getInt();
			}else{
				return null;
			}
			
			sb = new byte[commandCount];
			
			//将已经读取到的数据保存到缓存中,下一次继续使用
			controlSession.setAttribute(Constant.BUFFER_OF_COMMAND_STRING, sb);
			controlSession.setAttribute(Constant.LENGTH_OF_COMMAND, commandCount);
		}
		
		//读取消息内容
		int num;
		while(buf.hasRemaining()){
			num = Math.min(sb.length-readCount, commandCount);
			buf.get(sb, readCount, num);
			readCount+=num;
		}
		
		//消息读取完成
		if(commandCount==readCount){
			String msgs[] = new String(sb).split(String.valueOf(SPACE));
			
			//删除之前保存的属性
			controlSession.removeAttribute(Constant.LENGTH_OF_COMMAND);
			controlSession.removeAttribute(Constant.BUFFER_OF_COMMAND_STRING);
			
			return msgs;
		}else{
			controlSession.setAttribute(Constant.READ_COMMAND_LEN, readCount);
		}
		
		return null;
	}

}
