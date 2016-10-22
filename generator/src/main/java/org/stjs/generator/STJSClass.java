/**
 *  Copyright 2011 Alexandru Craciun, Eyal Kaspi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.stjs.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.stjs.generator.name.DependencyType;
import org.stjs.generator.utils.ClassUtils;
import org.stjs.generator.utils.PreConditions;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * This class represents a class and the corresponding generated javascript file. The information about dependencies and
 * sources are stored at generation time in a properties file that has as name [class-name].stjs (and it's packed along
 * with the source file in the same folder). Thus, if a STJS library is built, it will be delivered with all this
 * information, as the original Java code will no longer be available with the library.
 *
 * @author acraciun
 */
public class STJSClass implements ClassWithJavascript {
	private static final Logger LOG = Logger.getLogger(STJSClass.class.getName());

	private static final String DEPENDENCIES_PROP = "dependencies";
	public static final String CLASS_PROP = "class";
	private static final String GENERATED_JS_FILE_PROP = "js";
	public static final String JS_NAMESPACE = "jsNamespace";

	private final Properties properties;

	private final ClassResolver classResolver;
	private Map<String, DependencyType> dependencies = Collections.emptyMap();
	private List<ClassWithJavascript> directDependencies;
	private Map<ClassWithJavascript, DependencyType> directDependenciesMap;
	// null means namespace is unknown, empty string means no namespace
	private String javascriptNamespace;

	private URI generatedJavascriptFile;

	private final Class<?> javaClass;
	private final File targetFolder;

	/**
	 * constructor for storage, instances created with this constructor are very short lived and should only be used to
	 * output the small .stjs properties file that resides next to .class files
	 */
	public STJSClass(ClassResolver classResolver, File targetFolder, Class<?> javaClass) {
		PreConditions.checkNotNull(classResolver);
		PreConditions.checkNotNull(targetFolder);
		PreConditions.checkNotNull(javaClass);
		this.targetFolder = targetFolder;
		this.javaClass = javaClass;
		this.properties = new Properties();
		this.classResolver = classResolver;
		this.javascriptNamespace = null;
	}

	/**
	 * constructor for loading
	 */
	public STJSClass(ClassResolver classResolver, ClassLoader classLoader, Class<?> javaClass) {
		PreConditions.checkNotNull(classResolver);
		PreConditions.checkNotNull(classLoader);
		PreConditions.checkNotNull(javaClass);

		this.javaClass = javaClass;
		this.targetFolder = null;
		this.classResolver = classResolver;
		properties = loadProperties(classLoader);

		// deps
		dependencies = readDependeciesProperty();

		// js file
		generatedJavascriptFile = readGeneratedJavascriptFileProperty();

		javascriptNamespace = readJavascriptNamespaceProperty();
		if (javascriptNamespace == null) {
			// Old versions of ST-JS did not set the jsNamespace property, so we must look into the compiled
			// class to figure out the namespace
			javascriptNamespace = readJavascriptNamespaceAnnotation();
		}
	}

	private Properties loadProperties(ClassLoader classLoader) {
		Properties props = new Properties();

		InputStream inputStream = null;
		try {
			inputStream = classLoader.getResourceAsStream(ClassUtils.getPropertiesFileName(getJavaClassName()));
			if (inputStream == null) {
				LOG.severe("CANNOT find:" + ClassUtils.getPropertiesFileName(getJavaClassName()) + " clazz:"
						+ classLoader.getResource(ClassUtils.getPropertiesFileName(getJavaClassName())));
			} else {
				props.load(inputStream);
			}
		}
		catch (IOException e) {
			throw new JavascriptClassGenerationException(getJavaClassName(), e);
		}
		finally {
			Closeables.closeQuietly(inputStream);
		}
		return props;
	}

	private Map<String, DependencyType> readDependeciesProperty() {
		String depProp = properties.getProperty(DEPENDENCIES_PROP);
		if (depProp != null) {
			// remove []
			depProp = depProp.trim();
			if (depProp.length() > 2) {
				String deps[] = depProp.substring(1, depProp.length() - 1).split(",");
				Map<String, DependencyType> depMap = new HashMap<String, DependencyType>();
				for (String dep : deps) {
					depMap.put(DependencyType.getTypeName(dep), DependencyType.getDependencyType(dep));
				}
				return depMap;
			}
		}
		return Collections.emptyMap();
	}

	private String writeDependeciesProperty(Map<String, DependencyType> deps) {
		StringBuilder s = new StringBuilder();
		for (Map.Entry<String, DependencyType> entry : deps.entrySet()) {
			if (s.length() > 0) {
				s.append(',');
			}
			s.append(DependencyType.getTypeWithPrefix(entry.getKey(), entry.getValue()));
		}
		return "[" + s.toString() + "]";
	}

