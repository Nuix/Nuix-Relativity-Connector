dependency = __DIR__+"relativity-client-1.0.jar"

url = new java.io.File(dependency).toURI().toURL();

classLoader = java.lang.ClassLoader.getSystemClassLoader();
method = java.net.URLClassLoader.class.getDeclaredMethod("addURL", java.net.URL.class);
method.setAccessible(true);
method.invoke(classLoader, url);

uploadUi = Packages.com.nuix.relativityclient.RelativityClientUI.getInstance(utilities,currentCase,__DIR__);
