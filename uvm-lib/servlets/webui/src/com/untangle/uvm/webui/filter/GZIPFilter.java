package com.untangle.uvm.webui.filter;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet filter for compressing output to the Web browser if supported.
 */
public class GZIPFilter implements Filter {
	private static final String HAS_RUN_KEY = GZIPFilter.class.getName()
			+ ".HAS_RUN";

	/** @see Filter * */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		boolean compress = false;
		if (request.getAttribute(HAS_RUN_KEY) == null
				&& request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			Enumeration headers = httpRequest.getHeaders("Accept-Encoding");
			while (headers.hasMoreElements()) {
				String value = (String) headers.nextElement();
				if (value.indexOf("gzip") != -1) {
					compress = true;
				}
			}
		}
		request.setAttribute(HAS_RUN_KEY, "true");
		if (compress) {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			httpResponse.addHeader("Content-Encoding", "gzip");
			GZIPResponseWrapper compressionResponse = new GZIPResponseWrapper(
					httpResponse);
			chain.doFilter(request, compressionResponse);
			compressionResponse.finish();
		} else {
			chain.doFilter(request, response);
		}
	}

	/** @see Filter * */
	public void init(FilterConfig config) throws ServletException {
	}

	/** @see Filter * */
	public void destroy() {
	}
}