	private URI readGeneratedJavascriptFileProperty() {
		String jsFile = properties.getProperty(GENERATED_JS_FILE_PROP);
		if (jsFile != null) {
			try {
				return new URI(jsFile);
			}
			catch (URISyntaxException e) {
				throw new JavascriptClassGenerationException(getJavaClassName(), "Could not load URI from " + jsFile, e);
			}
		}
		return null;
	}

	private String readJavascriptNamespaceProperty() {
		return properties.getProperty(JS_NAMESPACE);
	}

	private String readJavascriptNamespaceAnnotation() {
		// If the jsNamespace property is not defined, it means that class was
		// generated with a version of ST-JS earlier than 3.1.2 that did not write that property down. Fortunately for us
		// it also means that any namespace can be read by inspecting the @Namespace annotation directly on that
		// class.
		String ns = NamespaceUtil.resolveNamespaceSimple(javaClass);
		if (ns == null) {
			// With earlier versions of ST-JS (pre 3.1.2) if no namespace is found on the class, then there is
			// no namespace at all
			ns = "";
		}
		return ns;
	}

	public File getStjsPropertiesFile() {
		File propFile = new File(targetFolder, ClassUtils.getPropertiesFileName(getJavaClassName()));
		if (!propFile.getParentFile().exists() && !propFile.getParentFile().mkdirs()) {
			throw new JavascriptClassGenerationException(getJavaClassName(), "Unable to create parent folder for the properties file:" + propFile);
		}
		return propFile;
	}

	public void store() {
		if (targetFolder == null) {
			throw new IllegalStateException("This properties file was open for read only");
		}
		Writer propertiesWriter = null;
		try {
			propertiesWriter = Files.newWriter(getStjsPropertiesFile(), Charsets.UTF_8);
			properties.setProperty(CLASS_PROP, getJavaClassName());
			properties.store(propertiesWriter, "Generated by STJS ");
		}
		catch (IOException e1) {
			throw new JavascriptClassGenerationException(getJavaClassName(), "Could not open properties file " + getStjsPropertiesFile() + ":" + e1, e1);
		}
		finally {
			try {
				if (propertiesWriter != null) {
					propertiesWriter.flush();
					propertiesWriter.close();
				}
			}
			catch (IOException e) {
				throw new JavascriptClassGenerationException(getJavaClassName(), e);
			}
		}
	}

	public void setJavascriptNamespace(String jsNamespace) {
		this.javascriptNamespace = jsNamespace;
		properties.put(JS_NAMESPACE, jsNamespace);
	}

	public void setDependencies(Map<String, DependencyType> deps) {

		if (deps == null) {
			properties.remove(DEPENDENCIES_PROP);
			this.dependencies = new HashMap<>();
		} else {
			this.dependencies = new HashMap<>(deps);
			// filter out anonymous classes
			this.dependencies.remove("");
			properties.put(DEPENDENCIES_PROP, writeDependeciesProperty(dependencies));
		}
	}

	public void setGeneratedJavascriptFile(URI generatedJavascriptFile) {
		this.generatedJavascriptFile = generatedJavascriptFile;
		if (generatedJavascriptFile == null) {
			properties.remove(GENERATED_JS_FILE_PROP);
		} else {
			properties.put(GENERATED_JS_FILE_PROP, generatedJavascriptFile.toString());
		}

	}

	@Override
	public final String getJavaClassName() {
		return javaClass.getName();
	}

	@Override
	public Class<?> getJavaClass() {
		return this.javaClass;
	}

	@Override
	public String getJavascriptClassName() {
		String simpleName = getJavaClass().getSimpleName();
		String ns = getJavascriptNamespace();
		if (ns != null && !ns.isEmpty()) {
			return ns + "." + simpleName;
		}
		return simpleName;
	}

	@Override
	public String getJavascriptNamespace() {
		return this.javascriptNamespace;
	}

	@Override
	public List<URI> getJavascriptFiles() {
		if (generatedJavascriptFile == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(generatedJavascriptFile);
	}

	@Override
	public List<ClassWithJavascript> getDirectDependencies() {
		if (directDependencies == null) {
			directDependencies = new ArrayList<ClassWithJavascript>(dependencies.size());
			for (String depClassName : dependencies.keySet()) {
				directDependencies.add(classResolver.resolve(depClassName.trim()));
			}
		}
		return directDependencies;
	}

	@Override
	public Map<ClassWithJavascript, DependencyType> getDirectDependencyMap() {
		if (directDependenciesMap == null) {
			directDependenciesMap = new HashMap<ClassWithJavascript, DependencyType>(dependencies.size());
			for (Map.Entry<String, DependencyType> entry : dependencies.entrySet()) {
				directDependenciesMap.put(classResolver.resolve(entry.getKey().trim()), entry.getValue());
			}
		}
		return directDependenciesMap;
	}

	@Override
	public String toString() {
		return "STJSClass [javaClassName=" + getJavaClassName() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getJavaClassName().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		STJSClass other = (STJSClass) obj;

		return getJavaClassName().equals(other.getJavaClassName());
	}

}
