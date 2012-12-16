/*--------------------------------------------------------------------------
 *  Copyright 2009 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// sqlite-jdbc Project
//
// ResourceFinder.java
// Since: Apr 28, 2009
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import java.net.URL;

/**
 * Resource address finder for files inside the jar file
 * 
 * @author leo
 * 
 */
public class ResourceFinder
{
    /**
     * Gets the {@link URL} of the file resource
     * 
     * @param referenceClass
     *            the base class for finding resources files. This method will
     *            search the package containing the given referenceClass.
     * @param resourceFileName
     *            the resource file name relative to the package of the
     *            referenceClass
     * @return the URL of the file resource
     */
    public static URL find(Class< ? > referenceClass, String resourceFileName)
    {
        return find(referenceClass.getClassLoader(), referenceClass.getPackage(), resourceFileName);
    }

    /**
     * Finds the {@link URL} of the resource
     * 
     * @param basePackage
     *            the base package to find the resource
     * @param resourceFileName
     *            the resource file name relative to the package folder
     * @return the URL of the specified resource
     */
    public static URL find(ClassLoader classLoader, Package basePackage, String resourceFileName)
    {
        return find(classLoader, basePackage.getName(), resourceFileName);
    }

    /**
     * Finds the {@link URL} of the resource
     * 
     * @param packageName
     *            the base package name to find the resource
     * @param resourceFileName
     *            the resource file name relative to the package folder
     * @return the URL of the specified resource
     */
    public static URL find(ClassLoader classLoader, String packageName, String resourceFileName)
    {
        String packagePath = packagePath(packageName);
        String resourcePath = packagePath + resourceFileName;
        if (!resourcePath.startsWith("/"))
            resourcePath = "/" + resourcePath;

        return classLoader.getResource(resourcePath);
    }

    @SuppressWarnings("unused")
    private static String packagePath(Class< ? > referenceClass)
    {
        return packagePath(referenceClass.getPackage());
    }

    /**
     * @param basePackage Package object
     * @return Package path String in the unix-like format.
     */
    private static String packagePath(Package basePackage)
    {
        return packagePath(basePackage.getName());
    }

    /**
     * @param packageName Package name string
     * @return Package path String in the unix-like format.
     */
    private static String packagePath(String packageName)
    {
        String packageAsPath = packageName.replaceAll("\\.", "/");
        return packageAsPath.endsWith("/") ? packageAsPath : packageAsPath + "/";
    }

}
