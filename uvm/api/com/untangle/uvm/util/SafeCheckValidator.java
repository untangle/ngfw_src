/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.lang.reflect.Field;
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
    private static final String JAVA = "java.";
    private static final ConcurrentHashMap<Class<?>, ClassInfo> CACHE = new ConcurrentHashMap<>();

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

        /**
         * Returns true if this class has no annotated fields and no traversable fields.
         *
         * @return true if scanning this class can be skipped
         */
        boolean isEmpty()
        {
            return safeCheckFields.isEmpty() && traversableFields.isEmpty();
        }
    }

    /**
     * Builds and caches reflection info for a class.
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

            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(SafeCheck.class) && field.getType() == String.class) {
                    field.setAccessible(true);
                    safeCheck.add(field);
                } else if (!field.getType().isPrimitive() && !field.getType().getName().startsWith(JAVA)) {
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
     *
     * @param obj
     *        The object to sanitize
     * @param visited
     *        Set of already visited objects to prevent infinite recursion
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

        ClassInfo info = getClassInfo(obj.getClass());
        if (info.isEmpty()) {
            return;
        }

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
