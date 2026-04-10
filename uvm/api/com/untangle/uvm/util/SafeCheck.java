/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field as requiring shell-safety sanitization.
 * Fields annotated with {@code @SafeCheck} are automatically sanitized by
 * {@link SafeCheckValidator#validateAll(Object)} in the JSON-RPC
 * {@code preInvokeCallback}, stripping command injection constructs
 * (backtick substitution, {@code $(cmd)}, {@code ${var}}, chaining
 * operators, pipes, redirects, and newline injection) before settings
 * reach application code.
 *
 * <h3>Usage rules</h3>
 * <ul>
 *   <li>Apply only to instance {@code String} fields. Non-String and
 *       static fields are ignored by the validator.</li>
 *   <li>The validator traverses into nested objects and Collections/Maps
 *       automatically, so annotating a field on a deeply nested class
 *       (e.g. {@code IpsecVpnTunnel.description}) works as long as the
 *       parent settings object is reachable via the JSON-RPC call.</li>
 *   <li>Collection and Map fields <b>must</b> declare their generic type
 *       parameters (e.g. {@code List<MySettings>}, not raw {@code List}).
 *       The validator uses compile-time generic type info to determine
 *       whether a Collection's elements need scanning. Raw types make the
 *       element type invisible and the annotated fields will be silently
 *       skipped.</li>
 *   <li>Inherited fields are supported - the validator walks the
 *       superclass chain.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SafeCheck
{
}
