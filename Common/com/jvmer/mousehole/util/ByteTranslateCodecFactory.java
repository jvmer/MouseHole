package com.jvmer.mousehole.util;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import com.jvmer.mousehole.util.Constant;

public class ByteTranslateCodecFactory implements ProtocolCodecFactory {

	@Override
	public ProtocolDecoder getDecoder(IoSession session) throws Exception {
		ProtocolDecoder decoder = (ProtocolDecoder) session.getAttribute(Constant.DECODER);
		if(decoder==null){
			decoder = new BytesTranslateDecoder();
			session.setAttribute(Constant.DECODER, decoder);
		}
		return decoder;
	}

	@Override
	public ProtocolEncoder getEncoder(IoSession session) throws Exception {
		ProtocolEncoder encoder = (ProtocolEncoder) session.getAttribute(Constant.ENCODER);
		if(encoder==null){
			encoder = new ByteTranslateEncoder();
			session.setAttribute(Constant.ENCODER, encoder);
		}
		return encoder;
	}

}
