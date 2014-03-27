package me.chaopeng.hotswap;

import groovy.lang.GroovyClassLoader;
import me.chaopeng.utils.ClassPathScanner;
import me.chaopeng.utils.DirUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Main
 *
 * @author chao
 * @version 1.0 - 2014-03-27
 */
public class HotSwapDemo {

	private static Logger logger = LoggerFactory.getLogger(HotSwapDemo.class);
	private static boolean PRODUCT_MODE = false;

	public static void main(String[] args) throws Exception {

		GroovyClassLoader gcl = new GroovyClassLoader();
		Dispatcher dispatcher = new Dispatcher();

		Set<Class> classpathClasses = ClassPathScanner.scan("me.chaopeng.hotswap.handler", false, false, false, null);
		if(classpathClasses.isEmpty()) {
			PRODUCT_MODE = true;
			logger.info("production mode - supported hotswap");
			logger.debug("cannot find handlers in classpath");

			Collection<Class> fileClasses = loadClassFromFile(gcl, HotSwapDemo.class.getClassLoader().getResource("handlers").getFile());
			logger.debug("load handlers from files");
			dispatcher.load(fileClasses);
		}
		else {
			logger.info("development mode - unsupported hotswap");
			logger.debug("load handlers from classpath");
			dispatcher.load(classpathClasses);
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String str = br.readLine().trim();
			if(str.equals("exit")) {
				break;
			} else if(str.equals("reload")){
				if(PRODUCT_MODE) {
					Collection<Class> fileClasses = loadClassFromFile(gcl, HotSwapDemo.class.getClassLoader().getResource("handlers").getFile());
					logger.debug("reload handlers from files");
					dispatcher.load(fileClasses);
				}
			} else {
				int cmd = Integer.valueOf(str);
				dispatcher.invoke(cmd);
			}
		}
	}

	private static Collection<Class> loadClassFromFile(GroovyClassLoader gcl, String path) throws IOException {
		gcl.clearCache();
		File[] files = DirUtils.ls(path, "groovy");

		List<Class> classes = new ArrayList<>();
		for (File file : files) {
			logger.debug("load file = {}", file.getAbsolutePath());
			Class cls = gcl.parseClass(file);
			classes.add(cls);
		}

		return classes;
	}
}
