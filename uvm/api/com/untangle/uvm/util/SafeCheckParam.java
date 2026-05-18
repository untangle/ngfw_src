/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code String} parameter on a JSON-RPC method as requiring
 * shell-safety validation. The Jabsorb {@code preInvokeCallback}
 * inspects parameter annotations on each invoked method and routes
 * annotated String arguments through
 * {@link SafeCheckValidator#validate(String, SafeType[], String, String)}.
 * A bad value raises {@link SafeCheckValidationException} which Jabsorb
 * propagates to the client as a JSON-RPC error.
 *
 * <h3>Why a parameter-level analog to {@link SafeCheck}</h3>
 * {@code @SafeCheck} only covers fields on POJOs that pass through
 * {@code SafeCheckValidator.validateAll(Object)}. Raw {@code String}
 * arguments to RPC methods (e.g. {@code setAdminPassword(String, String, String)})
 * bypass that path entirely - {@code validateAll} short-circuits on
 * {@code java.*} objects. {@code @SafeCheckParam} closes that gap.
 *
 * <h3>Usage</h3>
 * <pre>
 *   public void setAdminPassword(
 *       &#64;SafeCheckParam(SafeType.OPAQUE_SECRET) String password,
 *       &#64;SafeCheckParam(SafeType.EMAIL)         String email,
 *       &#64;SafeCheckParam(SafeType.ALPHANUM)      String installType)
 * </pre>
 *
 * <p>Multiple types specify OR-semantics, matching {@link SafeCheck}.</p>
 *
 * <h3>Where to declare it</h3>
 * Place the annotation on the <b>implementation</b> method, not the
 * interface. Jabsorb's {@code ClassAnalyzer} resolves methods via
 * {@code Class.getMethods()} on the impl class, and Java's
 * {@code Method.getParameterAnnotations()} does <b>not</b> inherit
 * parameter annotations from interfaces.
 *
 * <h3>Null / empty policy</h3>
 * Same as {@link SafeType#validate(String)}: {@code null} and {@code ""}
 * are accepted by every type. Required-ness checks remain the caller
 * layer's responsibility - this annotation speaks only to <i>content</i>.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SafeCheckParam
{
    /**
     * The {@link SafeType}(s) the parameter's value must match. The
     * validator accepts the value if any listed type accepts it.
     */
    SafeType[] value();

    /**
     * Optional override for the per-type default error message. Empty
     * string means "use the default message(s) of the listed types".
     * The offending value is never echoed in the error.
     */
    String errorMessage() default "";
}
