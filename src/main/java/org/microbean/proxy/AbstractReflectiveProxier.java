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

import java.util.function.Supplier;

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
 *
 * @see #type(Type)
 *
 * @see #executableElement(Executable)
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
   * Returns {@code true} if the supplied {@link Method} is the {@link Object#equals(Object)
   * java.lang.Object#equals(java.lang.Object)} method.
   *
   * @param m a {@link Method}; must not be {@code null}
   *
   * @return {@code true} if the supplied {@link Method} is the {@link Object#equals(Object)
   * java.lang.Object#equals(java.lang.Object)} method
   *
   * @exception NullPointerException if {@code m} is {@code null}
   */
  protected final boolean equalsMethod(final Method m) {
    return
      m.getDeclaringClass() == Object.class &&
      m.getReturnType() == boolean.class &&
      m.getParameterCount() == 1 &&
      m.getParameterTypes()[0] == Object.class &&
      m.getName().equals("equals");
  }

  /**
   * Returns an {@link ExecutableElement} corresponding to the supplied {@link Executable}.
   *
   * @param e an {@link Executable}; must not be {@code null}
   *
   * @return an {@link ExecutableElement} corresponding to the supplied {@link Executable}; never {@code null}
   *
   * @exception NullPointerException if {@code e} is {@code null}
   *
   * @exception IllegalArgumentException if somehow {@code e} is neither a {@link Constructor} nor a {@link Method}
   */
  protected final ExecutableElement executableElement(final Executable e) {
    final Domain domain = domain();
    return switch (e) {
    case null -> throw new NullPointerException("e");
    case Constructor<?> c ->
      domain.executableElement(domain.typeElement(c.getDeclaringClass().getCanonicalName()),
                               domain.noType(TypeKind.VOID),
                               "<init>",
                               this.types(c.getParameterTypes()));
    case Method m ->
      domain.executableElement(domain.typeElement(m.getDeclaringClass().getCanonicalName()),
                               this.type(m.getReturnType()),
                               m.getName(),
                               this.types(m.getParameterTypes()));
    default -> throw new IllegalArgumentException("e: " + e);
    };
  }

  /**
   * Returns {@code true} if the supplied {@link Method} is the {@link Object#hashCode() java.lang.Object#hashCode()}
   * method.
   *
   * @param m a {@link Method}; must not be {@code null}
   *
   * @return {@code true} if the supplied {@link Method} is the {@link Object#hashCode() java.lang.Object#hashCode()}
   * method
   *
   * @exception NullPointerException if {@code m} is {@code null}
   */
  protected final boolean hashCodeMethod(final Method m) {
    return
      m.getDeclaringClass() == Object.class &&
      m.getReturnType() == int.class &&
      m.getParameterCount() == 0 &&
      m.getName().equals("hashCode");
  }

  /**
   * Returns a {@link Parameterizable} corresponding to the supplied {@link GenericDeclaration}.
   *
   * @param gd a {@link GenericDeclaration}; must not be {@code null}
   *
   * @return a {@link Parameterizable} corresponding to the supplied {@link GenericDeclaration}; never {@code null}
   *
   * @exception NullPointerException if {@code gd} is {@code null}
   *
   * @exception IllegalArgumentException if {@code gd} is neither a {@link Class} nor an {@link Executable}
   */
  protected final Parameterizable parameterizable(final GenericDeclaration gd) {
    final Domain domain = this.domain();
    return switch (gd) {
    case null -> throw new NullPointerException("gd");
    case Class<?> c -> domain.typeElement(c.getCanonicalName());
    case Executable e -> this.executableElement(e);
    default -> throw new IllegalArgumentException("gd: " + gd);
    };
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

  /**
   * Returns the {@link TypeMirror} corresponding to the supplied {@link Type}.
   *
   * @param t a {@link Type}; must not be {@code null}
   *
   * @return the {@link TypeMirror} corresponding to the supplied {@link Type}; never {@code null}
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @exception IllegalArgumentException if {@code t} is not a {@link Class}, {@link GenericArrayType}, {@link
   * ParameterizedType}, {@link TypeVariable} or {@link WildcardType}
   */
  protected final TypeMirror type(final Type t) {
    // TODO: anywhere there is domain.declaredType(), consider passing
    // domain.moduleElement(this.getClass().getModule().getName()) as the first argument. Not sure how this works
    // exactly but I think it might be necessary.
    final Domain domain = this.domain();
    return switch (t) {
    case null -> throw new NullPointerException("t");
    case Class<?> c when t == boolean.class -> domain.primitiveType(TypeKind.BOOLEAN);
    case Class<?> c when t == byte.class -> domain.primitiveType(TypeKind.BYTE);
    case Class<?> c when t == char.class -> domain.primitiveType(TypeKind.CHAR);
    case Class<?> c when t == double.class -> domain.primitiveType(TypeKind.DOUBLE);
    case Class<?> c when t == float.class -> domain.primitiveType(TypeKind.FLOAT);
    case Class<?> c when t == int.class -> domain.primitiveType(TypeKind.INT);
    case Class<?> c when t == long.class -> domain.primitiveType(TypeKind.LONG);
    case Class<?> c when t == short.class -> domain.primitiveType(TypeKind.SHORT);
    case Class<?> c when t == void.class -> domain.noType(TypeKind.VOID);
    case Class<?> c when t == Object.class -> domain.javaLangObject().asType(); // cheap and easy optimization
    case Class<?> c when c.isArray() -> domain.arrayTypeOf(this.type(c.getComponentType()));
    case Class<?> c -> domain.declaredType(c.getCanonicalName());
    case GenericArrayType g -> domain.arrayTypeOf(this.type(g.getGenericComponentType()));
    case ParameterizedType pt when pt.getOwnerType() == null ->
      domain.declaredType(domain.typeElement(((Class<?>)pt.getRawType()).getCanonicalName()),
                          this.types(pt.getActualTypeArguments()));
    case ParameterizedType pt ->
      domain.declaredType((DeclaredType)this.type(pt.getOwnerType()),
                          domain.typeElement(((Class<?>)pt.getRawType()).getCanonicalName()),
                          this.types(pt.getActualTypeArguments()));
    case TypeVariable<?> tv -> domain.typeVariable(this.parameterizable(tv.getGenericDeclaration()), tv.getName());
    case WildcardType w when w.getLowerBounds().length <= 0 -> domain.wildcardType(this.type(w.getUpperBounds()[0]), null);
    case WildcardType w -> domain.wildcardType(null, this.type(w.getLowerBounds()[0]));
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

  /**
   * Returns an array of {@link TypeMirror}s whose elements correspond to the elements in the supplied {@link Type} array.
   *
   * @param ts an array of {@link Type}s; must not be {@code null}
   *
   * @return an array of {@link TypeMirror}s whose elements correspond to the elements in the supplied {@link Type}
   * array; never {@code null}
   *
   * @exception NullPointerException if {@code ts} is {@code null} or contains {@code null} elements
   *
   * @exception IllegalArgumentException if any element of {@code ts} is deemed illegal by the {@link #type(Type)}
   * method
   *
   * @see #type(Type)
   */
  protected final TypeMirror[] types(final Type[] ts) {
    if (ts.length <= 0) {
      return EMPTY_TYPE_MIRROR_ARRAY;
    } else if (ts.length == 1) {
      // cheap and easy optimization
      return new TypeMirror[] { type(ts[0]) };
    }
    final TypeMirror[] rv = new TypeMirror[ts.length];
    for (int i = 0; i < ts.length; i++) {
      rv[i] = type(ts[i]);
    }
    return rv;
  }

}
