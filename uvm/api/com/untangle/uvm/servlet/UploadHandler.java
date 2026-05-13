/**
 * $Id$
 */
package com.untangle.uvm.servlet;

import org.apache.commons.fileupload.FileItem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

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
}
