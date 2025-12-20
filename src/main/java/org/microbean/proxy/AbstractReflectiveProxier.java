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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import java.util.List;

import java.util.function.Supplier;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.microbean.construct.Domain;

/**
 * An {@link AbstractProxier} that helps subclasses create {@link Proxy proxies} using the {@link
 * java.lang.reflect.Proxy java.lang.reflect.Proxy} machinery present in the Java Development Kit.
 *
 * <p>This class also contains various {@code protected} utility methods that help with converting reflective {@link
 * Type}s and {@link Executable}s to their {@link TypeMirror} and {@link ExecutableElement} counterparts.</p>
 *
 * @param <PS> the {@link ProxySpecification} type
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #proxy(ProxySpecification, Supplier)
 */
public abstract non-sealed class AbstractReflectiveProxier<PS extends ProxySpecification> extends AbstractProxier<PS> {

  private static final TypeMirror[] EMPTY_TYPE_MIRROR_ARRAY = new TypeMirror[0];

  /**
   * Creates a new {@link AbstractReflectiveProxier} implementation.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   */
  protected AbstractReflectiveProxier(final Domain domain) {
    super(domain);
  }

  /**
   * A convenience method that returns {@code true} if the supplied {@link Method} is the {@link Object#equals(Object)
   * java.lang.Object#equals(java.lang.Object)} method.
   *
   * @param m a {@link Method}; must not be {@code null}
   *
   * @return {@code true} if the supplied {@link Method} is the {@link Object#equals(Object)
   * java.lang.Object#equals(java.lang.Object)} method
   *
   * @exception NullPointerException if {@code m} is {@code null}
   */
  // (Convenience.)
  protected final boolean equalsMethod(final Method m) {
    return
      m.getDeclaringClass() == Object.class &&
      m.getReturnType() == boolean.class &&
      m.getParameterCount() == 1 &&
      m.getParameterTypes()[0] == Object.class &&
      m.getName().equals("equals");
  }

  /**
   * A convenience method that returns {@code true} if the supplied {@link Method} is the {@link Object#hashCode()
   * java.lang.Object#hashCode()} method.
   *
   * @param m a {@link Method}; must not be {@code null}
   *
   * @return {@code true} if the supplied {@link Method} is the {@link Object#hashCode() java.lang.Object#hashCode()}
   * method
   *
   * @exception NullPointerException if {@code m} is {@code null}
   */
  // (Convenience.)
  protected final boolean hashCodeMethod(final Method m) {
    return
      m.getDeclaringClass() == Object.class &&
      m.getReturnType() == int.class &&
      m.getParameterCount() == 0 &&
      m.getName().equals("hashCode");
  }

  /**
   * Returns a {@link Proxy} appropriate for the supplied specification and {@link Supplier} of instances.
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
   *
   * @exception IllegalArgumentException if the {@linkplain ProxySpecification#superclass() superclass} is not {@link
   * Object java.lang.Object} (only interfaces may be proxied reflectively)
   *
   * @see #proxy(ProxySpecification, Class[], Supplier)
   *
   * @see #classLoader()
   */
  @Override // AbstractProxier<PS>
  public final <R> Proxy<R> proxy(final PS ps, final Supplier<? extends R> instanceSupplier) {
    final Domain domain = this.domain();
    if (!domain.javaLangObject(ps.superclass())) {
      throw new IllegalArgumentException("ps: " + ps);
    }
    final List<? extends TypeMirror> interfaceTypeMirrors = ps.interfaces();
    final int size = interfaceTypeMirrors.size();
    final Class<?>[] interfaces = new Class<?>[size];
    final ClassLoader classLoader = this.classLoader();
    try {
      for (int i = 0; i < size; i++) {
        final TypeElement e = (TypeElement)((DeclaredType)interfaceTypeMirrors.get(i)).asElement();
        final String binaryName = domain.toString(domain.binaryName(e));
        interfaces[i] = Class.forName(binaryName, false, classLoader);
      }
    } catch (final ClassNotFoundException cnfe) {
      throw new IllegalArgumentException("ps: " + ps, cnfe);
    }
    return this.proxy(ps, interfaces, instanceSupplier);
  }

  /**
   * Returns a {@link Proxy} appropriate for the supplied specification and {@link Supplier} of contextual instances.
   *
   * @param <R> the contextual instance type
   *
   * @param ps an appropriate proxy specification; must not be {@code null}
   *
   * @param interfaces the interfaces to implement; every element is guaranteed to {@linkplain Class#isInterface() be an interface}
   *
   * @param instanceSupplier a {@link Supplier} of contextual instances; must not be {@code null}; may or may not create
   * a new contextual instance each time it is invoked; may or may not be invoked multiple times depending on the
   * subclass implementation
   *
   * @return a non-{@code null} {@link Proxy}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  protected abstract <R> Proxy<R> proxy(final PS ps,
                                        final Class<?>[] interfaces,
                                        final Supplier<? extends R> instanceSupplier);

}
