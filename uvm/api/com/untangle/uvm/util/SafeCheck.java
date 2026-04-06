/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field as requiring shell-safety validation.
 * Fields annotated with @SafeCheck will be validated by
 * SafeCheckValidator.validateAll() to reject values containing
 * shell metacharacters that could enable command injection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SafeCheck
{
}
