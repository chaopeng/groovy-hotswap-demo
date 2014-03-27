package me.chaopeng.hotswap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatcher
 *
 * @author chao
 * @version 1.0 - 2014-03-27
 */
public class Dispatcher {

	public static class Commander {
		private final Object o;
		private final Method method;

		public Commander(Object o, Method method) {
			this.o = o;
			this.method = method;
		}
	}

	private Map<Integer, Commander> commanders = new HashMap<>();

	/**
	 * 协议加载 非线程安全，多线程调用的是傻逼吗？
	 */
	public void load(Collection<Class> classes){
		for (Class cls : classes) {

			try {
				Object o = cls.newInstance();
				Method[] methods = cls.getDeclaredMethods();

				for (Method method : methods) {
					CMD cmd = method.getAnnotation(CMD.class);
					if(cmd != null) {
						commanders.put(cmd.value(), new Commander(o, method));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}


		}
	}

	/**
	 * 协议调用
	 */
	public void invoke(int cmd){
		Commander commander = commanders.get(cmd);
		if(commander != null) {
			try {
				commander.method.invoke(commander.o);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
}
