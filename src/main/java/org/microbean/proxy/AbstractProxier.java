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

import java.util.Objects;

import java.util.function.Supplier;

import org.microbean.construct.Domain;

/**
 * An abstract base class for subclassses that create {@linkplain Proxy proxies}.
 *
 * @param <PS> the {@link ProxySpecification} type
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public abstract sealed class AbstractProxier<PS extends ProxySpecification>
  permits AbstractReflectiveProxier, AbstractToolkitProxier {

  private final Domain domain;

  /**
   * Creates a new {@link AbstractProxier} implementation.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   */
  protected AbstractProxier(final Domain domain) {
    this.domain = Objects.requireNonNull(domain, "domain");
  }

  /**
   * Returns the {@link ClassLoader} for loading classes.
   *
   * <p>The default implementation of this method returns the return value of an invocation of the {@link
   * Thread#getContextClassLoader()} method.</p>
   *
   * <p>Overrides of this method must not return {@code null} or undefined behavior may result.</p>
   *
   * @return a non-{@code null} {@link ClassLoader}
   */
  protected ClassLoader classLoader() {
    return Thread.currentThread().getContextClassLoader();
  }
  
  /**
   * Returns the {@link Domain} supplied at construction time.
   *
   * @return a non-{@code null} {@link Domain}
   */
  protected final Domain domain() {
    return this.domain;
  }

  /**
   * Returns a {@link Proxy} appropriate for the supplied proxy specification and {@link Supplier} of contextual
   * instances.
   *
   * @param <R> the contextual instance type
   *
   * @param ps an appropriate proxy specification; must not be {@code null}
   *
   * @param instanceSupplier a {@link Supplier} of contextual instances; must not be {@code null}; may or may not create
   * a new contextual instance each time it is invoked; may or may not be invoked multiple times depending on the
   * subclass implementation
   *
   * @return a non-{@code null} {@link Proxy}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  public abstract <R> Proxy<R> proxy(final PS ps, final Supplier<? extends R> instanceSupplier);

}
