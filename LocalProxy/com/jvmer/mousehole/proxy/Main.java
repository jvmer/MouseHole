package com.jvmer.mousehole.proxy;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.jvmer.mousehole.util.Config;
import com.jvmer.mousehole.util.Constant;

/**
 * 本地代理服务启动主类
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
	int controlPort = 0;
	/**
	 * 目标服务地址
	 */
	String localAppDestHost = null;
	/**
	 * 目标服务端口
	 */
	int localAppDestPort = 0;
	/**
	 * 连接超时时间
	 */
	long connectTimeOut = Constant.DEFAULT_CONNECT_TIMEOUT;
	/**
	 * 代理服务
	 */
	ProxyServer proxyServer = null;
	
	/**
	 * 启动控制服务与发布服务
	 * @param args
	 */
	public static void main(String args[]){
		//启动服务
		new Main().start();
	}
	
	/**
	 * 启动服务
	 */
	public void start() {
		//初始化bin log目录
		initDataBinLogFolder();
		
		//读取配置文件
		Config.readConfig(Constant.CONFIG_FILE_NAME, config);
		//目标服务地址
		localAppDestHost = config.get(Constant.LOCAL_APP_DESC_HOST);
		//控制服务地址
		controlHost = config.get(Constant.CONTROL_HOST);
		//设置端口
		if(config.containsKey(Constant.CONTROL_PORT)){
			String tmp = config.get(Constant.CONTROL_PORT);
			controlPort = Integer.parseInt(tmp);
		}
		if(config.containsKey(Constant.LOCAL_APP_DESC_PORT)){
			String tmp = config.get(Constant.LOCAL_APP_DESC_PORT);
			localAppDestPort = Integer.parseInt(tmp);
		}
		//设置超时时间
		if(config.containsKey(Constant.CONNECT_TIMEOUT)){
			connectTimeOut = Long.parseLong(config.get(Constant.CONNECT_TIMEOUT));
		}
		
		proxyServer = new ProxyServer(localAppDestHost, localAppDestPort, controlHost, controlPort, connectTimeOut);
		proxyServer.start();
		
		//守护线程
		new DaemonThread().start();
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
	}
}
