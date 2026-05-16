/**
 * $Id$
 */
package com.untangle.uvm.servlet;

import org.apache.commons.fileupload.FileItem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.util.SafeType;

public interface UploadHandler
{
    public String getName();

    public Object handleFile(FileItem fileItem, String argument) throws Exception;

    default Object handleV2File(byte[] backupFileBytes, Map<String, String> arguments) throws Exception {
        return null;
    }

    /**
     * v1 file-extension allowlist (lowercase, no leading dot - e.g.
     * {@code Set.of("zip", "conf")}). The empty default means no extension
     * is exposed to the handler: {@code FileItem.getName()} returns
     * {@code "upload"}.
     *
     * <p>The servlet-side wrapper applies a defensive lowercase to this set,
     * so case mismatches do not break legitimate uploads, but follow the
     * lowercase convention to avoid confusion.</p>
     *
     * <p>Used by {@code SafeUpload.safeUploadName} at the servlet boundary.</p>
     *
     * @return the set of allowed file extensions, or an empty set if no
     *         extension should be exposed to the handler
     */
    default Set<String> getAllowedExtensions() { return Collections.emptySet(); }

    /**
     * v2 args schema: arg name to {@link SafeType}[]. Keys not in this map
     * are REJECTED at the v2 servlet boundary. Empty map (the default)
     * means the handler accepts no v2 args - every non-reserved form field
     * is rejected.
     *
     * <p>The {@code "type"} form field is reserved (it is the dispatch key)
     * and is filtered out before the schema check, so handlers must not
     * declare it.</p>
     *
     * <p>Each value is validated against its declared SafeType list using
     * {@link com.untangle.uvm.util.SafeCheckValidator#validate(String,
     * SafeType[], String)}; the first matching type accepts the value.</p>
     *
     * @return the per-key SafeType allowlist, or empty for "no v2 args accepted"
     */
    default Map<String, SafeType[]> getArgumentTypes() { return Collections.emptyMap(); }
}
