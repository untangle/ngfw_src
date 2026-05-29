/**
 * $Id$
 */
package com.untangle.uvm.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Objects;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;

/**
 * {@link FileItem} wrapper that returns a sanitized name from {@link #getName()}
 * and delegates every other method to the wrapped item.
 *
 * <p>Used at the v1 upload servlet boundary to ensure handlers never see the
 * attacker-controlled Content-Disposition filename. The override name is
 * computed by
 * {@link com.untangle.uvm.util.SafeUpload#safeUploadName(String, java.util.Set)}.</p>
 *
 * <p><b>Equality:</b> {@code equals}/{@code hashCode} are NOT overridden. Two
 * {@code SafeFileItem} instances wrapping the same underlying item compare
 * unequal. Do not place {@code FileItem} instances in {@code Set}/{@code Map}
 * structures and assume wrapper-equality semantics.</p>
 */
public class SafeFileItem implements FileItem
{
    private static final long serialVersionUID = 1L;

    private final FileItem wrapped;
    private final String safeName;

    /**
     * Construct a wrapper that returns {@code safeName} from {@link #getName()}.
     *
     * @param wrapped  the underlying FileItem; must be non-null
     * @param safeName the override returned from {@link #getName()}; must be
     *                 non-null (typically {@code "upload"} or {@code "upload.<ext>"})
     */
    public SafeFileItem(FileItem wrapped, String safeName)
    {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped FileItem must not be null");
        this.safeName = Objects.requireNonNull(safeName, "safeName must not be null");
    }

    /**
     * Return the sanitized override name instead of the user-supplied
     * Content-Disposition filename.
     *
     * @return the {@code safeName} passed to the constructor
     */
    @Override
    public String getName()
    {
        return safeName;
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return an {@link InputStream} over the uploaded bytes
     * @throws IOException if the wrapped item's stream cannot be opened
     */
    @Override
    public InputStream getInputStream() throws IOException
    {
        return wrapped.getInputStream();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return the Content-Type of the uploaded item, or null if unknown
     */
    @Override
    public String getContentType()
    {
        return wrapped.getContentType();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return true if the item's contents are held in memory
     */
    @Override
    public boolean isInMemory()
    {
        return wrapped.isInMemory();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return the size of the uploaded item, in bytes
     */
    @Override
    public long getSize()
    {
        return wrapped.getSize();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return the uploaded bytes
     */
    @Override
    public byte[] get()
    {
        return wrapped.get();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @param encoding the character encoding to use
     * @return the contents as a String decoded with {@code encoding}
     * @throws UnsupportedEncodingException if the encoding is not supported
     */
    @Override
    public String getString(String encoding) throws UnsupportedEncodingException
    {
        return wrapped.getString(encoding);
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return the contents as a String using the default encoding
     */
    @Override
    public String getString()
    {
        return wrapped.getString();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @param file the destination file
     * @throws Exception if the wrapped item cannot be written
     */
    @Override
    public void write(File file) throws Exception
    {
        wrapped.write(file);
    }

    /**
     * Delegates to the wrapped item.
     */
    @Override
    public void delete()
    {
        wrapped.delete();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return the form-field name of this item
     */
    @Override
    public String getFieldName()
    {
        return wrapped.getFieldName();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @param name the form-field name to set
     */
    @Override
    public void setFieldName(String name)
    {
        wrapped.setFieldName(name);
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return true if this item represents a simple form field (not a file)
     */
    @Override
    public boolean isFormField()
    {
        return wrapped.isFormField();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @param state true to mark this item as a simple form field
     */
    @Override
    public void setFormField(boolean state)
    {
        wrapped.setFormField(state);
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return an {@link OutputStream} for writing the item's contents
     * @throws IOException if the wrapped item's stream cannot be opened
     */
    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return wrapped.getOutputStream();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @return the headers associated with the wrapped item
     */
    @Override
    public FileItemHeaders getHeaders()
    {
        return wrapped.getHeaders();
    }

    /**
     * Delegates to the wrapped item.
     *
     * @param headers the headers to set on the wrapped item
     */
    @Override
    public void setHeaders(FileItemHeaders headers)
    {
        wrapped.setHeaders(headers);
    }
}
