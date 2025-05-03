/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2025 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.proxy;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import java.lang.System.Logger;

import java.util.Objects;

import java.util.function.Supplier;

import org.microbean.construct.Domain;

import static java.lang.System.getLogger;

import static java.lang.System.Logger.Level.DEBUG;

/**
 * An {@link AbstractProxier} built using some kind of <dfn>toolkit</dfn>, such as <a
 * href="https://bytebuddy.net/#/">Byte Buddy</a> and the like.
 *
 * @param <PS> the {@link ProxySpecification} type
 *
 * @param <T> the toolkit representation of an unloaded {@link Class}
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public abstract non-sealed class AbstractToolkitProxier<PS extends ProxySpecification, T> extends AbstractProxier<PS> {

  private static final Logger LOGGER = getLogger(AbstractToolkitProxier.class.getName());

  private final Lookup lookup;

  /**
   * Creates a new {@link AbstractToolkitProxier}.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @param lookup a {@link Lookup}; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @see #lookup(Class)
   */
  protected AbstractToolkitProxier(final Domain domain, final Lookup lookup) {
    super(domain);
    // see #lookup(Class) below
    this.lookup = Objects.requireNonNull(lookup, "lookup");
  }

  /**
   * Creates a generated class definition from the information present in the supplied {@link ProxySpecification}, and
   * returns it for eventual supplying to an inovcation of the {@link #proxyClass(Object, ClassLoader)} method.
   *
   * @param ps a {@link ProxySpecification}; must not be {@code null}
   *
   * @return a non-{@code null} generated class definition
   *
   * @exception NullPointerException if {@code ps} is {@code null}
   *
   * @exception Throwable if creation of the generated class definition fails
   *
   * @see #proxyClass(Object, ClassLoader)
   */
  protected abstract T generate(final PS ps) throws Throwable;

  /**
   * Returns a {@link Lookup} suitable for the supplied {@link Class}.
   *
   * @param c a {@link Class}; must not be {@code null}
   *
   * @return a {@link Lookup} suitable for the supplied {@link Class}
   8
   * @see Lookup#in(Class)
   */
  protected Lookup lookup(final Class<?> c) {
    return this.lookup.in(c);
  }

  /**
   * Loads and returns the proxy class corresponding to the supplied proxy specification, {@linkplain
   * #generate(ProxySpecification) generating it} first if necessary.
   *
   * @param ps a proxy specification; must not be {@code null}
   *
   * @return a non-{@code null} {@link Class} that is guaranteed to be {@linkplain Class#isAssignableFrom(Class)
   * assignable to} {@link Proxy Proxy.class}
   *
   * @exception ClassNotFoundException if the class could not be found
   *
   * @see #generate(ProxySpecification)
   *
   * @see #proxyClass(Object, ClassLoader)
   */
  protected final Class<?> proxyClass(final PS ps) throws ClassNotFoundException {
    final String name = ps.name();
    final ClassLoader cl = this.classLoader();
    Class<?> pc;
    try {
      pc = cl.loadClass(name);
    } catch (ClassNotFoundException primaryLoadException) {
      try {
        pc = this.proxyClass(this.generate(ps), cl);
        if (pc == null) {
          primaryLoadException = new ClassNotFoundException(name + "; proxyClass(generate(" + ps + "), " + cl + " == null");
          throw primaryLoadException;
        }
        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "Proxy class generated because it was not initially found", primaryLoadException);
        }
      } catch (final ClassNotFoundException | RuntimeException generationException) {
        try {
          // We try again; generation may have failed because of a race condition defining a class in the
          // classloader. This will probably fail 99.9% of the time.
          pc = cl.loadClass(name);
        } catch (final ClassNotFoundException secondaryLoadException) {
          generationException.addSuppressed(primaryLoadException);
          secondaryLoadException.addSuppressed(generationException);
          throw secondaryLoadException;
        } catch (final Error e) {
          e.addSuppressed(generationException);
          throw e;
        } catch (final Throwable wtf) {
          generationException.addSuppressed(wtf);
          throw generationException;
        }
      } catch (final Error e) {
        e.addSuppressed(primaryLoadException);
        throw e;
      } catch (final Throwable wtf) {
        primaryLoadException.addSuppressed(wtf);
        throw primaryLoadException;
      }
    }
    return pc;
  }

  /**
   * Called by the {@link #proxyClass(ProxySpecification)} method, converts the supplied class definition into a {@link
   * Class}, using the supplied {@link ClassLoader}, and returns it.
   *
   * <p>Overrides of this method must not call the {@link #proxyClass(ProxySpecification)} method or undefined behavior
   * may result.</p>
   *
   * @param t a class definition; must not be {@code null}
   *
   * @param cl a {@link ClassLoader}; must not be {@code null}
   *
   * @return a non-{@code null} {@link Class}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception ClassNotFoundException if an invocation of {@link ClassLoader#loadClass(String)} fails
   *
   * @see #proxyClass(ProxySpecification)
   */
  // Turns a T into a Class (loads it) using a ClassLoader.
  protected abstract Class<?> proxyClass(final T t, final ClassLoader cl) throws ClassNotFoundException;

}
