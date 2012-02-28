package com.jvmer.mousehole.publish;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.jvmer.mousehole.control.ControlServer;
import com.jvmer.mousehole.util.ByteTranslateCodecFactory;
import com.jvmer.mousehole.util.Constant;

/**
 * 发布服务
 * @author butnet
 */
public class PublishServer {
	protected final Logger logger = LogManager.getLogger(PublishServer.class);
	protected String publishHost;
	protected int publishPort;
	protected ControlServer controlServer = null;
	protected long connectTimeOut = -1;
	protected NioSocketAcceptor acceptor = null;
	
	/**
	 * @param publicHost 发布服务地址(默认0.0.0.0)
	 * @param publicPort 发布服务端口
	 * @param controlAddress 控制端地址
	 */
	public PublishServer(String publicHost, int publicPort, ControlServer controlServer, long connectTimeOut){
		if(publicHost==null)
			publicHost = Constant.ANY_HOST;
		
		this.publishHost = publicHost;
		this.publishPort = publicPort;
		
		this.controlServer = controlServer;
		this.connectTimeOut = connectTimeOut;
	}
	public void start(){
		if(acceptor==null){
			acceptor = new NioSocketAcceptor();
			// 设置绑定套接字前启用 SO_REUSEADDR 可允许上一个连接处于超时状态时绑定套接字。
			acceptor.setReuseAddress(true);
			// 设置编解码
			acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ByteTranslateCodecFactory()));
			//设置发布服务处理方式
			acceptor.setHandler(new PublishServerIoHandler(controlServer, connectTimeOut, true));
			try {
				acceptor.bind(new InetSocketAddress(publishHost, publishPort));
				logger.info("发布服务启动成功 ["+publishHost+":"+publishPort+"]");
			} catch (IOException e) {
				logger.error("启动发布服务监听失败", e);
			}
		}
	}
	
	
	/**
	 * 停止发布服务
	 */
	public void stop(){
		if(acceptor!=null && acceptor.isActive()){
			acceptor.dispose(true);
			acceptor = null;
		}
	}
}
