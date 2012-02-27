package com.jvmer.mousehole.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Config {
	private static final Logger logger = LogManager.getLogger(Config.class);
	public static <T> T readConfig(String f, T t) {
		return readConfig(new File(f), t);
	}
	@SuppressWarnings("unchecked")
	public static <T> T readConfig(File f, T t) {
		FileReader fr = null;
		BufferedReader reader = null;
		try {
			fr = new FileReader(f);
			reader = new BufferedReader(fr);

			String line = null, name = null, value = null;
			int i = -1, row = 0;
			while ((line = reader.readLine()) != null) {
				row++;
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")
						|| line.startsWith("!") || line.startsWith("//"))
					continue;
				i = line.indexOf('=');
				if (i == -1) {
					// LOG.log(Level.WARNING, "文件: "+f+" 第"+row+"行缺少'='");
					// continue;
					name = line.trim();
					value = null;
					logger.warn("文件: "+f+" 第"+row+"行缺少'=', ("+name+"=null)");
				} else {
					name = line.substring(0, i).trim();
					value = line.substring(i + 1).trim();
					while (value.endsWith("\\")) {
						value = value.substring(0, value.length() - 1);
						line = reader.readLine();
						if (line != null) {
							value += line;
						} else
							break;
					}
					value = value.replaceAll("\\\\n", "\n");
					value = value.replaceAll("\\\\r", "\r");
					value = value.replaceAll("\\\\\\\\", "\\");
				}
				if (t instanceof Map)
					((Map<Object, Object>) t).put(name, value);
				else
					BeanUtils.setProperty(t, name, value);
			}

			return t;
		} catch (Exception e) {
			logger.info("读取任务配置文件失败", e);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			if (fr != null)
				try {
					fr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return t;
	}
}
