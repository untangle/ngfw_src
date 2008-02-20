package com.untangle.uvm.webui.filter;

import java.io.IOException;

import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;

/**
 * Compressed implementation of {@link ServletOutputStream} using GZIP.
 */
public class GZIPResponseStream extends ServletOutputStream {
	private ServletOutputStream out;
	private GZIPOutputStream gzip;

	/**
	 * Compresses the specified stream using the specified initial buffer.
	 * 
	 * @param out
	 *            the stream to compress.
	 * @throws IOException
	 *             if an error occurs with the {@link GZIPOutputStream}.
	 */
	public GZIPResponseStream(ServletOutputStream out) throws IOException {
		this.out = out;
		reset();
	}

	/** @see ServletOutputStream * */
	public void close() throws IOException {
		gzip.close();
	}

	/** @see ServletOutputStream * */
	public void flush() throws IOException {
		gzip.flush();
	}

	/** @see ServletOutputStream * */
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	/** @see ServletOutputStream * */
	public void write(byte[] b, int off, int len) throws IOException {
		gzip.write(b, off, len);
	}

	/** @see ServletOutputStream * */
	public void write(int b) throws IOException {
		gzip.write(b);
	}

	/**
	 * Resets the stream.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public void reset() throws IOException {
		gzip = new GZIPOutputStream(out);
	}
}
