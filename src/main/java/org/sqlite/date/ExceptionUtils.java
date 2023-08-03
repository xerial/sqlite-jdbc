/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sqlite.date;

/**
 * Provides utilities for manipulating and examining <code>Throwable</code> objects.
 *
 * @since 1.0
 */
public class ExceptionUtils {

    /**
     * Throw a checked exception without adding the exception to the throws clause of the calling
     * method. This method prevents throws clause pollution and reduces the clutter of "Caused by"
     * exceptions in the stacktrace.
     *
     * <p>The use of this technique may be controversial, but exceedingly useful to library
     * developers. <code>
     *  public int propagateExample { // note that there is no throws clause
     *      try {
     *          return invocation(); // throws IOException
     *      } catch (Exception e) {
     *          return ExceptionUtils.rethrow(e);  // propagates a checked exception
     *      }
     *  }
     * </code>
     *
     * <p>This is an alternative to the more conservative approach of wrapping the checked exception
     * in a RuntimeException: <code>
     *  public int wrapExample { // note that there is no throws clause
     *      try {
     *          return invocation(); // throws IOException
     *      } catch (Error e) {
     *          throw e;
     *      } catch (RuntimeException e) {
     *          throw e;  // wraps a checked exception
     *      } catch (Exception e) {
     *          throw new UndeclaredThrowableException(e);  // wraps a checked exception
     *      }
     *  }
     * </code>
     *
     * <p>One downside to using this approach is that the java compiler will not allow invoking code
     * to specify a checked exception in a catch clause unless there is some code path within the
     * try block that has invoked a method declared with that checked exception. If the invoking
     * site wishes to catch the shaded checked exception, it must either invoke the shaded code
     * through a method re-declaring the desired checked exception, or catch Exception and use the
     * instanceof operator. Either of these techniques are required when interacting with non-java
     * jvm code such as Jython, Scala, or Groovy, since these languages do not consider any
     * exceptions as checked.
     *
     * @since 3.5
     * @see {{@link #wrapAndThrow(Throwable)}
     * @param throwable The throwable to rethrow.
     * @return R Never actually returns, this generic type matches any type which the calling site
     *     requires. "Returning" the results of this method, as done in the propagateExample above,
     *     will satisfy the java compiler requirement that all code paths return a value.
     * @throws throwable
     */
    public static <R> R rethrow(Throwable throwable) {
        // claim that the typeErasure invocation throws a RuntimeException
        return ExceptionUtils.<R, RuntimeException>typeErasure(throwable);
    }

    /**
     * Claim a Throwable is another Exception type using type erasure. This hides a checked
     * exception from the java compiler, allowing a checked exception to be thrown without having
     * the exception in the method's throw clause.
     */
    @SuppressWarnings("unchecked")
    private static <R, T extends Throwable> R typeErasure(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
