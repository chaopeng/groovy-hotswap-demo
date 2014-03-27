package me.chaopeng.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 包扫描器
 * @author chao
 *
 */
public class ClassPathScanner {

	private static final Logger logger = LoggerFactory
			.getLogger(ClassPathScanner.class);

	/**
	 * 扫描包
	 * @param basePackage 基础包
	 * @param recursive 是否递归搜索子包
	 * @param excludeInner 是否排除内部类 true->是 false->否
	 * @param checkInOrEx 过滤规则适用情况 true—>搜索符合规则的 false->排除符合规则的
	 * @param classFilterStrs List<java.lang.String> 自定义过滤规则，如果是null或者空，即全部符合不过滤
	 * @return Set
	 */
	public static Set<Class> scan(String basePackage, boolean recursive, boolean excludeInner, boolean checkInOrEx,
			List<String> classFilterStrs) {
		Set<Class> classes = new LinkedHashSet<>();
		String packageName = basePackage;
		List<Pattern> classFilters = toClassFilters(classFilterStrs);
		
		if (packageName.endsWith(".")) {
			packageName = packageName
					.substring(0, packageName.lastIndexOf('.'));
		}
		String package2Path = packageName.replace('.', '/');

		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader()
					.getResources(package2Path);
			while (dirs.hasMoreElements()) {
				URL url = dirs.nextElement();
				String protocol = url.getProtocol();
				if ("file".equals(protocol)) {
					logger.debug("扫描file类型的class文件....");
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					doScanPackageClassesByFile(classes, packageName, filePath,
							recursive, excludeInner, checkInOrEx, classFilters);
				} else if ("jar".equals(protocol)) {
					logger.debug("扫描jar文件中的类....");
					doScanPackageClassesByJar(packageName, url, recursive,
							classes, excludeInner, checkInOrEx, classFilters);
				}
			}
		} catch (IOException e) {
			logger.error("IOException error:", e);
		}

		return classes;
	}

	/**
	 * 以jar的方式扫描包下的所有Class文件
	 */
	private static void doScanPackageClassesByJar(String basePackage, URL url,
			final boolean recursive, Set<Class> classes, boolean excludeInner, boolean checkInOrEx, List<Pattern> classFilters) {
		String packageName = basePackage;
		String package2Path = packageName.replace('.', '/');
		JarFile jar;
		try {
			jar = ((JarURLConnection) url.openConnection()).getJarFile();
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (!name.startsWith(package2Path) || entry.isDirectory()) {
					continue;
				}

				// 判断是否递归搜索子包
				if (!recursive
						&& name.lastIndexOf('/') != package2Path.length()) {
					continue;
				}
				// 判断是否过滤 inner class
				if (excludeInner && name.indexOf('$') != -1) {
					logger.debug("exclude inner class with name:" + name);
					continue;
				}
				String classSimpleName = name
						.substring(name.lastIndexOf('/') + 1);
				// 判定是否符合过滤条件
				if (filterClassName(classSimpleName, checkInOrEx, classFilters)) {
					String className = name.replace('/', '.');
					className = className.substring(0, className.length() - 6);
					try {
						classes.add(Thread.currentThread()
								.getContextClassLoader().loadClass(className));
					} catch (ClassNotFoundException e) {
						logger.error("Class.forName error:", e);
					}
				}
			}
		} catch (IOException e) {
			logger.error("IOException error:", e);
		}
	}

	/**
	 * 以文件的方式扫描包下的所有Class文件
	 * 
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 * @param classes
	 */
	private static void doScanPackageClassesByFile(Set<Class> classes,
			String packageName, String packagePath, boolean recursive, final boolean excludeInner, final boolean checkInOrEx, final List<Pattern> classFilters) {
		File dir = new File(packagePath);
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		final boolean fileRecursive = recursive;
		File[] dirfiles = dir.listFiles(new FileFilter() {
			// 自定义文件过滤规则
			public boolean accept(File file) {
				if (file.isDirectory()) {
					return fileRecursive;
				}
				String filename = file.getName();
				if (excludeInner && filename.indexOf('$') != -1) {
					logger.debug("exclude inner class with name:" + filename);
					return false;
				}
				return filterClassName(filename, checkInOrEx, classFilters);
			}
		});
		for (File file : dirfiles) {
			if (file.isDirectory()) {
				doScanPackageClassesByFile(classes,
						packageName + "." + file.getName(),
						file.getAbsolutePath(), recursive, excludeInner, checkInOrEx, classFilters);
			} else {
				String className = file.getName().substring(0,
						file.getName().length() - 6);
				try {
					classes.add(Thread.currentThread().getContextClassLoader()
							.loadClass(packageName + '.' + className));

				} catch (ClassNotFoundException e) {
					logger.error("IOException error:", e);
				}
			}
		}
	}

	/**
	 * 根据过滤规则判断类名
	 */
	private static boolean filterClassName(String className, boolean checkInOrEx, List<Pattern> classFilters) {
		if (!className.endsWith(".class")) {
			return false;
		}
		if (null == classFilters || classFilters.isEmpty()) {
			return true;
		}
		String tmpName = className.substring(0, className.length() - 6);
		boolean flag = false;
		for (Pattern p : classFilters) {
			if (p.matcher(tmpName).find()) {
				flag = true;
				break;
			}
		}
		return (checkInOrEx && flag) || (!checkInOrEx && !flag);
	}

	/**
	 * @param pClassFilters the classFilters to set
	 */
	private static List<Pattern> toClassFilters(List<String> pClassFilters) {
		List<Pattern> classFilters = new ArrayList<Pattern>();
		if(pClassFilters!=null){
			
			for (String s : pClassFilters) {
				String reg = "^" + s.replace("*", ".*") + "$";
				Pattern p = Pattern.compile(reg);
				classFilters.add(p);
			}
		}
		return classFilters;
	}

}
