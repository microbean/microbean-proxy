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

import java.util.List;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.microbean.attributes.Attributes;

import org.microbean.bean.BeanTypeList;
import org.microbean.bean.Id;

import org.microbean.construct.Domain;

import static javax.lang.model.element.ElementKind.INTERFACE;

import static javax.lang.model.type.TypeKind.DECLARED;

import static org.microbean.bean.BeanTypes.proxiableBeanType;

/**
 * Information about a proxy.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
// Deliberately not final.
public class ProxySpecification {


  /*
   * Instance fields.
   */


  private final Domain domain;

  private final DeclaredType sc;

  private final List<TypeMirror> interfaces;

  private final List<Attributes> attributes;

  private final String name;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link ProxySpecification}.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @param id an {@link Id}; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception IllegalArgumentException if {@code types} does not represent a type that can be proxied
   *
   * @see org.microbean.bean.BeanTypes#proxiableBeanType(TypeMirror)
   */
  public ProxySpecification(final Domain domain, final Id id) {
    super();
    this.domain = domain; // not nullable
    this.attributes = id.attributes();
    final BeanTypeList types = id.types();
    final TypeMirror t = types.get(0); // putative superclass
    if (t.getKind() != DECLARED || domain.javaLangObject(t) && types.size() == 1) {
      throw new IllegalArgumentException("id: " + id);
    } else if (((DeclaredType)t).asElement().getKind() == INTERFACE) {
      this.sc = (DeclaredType)domain.javaLangObject().asType();
      this.interfaces = types;
    } else if (!proxiableBeanType(t)) {
      throw new IllegalArgumentException("id: " + id);
    } else {
      this.sc = (DeclaredType)t;
      this.interfaces = types.interfaces();
    }
    this.name = computeName(domain, this.sc, this.interfaces);
  }


  /*
   * Instance methods.
   */


  @Override // Object
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      final ProxySpecification her = (ProxySpecification)other;
      if (!this.domain.equals(her.domain)) {
        return false;
      }
      if (!this.domain.sameType(this.superclass(), her.superclass())) {
        return false;
      }
      final List<TypeMirror> interfaces = this.interfaces();
      final List<TypeMirror> herInterfaces = her.interfaces();
      final int size = interfaces.size();
      if (herInterfaces.size() != size) {
        return false;
      }
      for (int i = 0; i < size; i++) {
        if (!this.domain.sameType(interfaces.get(i), herInterfaces.get(i))) {
          return false;
        }
      }
      return this.attributes().equals(her.attributes());
    } else {
      return false;
    }
  }

  @Override // Object
  public int hashCode() {
    int hashCode = 31;
    hashCode = 17 * hashCode + this.domain.hashCode();
    hashCode = 17 * hashCode + this.superclass().hashCode();
    hashCode = 17 * hashCode + this.interfaces().hashCode();
    hashCode = 17 * hashCode + this.attributes().hashCode();
    return hashCode;
  }

  /**
   * Returns an immutable {@link List} of {@link Attributes} describing this {@link ProxySpecification}.
   *
   * @return a non-{@code null}, immutable {@link List} of {@link Attributes} instances
   */
  public final List<Attributes> attributes() {
    return this.attributes;
  }

  /**
   * Returns the interfaces the proxy should implement.
   *
   * @return a non-{@code null}, immutable {@link List} of {@link TypeMirror}s
   */
  public final List<TypeMirror> interfaces() {
    return this.interfaces;
  }

  /**
   * Returns the name the proxy class should have.
   *
   * @return a non-{@code null} {@link String}
   */
  public final String name() {
    return this.name;
  }

  /**
   * Returns the superclass the proxy should specialize.
   *
   * @return a non-{@code null} {@link DeclaredType}
   */
  public final DeclaredType superclass() {
    return this.sc;
  }


  /*
   * Static methods.
   */


  static final String computeName(final Domain domain, final DeclaredType superclass, final List<TypeMirror> interfaces) {

    // TODO: there will absolutely be edge cases here and we know this is not complete.

    if (superclass.getKind() != DECLARED) {
      throw new IllegalArgumentException("superclass: " + superclass);
    }
    final DeclaredType proxyClassSibling;
    if (domain.javaLangObject(superclass)) {
      if (interfaces.isEmpty()) {
        throw new IllegalArgumentException("interfaces.isEmpty(); superclass: java.lang.Object");
      }
      // Interface-only. There will be at least one and it will be the most specialized.
      proxyClassSibling = (DeclaredType)interfaces.get(0);
      if (proxyClassSibling.getKind() != DECLARED) {
        throw new IllegalArgumentException("interfaces: " + interfaces);
      }
    } else {
      proxyClassSibling = superclass;
    }
    return domain.toString(domain.binaryName((TypeElement)proxyClassSibling.asElement())) + "_Proxy";
  }

}
