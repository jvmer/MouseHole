package com.jvmer.mousehole.util;

/**
 * 系统常量
 * @author Administrator
 */
/**
 * @author zhangbo
 *
 */
public interface Constant {
	/**
	 * 任一IP地址
	 */
	String ANY_HOST = "0.0.0.0";
	/**
	 * 服务器配置文件名称
	 */
	String CONFIG_FILE_NAME = "server.conf";
	/**
	 * 公网服务器地址
	 */
	String PUBLISH_SERVER_HOST = "publish.server.host";
	/**
	 * 公网服务器端口
	 */
	String PUBLISH_SERVER_PORT = "publish.server.port";
	/**
	 * 代理的服务器地址
	 */
	String LOCAL_APP_DESC_HOST = "local.app.desc.host";
	/**
	 * 代理服务器端口
	 */
	String LOCAL_APP_DESC_PORT = "local.app.desc.port";
	/**
	 * 控制服务IP
	 */
	String CONTROL_HOST = "control.host";
	/**
	 * 控制服务端口
	 */
	String CONTROL_PORT = "control.port";
	/**
	 * 控制服务默认端口
	 */
	int DEFAULT_CONTROL_LISTENER_PORT = 1525;
	/**
	 * 公网服务默认端口
	 */
	int DEFAULT_PUBLIC_PUBLISH_PORT = 8080;
	/**
	 * 默认连接控制服务超时时间
	 */
	long DEFAULT_CONNECT_TIMEOUT = 60000;
//	/**
//	 * socket超时关闭的时间毫秒数
//	 */
//	String TIME_OUT_OF_SOCKET = "server.time_out_of_socket";
	
	/**
	 * 发布服务session
	 */
	String PUBLISH_DATA_SESSION = "publish_data_session";
	/**
	 * 控制服务session
	 */
	String CONTROL_SESSION = "control_session";
	/**
	 * 与代理目标服务间的session
	 */
	String LOCAL_APP_DATA_SESSION = "local.app.session";
	/**
	 * 编码器
	 */
	String ENCODER = "encoder";
	/**
	 * 解码器
	 */
	String DECODER = "decoder";
	/**
	 * 接收命令的缓冲字符串
	 */
	String BUFFER_OF_COMMAND_STRING = "buffer.command";
	/**
	 * 命令字符串的长度
	 */
	String LENGTH_OF_COMMAND = "length.command";
//	/**
//	 * 是否为发布服务session
//	 */
//	String IS_PUBLISH_SESSION = "publish";
	/**
	 * 是否为control session
	 */
	String IS_CONTROL_SESSION = "control";
//	/**
//	 * 是否在传输中
//	 */
//	String IN_TRANSPORT = "in_transport";
	/**
	 * session中的缓存字节数组
	 */
	String BUFFER_OF_COMMAND_BYTES = "buffer_bytes.command";
	/**
	 * 已经读取的数据长度
	 */
	String READ_COMMAND_LEN = "read.command.len";
	/**
	 * 连接超时配置
	 */
	String CONNECT_TIMEOUT = "connect.timeout";
	/**
	 * 控制服务中session类型: data, proxy
	 */
	String CONTROL_SESSION_TYPE = "control.session.type";
	/**
	 * 请求数据的文件输出流
	 */
	String REQUEST_OUTPUT = "request.output";
//	
//	
	//控制协议命令
    /**
     * 注册服务器
     */
    String CMD_REGEDIT = "regedit";
    /**
     * 响应成功
     */
    String CMD_OK = "ok";
    /**
     * 响应错误
     */
    String CMD_ERROR = "error";
//    /**
//     * 发布服务
//     */
//    String CMD_PUBLISH = "publish";
    /**
     * 代理服务
     */
    String CMD_PROXY = "proxy";
    /**
     * 数据session
     */
    String CMD_DATA = "data";
    /**
     * 创建create
     */
    String CMD_CREATE = "create";
}
