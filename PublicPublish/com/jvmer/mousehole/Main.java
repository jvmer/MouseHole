package com.jvmer.mousehole;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.jvmer.mousehole.control.ControlServer;
import com.jvmer.mousehole.publish.PublishServer;
import com.jvmer.mousehole.util.Config;
import com.jvmer.mousehole.util.Constant;

/**
 * 主类
 * @author butnet
 */
public class Main {
	/**
	 * 配置项
	 */
	Map<String, String> config = new LinkedHashMap<String, String>();
	/**
	 * 控制服务器地址
	 */
	String controlHost = null;
	/**
	 * 控制服务器端口
	 */
	int controlPort = Constant.DEFAULT_CONTROL_LISTENER_PORT;
	/**
	 * 公网服务地址
	 */
	String publicPublishHost = null;
	/**
	 * 公网服务端口
	 */
	int publicPublishPort = Constant.DEFAULT_PUBLIC_PUBLISH_PORT;
	/**
	 * 链接超时时间
	 */
	long connectTimeOut = Constant.DEFAULT_CONNECT_TIMEOUT;
	/**
	 * 控制服务(对内)
	 */
	ControlServer controlServer = null;
	/**
	 * 发布服务(对外)
	 */
	PublishServer publishServer = null;
	
	/**
	 * 启动控制服务与发布服务
	 * @param args
	 */
	public static void main(String args[]){
		new Main().start();
	}
	
	/**
	 * 启动服务
	 */
	public void start() {
		//读取配置文件server.conf
		Config.readConfig(Constant.CONFIG_FILE_NAME, config);
		//获取控制服务器地址
		controlHost = config.get(Constant.CONTROL_HOST);
		//获取公网服务地址
		publicPublishHost = config.get(Constant.PUBLISH_SERVER_HOST);
		//设置控制服务端口
		if(config.containsKey(Constant.CONTROL_PORT)){
			String tmp = config.get(Constant.CONTROL_PORT);
			controlPort = Integer.parseInt(tmp);
		}
		//设置公网服务端口
		if(config.containsKey(Constant.PUBLISH_SERVER_PORT)){
			String tmp = config.get(Constant.PUBLISH_SERVER_PORT);
			publicPublishPort = Integer.parseInt(tmp);
		}
		//解析连接超时时间
		if(config.containsKey(Constant.CONNECT_TIMEOUT)){
			connectTimeOut = Long.parseLong(config.get(Constant.CONNECT_TIMEOUT));
		}
		
		initDataBinLogFolder();
		
		//启动控制服务
		controlServer = new ControlServer(controlHost, controlPort);
		controlServer.start();
		
		//启动发布服务
		publishServer = new PublishServer(publicPublishHost, publicPublishPort, controlServer, connectTimeOut);
		publishServer.start();
	}

	/**
	 * 初始化Bin log目录
	 */
	private void initDataBinLogFolder() {
		File file = new File("receives/control");
		if(!file.exists())
			file.mkdirs();
		file = new File("receives/proxy");
		if(!file.exists())
			file.mkdirs();
		file = new File("receives/publish");
		if(!file.exists())
			file.mkdirs();
		file = new File("logs");
		if(!file.exists())
			file.mkdirs();
	}
}
