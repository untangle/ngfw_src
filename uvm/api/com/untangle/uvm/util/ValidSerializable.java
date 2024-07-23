/**
 * $Id$
 */
package com.untangle.uvm.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that classes which require serialization within the ngfw package 
 * must be annotated with @ValidSerializable. This ensures protection against serialization attacks 
 * where users could load custom classes and improperly configure settings, potentially 
 * compromising system security.
 */
@Retention(RetentionPolicy.RUNTIME) 
@Target(ElementType.TYPE)
public @interface ValidSerializable {
}
