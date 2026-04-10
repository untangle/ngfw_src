/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Sanitizes string values for shell-safety.
 * Detects actual command injection patterns rather than blindly
 * stripping individual characters. This preserves legitimate uses
 * of special characters (e.g. "$" in passwords, "|" in descriptions)
 * while catching injection constructs like `cmd`, $(cmd), ${var},
 * command chaining (;, &&, ||), pipes, redirects, and newline injection.
 */
public class SafeCheckValidator
{
    /**
     * Matches command injection constructs:
     *
     * Enclosed patterns (entire construct stripped):
     *   `command`    backtick command substitution
     *   $(command)   subshell command substitution
     *   ${variable}  variable/command expansion
     *
     * Chaining operators (stripped when followed by command-like content):
     *   &&           AND chaining
     *   ||           OR chaining
     *   ;            command separator
     *   |            pipe to command
     *
     * Redirect operators (stripped when followed by path-like content):
     *   > or >>      output redirect
     *   <            input redirect / process substitution
     *
     * Always stripped:
     *   \n \r        newline injection (acts as command separator)
     */
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
        String.join("|",
            "`[^`]+`",                              // `command`  backtick substitution
            "\\$\\([^)]*\\)",                        // $(command) subshell substitution
            "\\$\\{[^}]*\\}",                        // ${var}     variable expansion
            "&&(?=\\s*\\S)",                          // &&         AND chaining
            "\\|\\|(?=\\s*\\S)",                      // ||         OR chaining
            "[\\r\\n]",                               // \\n \\r    newline injection
            ";(?=\\s*[a-zA-Z0-9_/\\\\.])",            // ;          command separator
            "\\|(?=\\s*[a-zA-Z0-9_/\\\\.])",          // |          pipe to command
            ">{1,2}(?=\\s*[a-zA-Z0-9_/\\\\.])",      // > >>       output redirect
            "<(?=\\s*[a-zA-Z0-9_/(\\\\.])"            // <          input redirect / <()
        )
    );
    /** Prefix used to identify JDK classes that should not be reflectively scanned. */
    private static final String JAVA = "java.";

    /** Per-class cache of @SafeCheck and traversable field metadata. Populated once per class on first encounter. */
    private static final ConcurrentHashMap<Class<?>, ClassInfo> CACHE = new ConcurrentHashMap<>();

    /**
     * Per-class cache of subtree relevance. Stores whether a class (or any class
     * reachable through its instance fields) contains @SafeCheck annotations.
     * Classes cached as false are skipped entirely by validateAll(), avoiding
     * unnecessary traversal of object graphs that have no fields to sanitize.
     */
    private static final ConcurrentHashMap<Class<?>, Boolean> RELEVANCE_CACHE = new ConcurrentHashMap<>();

    /**
     * Collects all instance fields from a class and its superclass chain,
     * stopping at java.* classes. Static fields are excluded since @SafeCheck
     * applies only to per-instance settings data. Walking the superclass chain
     * ensures annotations on parent classes are not missed by getDeclaredFields().
     *
     * @param clazz
     *        The class to inspect
     * @return List of all instance fields in the class hierarchy
     */
    private static List<Field> getAllFields(Class<?> clazz)
    {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && !clazz.getName().startsWith(JAVA)) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Cached reflection info for a class.
     */
    private static class ClassInfo
    {
        final List<Field> safeCheckFields;
        final List<Field> traversableFields;

        /**
         * Constructor for ClassInfo.
         *
         * @param safeCheckFields
         *        List of fields annotated with SafeCheck
         * @param traversableFields
         *        List of non-primitive fields to recurse into
         */
        ClassInfo(List<Field> safeCheckFields, List<Field> traversableFields)
        {
            this.safeCheckFields = safeCheckFields;
            this.traversableFields = traversableFields;
        }

    }

    /**
     * Checks whether a class or any class reachable through its fields
     * has @SafeCheck annotations. Results are cached per class so the
     * reflection cost is paid only once. Classes with no @SafeCheck
     * anywhere in their subtree are skipped entirely by validateAll().
     *
     * @param clazz
     *        The class to check
     * @return true if this class or any reachable class has @SafeCheck fields
     */
    private static boolean isRelevant(Class<?> clazz)
    {
        Boolean cached = RELEVANCE_CACHE.get(clazz);
        if (cached != null) {
            return cached;
        }
        return computeRelevance(clazz, new HashSet<>());
    }

    /**
     * Recursive relevance check with cycle detection.
     *
     * @param clazz
     *        The class to check
     * @param visiting
     *        Set of classes currently being checked to prevent infinite recursion
     * @return true if @SafeCheck is found in this class or its subtree
     */
    private static boolean computeRelevance(Class<?> clazz, Set<Class<?>> visiting)
    {
        if (clazz == null || clazz.isPrimitive() || clazz.getName().startsWith(JAVA)) {
            return false;
        }
        Boolean cached = RELEVANCE_CACHE.get(clazz);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(clazz)) {
            return false;
        }

        boolean relevant = false;
        for (Field field : getAllFields(clazz)) {
            // Direct @SafeCheck field found
            if (field.isAnnotationPresent(SafeCheck.class) && field.getType() == String.class) {
                relevant = true;
                break;
            }

            Class<?> fieldType = field.getType();
            if (fieldType.isPrimitive()) {
                continue;
            }

            // Check Collection/Map generic type arguments
            if (Collection.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    for (Type typeArg : ((ParameterizedType) genericType).getActualTypeArguments()) {
                        if (typeArg instanceof Class<?> && computeRelevance((Class<?>) typeArg, visiting)) {
                            relevant = true;
                            break;
                        }
                    }
                }
                if (relevant) {
                    break;
                }
                continue;
            }

            // Check non-java custom types
            if (!fieldType.getName().startsWith(JAVA) && computeRelevance(fieldType, visiting)) {
                relevant = true;
                break;
            }
        }

        RELEVANCE_CACHE.put(clazz, relevant);
        return relevant;
    }

    /**
     * Builds and caches reflection metadata for a class. Partitions instance
     * fields into two groups:
     * <ul>
     *   <li>safeCheckFields: String fields annotated with @SafeCheck (to sanitize)</li>
     *   <li>traversableFields: non-primitive fields that may contain nested
     *       @SafeCheck objects. Includes Collection/Map fields (which hold
     *       settings objects) and non-java custom types. Excludes java.* types
     *       like String, Integer, etc. that can never have @SafeCheck.</li>
     * </ul>
     *
     * @param clazz
     *        The class to inspect
     * @return Cached ClassInfo with annotated and traversable fields
     */
    private static ClassInfo getClassInfo(Class<?> clazz)
    {
        return CACHE.computeIfAbsent(clazz, c -> {
            List<Field> safeCheck = new ArrayList<>();
            List<Field> traversable = new ArrayList<>();

            for (Field field : getAllFields(c)) {
                if (field.isAnnotationPresent(SafeCheck.class) && field.getType() == String.class) {
                    field.setAccessible(true);
                    safeCheck.add(field);
                } else if (!field.getType().isPrimitive()
                           && (!field.getType().getName().startsWith(JAVA)
                               || Collection.class.isAssignableFrom(field.getType())
                               || Map.class.isAssignableFrom(field.getType()))) {
                    field.setAccessible(true);
                    traversable.add(field);
                }
            }

            return new ClassInfo(
                safeCheck.isEmpty() ? Collections.emptyList() : safeCheck,
                traversable.isEmpty() ? Collections.emptyList() : traversable
            );
        });
    }

    /**
     * Strips shell metacharacters from the given value.
     *
     * @param value
     *        The string value to sanitize
     * @return The sanitized string with unsafe characters removed, or null if input is null
     */
    public static String sanitize(String value)
    {
        if (value == null) {
            return null;
        }
        return INJECTION_PATTERN.matcher(value).replaceAll("");
    }

    /**
     * Scans the given object and all reachable nested objects for fields
     * annotated with @SafeCheck and sanitizes their String values.
     * Recurses into collections, maps, and object fields.
     * Classes with no @SafeCheck in their subtree are skipped entirely.
     *
     * @param obj
     *        The object to sanitize
     */
    public static void validateAll(Object obj)
    {
        validateAll(obj, new HashSet<>());
    }

    /**
     * Recursive implementation that tracks visited objects to avoid cycles.
     * Dispatches based on object type:
     * <ul>
     *   <li>Collection: iterates elements recursively</li>
     *   <li>Map: iterates values recursively</li>
     *   <li>java.* objects: skipped (no @SafeCheck possible)</li>
     *   <li>Non-relevant classes: skipped via RELEVANCE_CACHE (subtree has no @SafeCheck)</li>
     *   <li>Relevant classes: sanitizes @SafeCheck fields, then traverses into sub-objects</li>
     * </ul>
     *
     * @param obj
     *        The object to sanitize
     * @param visited
     *        Set of already visited objects (identity-based) to prevent infinite recursion
     */
    private static void validateAll(Object obj, Set<Object> visited)
    {
        if (obj == null) {
            return;
        }
        if (!visited.add(obj)) {
            return;
        }
        if (obj instanceof Collection) {
            for (Object item : (Collection<?>) obj) {
                validateAll(item, visited);
            }
            return;
        }
        if (obj instanceof Map) {
            for (Object val : ((Map<?, ?>) obj).values()) {
                validateAll(val, visited);
            }
            return;
        }
        if (obj.getClass().getName().startsWith(JAVA)) {
            return;
        }

        // Skip classes that have no @SafeCheck anywhere in their subtree
        if (!isRelevant(obj.getClass())) {
            return;
        }

        ClassInfo info = getClassInfo(obj.getClass());

        try {
            for (Field field : info.safeCheckFields) {
                String value = (String) field.get(obj);
                field.set(obj, sanitize(value));
            }
            for (Field field : info.traversableFields) {
                Object child = field.get(obj);
                validateAll(child, visited);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to sanitize object of type: " + obj.getClass().getName(), e);
        }
    }
}
