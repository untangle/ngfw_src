/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Validates {@link SafeCheck}-annotated String fields on an object
 * graph against the field's declared {@link SafeType}(s). Validation
 * is fail-fast and read-only:
 * <ul>
 *   <li>A field whose value matches at least one of its declared
 *       SafeTypes is accepted.</li>
 *   <li>A field whose value matches none of them causes
 *       {@link SafeCheckValidationException} to be thrown. Jabsorb
 *       wraps this as a JSON-RPC error returned to the client.</li>
 *   <li>The input object is never mutated. Validation runs against
 *       {@code field.get(obj)} only - no {@code field.set} call. This
 *       makes the validator safe on final fields under Java 9+.</li>
 * </ul>
 *
 * <p>Empty / null values are accepted by every type - see
 * {@link SafeType#validate(String)} for the rationale.</p>
 *
 * <h3>Object graph traversal</h3>
 * The validator dispatches on the runtime class of every value:
 * Collections recurse into elements, Maps recurse into both keys and
 * values, arrays recurse into elements, custom objects recurse into
 * their non-primitive non-{@code java.*} fields. {@code java.*}
 * objects (String, Integer, etc.) are skipped because they cannot
 * carry {@code @SafeCheck} annotations.
 *
 * <h3>Cycle and resource protection</h3>
 * Cycles are broken by an identity-keyed visited-set. Recursion is
 * bounded by {@link #MAX_DEPTH}; collection iteration is bounded by
 * {@link #MAX_COLLECTION_ITER}. Exceeding either limit raises
 * {@link SafeCheckValidationException}.
 *
 * <h3>Empty {@code @SafeCheck} fallback</h3>
 * A field annotated with bare {@code @SafeCheck} (no SafeType
 * specified) is treated as {@link SafeType#SIMPLE_TEXT} and a
 * one-time warning is logged naming the field. This is a transitional
 * compatibility shim; every shipped annotation should declare a type
 * explicitly.
 */
public class SafeCheckValidator
{
    private static final Logger logger = LogManager.getLogger(SafeCheckValidator.class);

    /** Prefix used to identify JDK classes that should not be reflectively scanned. */
    private static final String JAVA = "java.";

    /**
     * Maximum recursion depth into the object graph. Real settings
     * graphs in this codebase reach depth ~3-7; 64 leaves a comfortable
     * margin while bounding pathological / cyclic inputs.
     */
    private static final int MAX_DEPTH = 64;

    /**
     * Maximum number of elements processed from any single Collection,
     * Map, or array during one validateAll call. Caps the cost of an
     * attacker-supplied multi-million-element collection.
     */
    private static final int MAX_COLLECTION_ITER = 100_000;

    /** Per-class cache of @SafeCheck and traversable field metadata. */
    private static final ConcurrentHashMap<Class<?>, ClassInfo> CACHE = new ConcurrentHashMap<>();

    /**
     * Per-class cache of subtree relevance. Stores whether a class (or any
     * class reachable through its instance fields) contains @SafeCheck
     * annotations. Classes cached as false are skipped entirely by
     * validateAll(), avoiding unnecessary traversal of object graphs that
     * have no fields to validate.
     */
    private static final ConcurrentHashMap<Class<?>, Boolean> RELEVANCE_CACHE = new ConcurrentHashMap<>();

    /**
     * Tracks fields for which we have already logged the
     * "empty @SafeCheck() - falling back to SIMPLE_TEXT" warning.
     * Prevents log spam.
     */
    private static final Set<String> WARNED_EMPTY_VALUE = ConcurrentHashMap.newKeySet();

    /**
     * Cached reflection info for a class.
     */
    private static class ClassInfo
    {
        final List<Field> safeCheckFields;
        final List<Field> traversableFields;

        /**
         * Stores the pre-computed lists of fields used by the validator.
         *
         * @param safeCheckFields   String fields directly carrying {@code @SafeCheck}.
         * @param traversableFields fields the validator must recurse into to reach
         *                          further {@code @SafeCheck} annotations.
         */
        ClassInfo(List<Field> safeCheckFields, List<Field> traversableFields)
        {
            this.safeCheckFields = safeCheckFields;
            this.traversableFields = traversableFields;
        }
    }

    /**
     * Collects all instance fields from a class and its superclass chain,
     * stopping at java.* classes. Static and synthetic fields are
     * excluded.
     *
     * @param clazz the class to inspect; may be {@code null} (returns empty list).
     * @return list of declared instance fields walking from {@code clazz} up to
     *         (but not including) the first {@code java.*} ancestor.
     */
    private static List<Field> getAllFields(Class<?> clazz)
    {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && !clazz.getName().startsWith(JAVA)) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (field.isSynthetic()) continue;
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Determines whether validating an instance of {@code clazz} could
     * encounter any {@code @SafeCheck} field directly or via a reachable
     * field. Cached. Conservatively returns {@code true} on cycle or on
     * unknown shapes (raw collections, type variables) so traversal is
     * never wrongly skipped.
     *
     * @param clazz the class to assess for relevance.
     * @return {@code true} if the class (or any class reachable through its
     *         instance fields) contains a {@code @SafeCheck} annotation;
     *         {@code false} if the subtree is provably free of annotations.
     */
    private static boolean isRelevant(Class<?> clazz)
    {
        Boolean cached = RELEVANCE_CACHE.get(clazz);
        if (cached != null) return cached;
        Set<Class<?>> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean rel = computeRelevance(clazz, visiting);
        RELEVANCE_CACHE.putIfAbsent(clazz, rel);
        return rel;
    }

    /**
     * Recursive worker for {@link #isRelevant(Class)}. Walks the field graph
     * tracking the set of classes currently being analyzed to break cycles.
     *
     * @param clazz    the class being analyzed.
     * @param visiting identity-keyed set of classes currently on the recursion
     *                 stack; used to detect cycles.
     * @return {@code true} if the class is relevant for {@code @SafeCheck}
     *         traversal; {@code false} otherwise.
     */
    private static boolean computeRelevance(Class<?> clazz, Set<Class<?>> visiting)
    {
        if (clazz == null || clazz.isPrimitive() || clazz.getName().startsWith(JAVA)) {
            return false;
        }
        Boolean cached = RELEVANCE_CACHE.get(clazz);
        if (cached != null) return cached;
        if (!visiting.add(clazz)) {
            // Cycle hit. Return conservative true so the traversal does
            // not silently skip a cyclic graph. Do NOT cache here - the
            // result is not yet stable.
            return true;
        }

        try {
            for (Field field : getAllFields(clazz)) {
                if (field.isAnnotationPresent(SafeCheck.class) && field.getType() == String.class) {
                    return true;
                }

                Class<?> fieldType = field.getType();
                if (fieldType.isPrimitive()) continue;

                if (fieldType.isArray()) {
                    Class<?> component = fieldType.getComponentType();
                    if (component != null && !component.isPrimitive()
                        && !component.getName().startsWith(JAVA)
                        && computeRelevance(component, visiting)) {
                        return true;
                    }
                    continue;
                }

                if (Collection.class.isAssignableFrom(fieldType)
                    || Map.class.isAssignableFrom(fieldType)) {
                    Type generic = field.getGenericType();
                    if (generic instanceof ParameterizedType) {
                        for (Type arg : ((ParameterizedType) generic).getActualTypeArguments()) {
                            if (isTypeRelevant(arg, visiting)) {
                                return true;
                            }
                        }
                    } else {
                        // Raw Collection/Map -  element type is invisible.
                        // Treat conservatively as relevant; warn once.
                        logger.warn("@SafeCheck-aware traversal: raw {} field {}.{} - declare generic type",
                            fieldType.getSimpleName(),
                            clazz.getName(), field.getName());
                        return true;
                    }
                    continue;
                }

                if (!fieldType.getName().startsWith(JAVA) && computeRelevance(fieldType, visiting)) {
                    return true;
                }
            }
            return false;
        } finally {
            visiting.remove(clazz);
        }
    }

    /**
     * Recursively check a generic Type (Class, ParameterizedType,
     * WildcardType, GenericArrayType, TypeVariable) for relevance.
     * Conservative - anything not analyzable is treated as relevant.
     *
     * @param type     the generic Type to inspect.
     * @param visiting identity-keyed set of classes currently on the recursion
     *                 stack; used to detect cycles.
     * @return {@code true} if the type or any contained type argument is
     *         relevant for {@code @SafeCheck} traversal; {@code false} otherwise.
     */
    private static boolean isTypeRelevant(Type type, Set<Class<?>> visiting)
    {
        if (type instanceof Class<?>) {
            return computeRelevance((Class<?>) type, visiting);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type raw = pt.getRawType();
            if (raw instanceof Class<?>
                && computeRelevance((Class<?>) raw, visiting)) {
                return true;
            }
            for (Type arg : pt.getActualTypeArguments()) {
                if (isTypeRelevant(arg, visiting)) return true;
            }
            return false;
        }
        // WildcardType / TypeVariable / GenericArrayType - assume relevant.
        return true;
    }

    /**
     * Builds and caches reflection metadata for a class.
     *
     * @param clazz the class whose metadata is required.
     * @return the (possibly cached) {@link ClassInfo} describing the class's
     *         {@code @SafeCheck} fields and traversable child fields.
     */
    private static ClassInfo getClassInfo(Class<?> clazz)
    {
        return CACHE.computeIfAbsent(clazz, c -> {
            List<Field> safeCheck = new ArrayList<>();
            List<Field> traversable = new ArrayList<>();

            for (Field field : getAllFields(c)) {
                if (field.isAnnotationPresent(SafeCheck.class)) {
                    if (field.getType() == String.class) {
                        field.setAccessible(true);
                        safeCheck.add(field);
                    }
                    // Non-String @SafeCheck fields are silently ignored
                    // (matches the prior validator's behavior).
                    continue;
                }

                Class<?> ft = field.getType();
                if (ft.isPrimitive()) continue;

                // Recurse into arrays, Collections, Maps, and user
                // objects (anything not in java.*).
                if (ft.isArray() || Collection.class.isAssignableFrom(ft)
                    || Map.class.isAssignableFrom(ft)
                    || !ft.getName().startsWith(JAVA)) {
                    field.setAccessible(true);
                    traversable.add(field);
                }
            }

            return new ClassInfo(
                safeCheck.isEmpty() ? Collections.emptyList() : safeCheck,
                traversable.isEmpty() ? Collections.emptyList() : traversable);
        });
    }

    /**
     * Validate a single String value against an explicit SafeType list.
     * Returns silently on success; throws
     * {@link SafeCheckValidationException} on failure.
     *
     * <p>This is the entry point for the {@link SafeCheckParam}
     * parameter-path used by the Jabsorb {@code preInvokeCallback}.
     * It is a sibling to {@link #validateValue(String, SafeCheck, String)}
     * (the field-path) - the two share identical OR-semantics and
     * error-formatting rules, but stay independent so a change to one
     * cannot regress the other.</p>
     *
     * <p>OR-semantics: the value is accepted if at least one listed
     * type accepts it. The offending value is never echoed in the
     * error message (avoids credential leakage and pivot through the
     * error channel).</p>
     *
     * @param value           the value being checked (may be null - accepted by every type)
     * @param types           the SafeType list to check against; null or empty falls back
     *                        to {@link SafeType#SIMPLE_TEXT} with a one-time warning
     * @param overrideMessage optional override for the per-type default error message;
     *                        empty/null means concatenate the type defaults with " OR "
     * @param contextLabel    short identifier for what is being validated
     *                        (e.g. {@code "ClassName.method(arg 2)"}) used in the error
     */
    public static void validate(String value, SafeType[] types, String overrideMessage, String contextLabel)
    {
        if (types == null || types.length == 0) {
            if (WARNED_EMPTY_VALUE.add("param:" + contextLabel)) {
                logger.warn("@SafeCheckParam at {} has empty value() - falling back to SafeType.SIMPLE_TEXT. "
                    + "Add an explicit SafeType to silence this warning.",
                    contextLabel);
            }
            types = new SafeType[]{SafeType.SIMPLE_TEXT};
        }

        for (SafeType type : types) {
            if (type.validate(value)) return;
        }

        String message;
        if (overrideMessage != null && !overrideMessage.isEmpty()) {
            message = overrideMessage;
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < types.length; i++) {
                if (i > 0) sb.append(" OR ");
                sb.append(types[i].defaultMessage());
            }
            message = sb.toString();
        }
        throw new SafeCheckValidationException(
            "Invalid value in " + contextLabel + ": " + message);
    }

    /**
     * Validate a single value against the SafeType list of an
     * annotation. Returns silently on success; throws
     * {@link SafeCheckValidationException} on failure.
     *
     * @param value     the field value being checked (may be null)
     * @param ann       the SafeCheck annotation on the field
     * @param fieldName "ClassSimpleName.fieldName" for error reporting
     */
    private static void validateValue(String value, SafeCheck ann, String fieldName)
    {
        SafeType[] types = ann.value();
        if (types.length == 0) {
            // Transitional fallback for un-typed @SafeCheck. Log once
            // per field so the developer notices.
            if (WARNED_EMPTY_VALUE.add(fieldName)) {
                logger.warn("@SafeCheck on field {} has empty value() - falling back to SafeType.SIMPLE_TEXT. "
                    + "Add an explicit SafeType to silence this warning.",
                    fieldName);
            }
            types = new SafeType[]{SafeType.SIMPLE_TEXT};
        }

        validateInternal(value, types, fieldName, ann.errorMessage());
    }

    /**
     * Validate a single value against an explicit SafeType list. Returns
     * silently on success; throws {@link SafeCheckValidationException} on
     * failure.
     *
     * <p>Use this from non-annotation call sites such as servlet boundaries
     * that validate request parameters directly. For SafeCheck-driven
     * validation use {@link #validateAll(Object)}.</p>
     *
     * <p>Caller contract: types must be non-null and non-empty. A null or
     * empty types argument is treated as a caller bug and surfaces an
     * unchecked exception; this is intentionally fail-fast.</p>
     *
     * @param value     the value being checked (may be null - all types accept null)
     * @param types     the allowed SafeTypes; must be non-null and non-empty
     * @param fieldName identifier shown in the error message (e.g.
     *                  "HandlerClass.args[key]"); never include the offending
     *                  value here
     */
    public static void validate(String value, SafeType[] types, String fieldName)
    {
        if (types == null) {
            throw new NullPointerException("SafeType[] must not be null");
        }
        if (types.length == 0) {
            throw new IllegalArgumentException("SafeType[] must not be empty");
        }
        validateInternal(value, types, fieldName, null);
    }

    /**
     * Core validation: try each {@link SafeType} in turn; throw with a
     * descriptive message (override or built from defaults) if none accept.
     *
     * @param value           the value being checked
     * @param types           the allowed SafeTypes (caller-validated non-empty)
     * @param fieldName       identifier shown in the error message
     * @param overrideMessage optional message replacing the default; pass
     *                        {@code null} (or empty) to build one from
     *                        {@link SafeType#defaultMessage()}
     */
    private static void validateInternal(String value, SafeType[] types, String fieldName, String overrideMessage)
    {
        for (SafeType type : types) {
            if (type.validate(value)) return;
        }

        // No type accepted. Build the message - the offending value is
        // never included (avoids credential leakage and pivot through
        // error-channel echo).
        String message;
        if (overrideMessage != null && !overrideMessage.isEmpty()) {
            message = overrideMessage;
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < types.length; i++) {
                if (i > 0) sb.append(" OR ");
                sb.append(types[i].defaultMessage());
            }
            message = sb.toString();
        }
        throw new SafeCheckValidationException(
            "Invalid value in field " + fieldName + ": " + message);
    }

    /**
     * Top-level entry: validate every {@code @SafeCheck} field reachable
     * from {@code obj}. Throws {@link SafeCheckValidationException} on
     * the first failing field. Caller (Jabsorb preInvokeCallback or App
     * setSettings method) propagates the exception.
     *
     * @param obj the root object whose reachable {@code @SafeCheck} fields
     *            should be validated; {@code null} is a no-op.
     */
    public static void validateAll(Object obj)
    {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        validateAll(obj, visited, 0);
    }

    /**
     * Recursive worker for {@link #validateAll(Object)}. Walks Collections,
     * Maps, arrays, and user objects, validating any {@code @SafeCheck}
     * field encountered. Bounded by {@link #MAX_DEPTH} and
     * {@link #MAX_COLLECTION_ITER}; cycle-safe via the {@code visited} set.
     *
     * @param obj     the current object in the traversal; may be {@code null}.
     * @param visited identity-keyed set of objects already validated; used to
     *                break reference cycles.
     * @param depth   current recursion depth; used to enforce {@link #MAX_DEPTH}.
     */
    private static void validateAll(Object obj, Set<Object> visited, int depth)
    {
        if (obj == null) return;
        if (depth > MAX_DEPTH) {
            throw new SafeCheckValidationException(
                "Validation aborted: object graph exceeds maximum depth of " + MAX_DEPTH);
        }
        if (!visited.add(obj)) return;

        if (obj instanceof Collection) {
            int n = 0;
            for (Object item : (Collection<?>) obj) {
                if (++n > MAX_COLLECTION_ITER) {
                    throw new SafeCheckValidationException(
                        "Validation aborted: collection exceeds maximum size of " + MAX_COLLECTION_ITER);
                }
                validateAll(item, visited, depth + 1);
            }
            return;
        }
        if (obj instanceof Map) {
            int n = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (++n > MAX_COLLECTION_ITER) {
                    throw new SafeCheckValidationException(
                        "Validation aborted: map exceeds maximum size of " + MAX_COLLECTION_ITER);
                }
                validateAll(entry.getKey(), visited, depth + 1);
                validateAll(entry.getValue(), visited, depth + 1);
            }
            return;
        }
        if (obj.getClass().isArray()) {
            // Skip primitive arrays - they cannot carry @SafeCheck.
            if (obj.getClass().getComponentType().isPrimitive()) return;
            int len = Array.getLength(obj);
            if (len > MAX_COLLECTION_ITER) {
                throw new SafeCheckValidationException(
                    "Validation aborted: array exceeds maximum size of " + MAX_COLLECTION_ITER);
            }
            for (int i = 0; i < len; i++) {
                validateAll(Array.get(obj, i), visited, depth + 1);
            }
            return;
        }
        if (obj.getClass().getName().startsWith(JAVA)) return;

        // Skip subtrees we know contain no @SafeCheck.
        if (!isRelevant(obj.getClass())) return;

        ClassInfo info = getClassInfo(obj.getClass());

        try {
            for (Field field : info.safeCheckFields) {
                String value = (String) field.get(obj);
                SafeCheck ann = field.getAnnotation(SafeCheck.class);
                String fieldName = obj.getClass().getSimpleName() + "." + field.getName();
                validateValue(value, ann, fieldName);
            }
            for (Field field : info.traversableFields) {
                Object child = field.get(obj);
                validateAll(child, visited, depth + 1);
            }
        } catch (IllegalAccessException e) {
            throw new SafeCheckValidationException(
                "Failed to access field on " + obj.getClass().getName() + ": " + e.getMessage());
        }
    }
}
