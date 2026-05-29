/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helpers for safely handling user-supplied filenames at the upload servlet
 * boundary. The design separates two concerns:
 *
 * <ul>
 *   <li><b>Untrusted external input</b> ({@link #safeUploadName}) - a
 *       user-supplied basename from a multipart Content-Disposition header.
 *       This method <i>never throws</i>; bad inputs collapse to {@code "upload"}
 *       so a legitimate upload is never blocked by an obscure parsing edge
 *       case. The security goal is canonicalization, not rejection.</li>
 *   <li><b>Trusted internal callers</b> ({@link #resolveSafe}) - a path
 *       resolution helper for ZIP/archive loops. This method throws on null
 *       arguments and on path-escape attempts; callers must handle the
 *       exception.</li>
 * </ul>
 */
public final class SafeUpload
{
    /**
     * Private constructor - this is a static-utility class and must not be
     * instantiated.
     */
    private SafeUpload() { }

    /**
     * Sanitize a user-supplied filename to a known-safe canonical form.
     *
     * <p>Returns one of:</p>
     * <ul>
     *   <li>{@code "upload"} - if rawName is null/empty/unparseable, has no
     *       extension, has a trailing dot, or its extension is not in the
     *       allowlist.</li>
     *   <li>{@code "upload." + ext} - if the extension (lowercased) appears
     *       in {@code allowed} (also lowercased defensively).</li>
     * </ul>
     *
     * <p>The original basename is always discarded - traversal characters,
     * shell metacharacters, NUL bytes, and weird Unicode all become harmless
     * because the only byte sequences that survive are bounded by the
     * allowlist's contents.</p>
     *
     * @param rawName the user-supplied filename (may be null)
     * @param allowed the set of allowed extensions, lowercase, no leading dot
     *                (e.g. {@code Set.of("zip", "conf")}); may be null/empty
     * @return {@code "upload"} or {@code "upload.<ext>"}, never null, never the
     *         original basename
     */
    public static String safeUploadName(String rawName, Set<String> allowed)
    {
        if (rawName == null || allowed == null || allowed.isEmpty()) return "upload";

        // Defensive lowercase of the allowlist - prevents handler authors from
        // breaking legitimate uploads by returning Set.of("ZIP") instead of
        // Set.of("zip"). Also filters null entries so a Set.of("zip", null)
        // does not NPE.
        Set<String> allowedLower = allowed.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (allowedLower.isEmpty()) return "upload";

        try {
            Path nameOnly = Paths.get(rawName).getFileName();
            if (nameOnly == null) return "upload";
            String base = nameOnly.toString().toLowerCase(Locale.ROOT);
            int dot = base.lastIndexOf('.');
            if (dot < 0 || dot == base.length() - 1) return "upload";
            String ext = base.substring(dot + 1);
            return allowedLower.contains(ext) ? "upload." + ext : "upload";
        } catch (InvalidPathException e) {
            return "upload";
        }
    }

    /**
     * Resolve {@code entryName} under {@code root} and assert the result stays
     * within {@code root}. For use in ZIP/archive extraction loops.
     *
     * <p>Note on TOCTOU: {@code getCanonicalFile()} resolves symlinks at check
     * time. An attacker who can create or replace symlinks between this call
     * and the eventual file open could escape. Callers that handle adversarial
     * archive content should also use {@code O_NOFOLLOW}-equivalent semantics
     * at file-open time.</p>
     *
     * @param root      the directory the resolved path must stay within;
     *                  must not be null
     * @param entryName the relative entry name to resolve under {@code root};
     *                  must not be null
     * @return a canonical {@link File} that is guaranteed to be at or below
     *         {@code root}
     * @throws IOException if either argument is null, or if the resolved path
     *                     would escape {@code root}
     */
    public static File resolveSafe(File root, String entryName) throws IOException
    {
        if (root == null || entryName == null) {
            throw new IOException("Null root or entryName");
        }
        File rootCanon = root.getCanonicalFile();
        File resolved = new File(rootCanon, entryName).getCanonicalFile();
        if (!resolved.toPath().startsWith(rootCanon.toPath())) {
            throw new IOException("Path escapes root: " + entryName);
        }
        return resolved;
    }

    /**
     * Strip CR/LF from a filename for safe inclusion in single-line log
     * messages. Used at the servlet boundary so we can preserve forensic info
     * about the original (attacker-controlled) filename without exposing it
     * to handlers AND without enabling log-injection if the appender is
     * line-based.
     *
     * @param s the (possibly null) string to sanitize
     * @return {@code "<null>"} if {@code s} is null, otherwise {@code s} with
     *         every CR and LF replaced by {@code '_'}
     */
    public static String safeForLog(String s)
    {
        if (s == null) return "<null>";
        return s.replace('\r', '_').replace('\n', '_');
    }
}
