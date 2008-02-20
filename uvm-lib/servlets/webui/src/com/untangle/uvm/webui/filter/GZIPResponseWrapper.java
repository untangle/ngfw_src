package com.untangle.uvm.webui.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Compresses a {@link HttpServletResponse} using GZIP compression.
 */
public class GZIPResponseWrapper extends HttpServletResponseWrapper {
	private ServletOutputStream out;
	private GZIPResponseStream compressedOut;
	private PrintWriter writer;

	/**
	 * Creates a new compressed response wrapping the specified HTTP response.
	 * 
	 * @param response
	 *            the HTTP response to wrap.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public GZIPResponseWrapper(HttpServletResponse response) throws IOException {
		super(response);
		compressedOut = new GZIPResponseStream(response.getOutputStream());
	}

	/**
	 * Ignore attempts to set the content length since the actual content length
	 * will be determined by the GZIP compression.
	 * 
	 * @param len
	 *            the content length
	 */
	public void setContentLength(int len) {
	}

	/** @see HttpServletResponse * */
	public ServletOutputStream getOutputStream() throws IOException {
		if (null == out) {
			if (null != writer) {
				throw new IllegalStateException("getWriter() has already been "
						+ "called on this response.");
			}
			out = compressedOut;
		}
		return out;
	}

	/** @see HttpServletResponse * */
	public PrintWriter getWriter() throws IOException {
		if (null == writer) {
			if (null != out) {
				throw new IllegalStateException("getOutputStream() has "
						+ "already been called on this response.");
			}
			writer = new PrintWriter(compressedOut);
		}
		return writer;
	}

	/** @see HttpServletResponse * */
	public void flushBuffer() {
		try {
			if (writer != null) {
				writer.flush();
			} else if (out != null) {
				out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** @see HttpServletResponse * */
	public void reset() {
		super.reset();
		try {
			compressedOut.reset();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** @see HttpServletResponse * */
	public void resetBuffer() {
		super.resetBuffer();
		try {
			compressedOut.reset();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Finishes writing the compressed data to the output stream. Note: this
	 * closes the underlying output stream.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public void finish() throws IOException {
		compressedOut.close();
	}
}
