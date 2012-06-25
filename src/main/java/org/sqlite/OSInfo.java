/*--------------------------------------------------------------------------
 *  Copyright 2008 Taro L. Saito
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
// OSInfo.java
// Since: May 20, 2008
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

/**
 * Provides OS name and architecture name.
 * 
 * @author leo
 * 
 */
public class OSInfo
{
   public static void main(String[] args) {
      if (args.length >= 1) {
         if ("--os".equals(args[0])) {
            System.out.print(getOSName());
            return;
         }
         else if ("--arch".equals(args[0])) {
            System.out.print(getArchName());
            return;
         }
      }

      System.out.print(getNativeLibFolderPathForCurrentOS());
   }

   /**
    * Gets native library fold path for the current operation system.
    * @return In the format of <code>OS_name/OS_architecture_name</code>
    */
   public static String getNativeLibFolderPathForCurrentOS() {
       return getOSName() + "/" + getArchName();
   }

   /**
    * Gets the operating system name.
    * @return One of <code>Windows</code>, <code>Mac</code>,<code>Linux</code>
    */
   public static String getOSName() {
       return translateOSNameToFolderName(System.getProperty("os.name"));
   }

   /**
    * Gets the operation system architecture name.
    * @return
    */
   public static String getArchName() {
       return translateArchNameToFolderName(System.getProperty("os.arch"));
   }

   /**
    * Extracts operating system name from given string.
    * @param osName The given string that contains operating system name.
    * @return One of <code>Windows</code>, <code>Mac</code>,<code>Linux</code>
    */
   public static String translateOSNameToFolderName(String osName) {
      if (osName.contains("Windows")) {
          return "Windows";
      }
      else if (osName.contains("Mac")) {
          return "Mac";
      }
      else if (osName.contains("Linux")) {
          return "Linux";
      }
      else {
          return osName.replaceAll("\\W", "");
      }
   }

   /**
    * Gets the operation system architecture name from given string.
    * @param archName
    * @return
    */
   public static String translateArchNameToFolderName(String archName) {
       return archName.replaceAll("\\W", "");
   }
}
