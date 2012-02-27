package com.jvmer.mousehole.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class BytesTranslateDecoder extends ProtocolDecoderAdapter {
	protected final Logger logger = LogManager.getLogger(BytesTranslateDecoder.class);

	@Override
	public void decode(IoSession controlSession, IoBuffer buf, ProtocolDecoderOutput output)
			throws Exception {
		byte[] b = new byte[1024];
		int num = 0, start = 0;
		while(buf.hasRemaining() && start<b.length){
			num = Math.min(buf.limit()-buf.position(), b.length-start);
			buf.get(b, start, num);
			start += num;
			
			if(start==b.length){
				byte[] tmp, m = new byte[b.length];
				tmp = b;
				b = m;
				output.write(tmp);
				
				start = 0;
			}
		}
		
		if(start>0){
			byte[] m = new byte[start];
			System.arraycopy(b, 0, m, 0, m.length);
			output.write(m);
		}
	}
}
