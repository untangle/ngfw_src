/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field as requiring shell-safety validation by
 * {@link SafeCheckValidator#validateAll(Object)} in the JSON-RPC
 * {@code preInvokeCallback}. Validation is fail-fast: a bad value
 * raises {@link SafeCheckValidationException} which Jabsorb propagates
 * to the client as a JSON-RPC error.
 *
 * <h3>Usage</h3>
 * Every annotation should specify the {@link SafeType}(s) the field
 * accepts:
 *
 * <pre>
 *   &#64;SafeCheck(SafeType.HOSTNAME)
 *   private String hostName;
 *
 *   // dual-purpose field
 *   &#64;SafeCheck({ SafeType.IP_OR_CIDR, SafeType.INTERFACE })
 *   private String nextHop;
 *
 *   // override the per-type default error message for UI friendliness
 *   &#64;SafeCheck(value = SafeType.HOSTNAME,
 *              errorMessage = "Enter a valid SNMP trap target hostname.")
 *   private String trapHost;
 * </pre>
 *
 * <p>An empty {@code value()} array is permitted as a transitional
 * default so that an untyped {@code @SafeCheck} declaration continues
 * to compile during migration. The validator falls back to
 * {@link SafeType#SIMPLE_TEXT} and logs a one-time warning naming the
 * field - every shipped {@code @SafeCheck} site should declare a type
 * explicitly.</p>
 *
 * <h3>Rules</h3>
 * <ul>
 *   <li>Apply only to instance {@code String} fields. Non-String and
 *       static fields are ignored.</li>
 *   <li>The validator traverses into nested objects, Collections, Maps,
 *       and arrays automatically.</li>
 *   <li>Collection and Map fields <b>must</b> declare their generic
 *       type parameters (e.g. {@code List<MySettings>}, not raw
 *       {@code List}). Raw types make element types invisible to
 *       reflection.</li>
 *   <li>{@link SafeType#OPAQUE_SECRET} fields permit shell
 *       metacharacters by design - sinks must use
 *       {@code execCommand(...)} or {@code shlex.quote(...)} rather
 *       than string-concat into a shell template.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SafeCheck
{
    /**
     * The {@link SafeType}(s) the field's value must match. The
     * validator accepts the value if any listed type accepts it.
     *
     * <p>Default is the empty array, which exists solely so that an
     * untyped {@code @SafeCheck} declaration continues to compile
     * during migration. The validator treats an empty list as
     * {@link SafeType#SIMPLE_TEXT} and logs a warning naming the
     * field - fix by adding an explicit type.</p>
     */
    SafeType[] value() default {};

    /**
     * Optional override for the per-type default error message, useful
     * when a UI-facing label gives the admin clearer guidance than the
     * generic type description. Empty string means "use the default
     * message(s) of the listed types".
     */
    String errorMessage() default "";
}
