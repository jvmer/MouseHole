package com.jvmer.mousehole.util;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class ByteTranslateEncoder extends ProtocolEncoderAdapter {
	@Override
	public void encode(IoSession session, Object msg, ProtocolEncoderOutput output)
			throws Exception {
		output.write(msg);
		output.flush();
	}
}
