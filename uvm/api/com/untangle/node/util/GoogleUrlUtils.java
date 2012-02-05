/*
 * $Id: GoogleUrlUtils.java,v 1.00 2011/07/21 15:28:13 dmorris Exp $
 */
package com.untangle.node.util;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * This code came from jgooglesafebrowser
 * http://code.google.com/p/jgooglesafebrowsing/source/browse/trunk/v2/src/main/java/com/buildabrand/gsb/util/URLUtils.java?r=22
 */

/**
 *
 * <h4>Copyright and License</h4>
 * This code is copyright (c) McCann Erickson Advertising Ltd, 2008 except where
 * otherwise stated. It is released as
 * open-source under the Creative Commons NC-SA license. See
 * <a href="http://creativecommons.org/licenses/by-nc-sa/2.5/">http://creativecommons.org/licenses/by-nc-sa/2.5/</a>
 * for license details. This code comes with no warranty or support.
 *
 * @author Dave Shanley <david.shanley@europe.mccann.com> & Henrik Sjostrand, Netvouz, http://www.netvouz.com/, info@netvouz.com
 */
public class GoogleUrlUtils
{
	
	private static final String FILL = ""; // The object we insert into the hashmaps
	private static final String ENCODING = "UTF-8";
	private static char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	private static final int minLineLength = 33; // A valid line contains at least 33 characters (1 operator, 32 MD5 hash)

	/** Unscapes a string repeatedly to remove all escaped characters. Returns null if the string is invalid.
	 * @author Henrik Sjostrand, Netvouz, http://www.netvouz.com/, info@netvouz.com
	 * @param url
	 * @return
	 * @throws Exception
	 */
	private static String unescape(String url) throws Exception {
		if (url == null)
			return null;

		// Because URLDecoder chokes on some invalid URLs (those containing a percent sign not 
		// followed by a hex value) we need to check we're not processing such a URL.
		StringBuffer text1 = new StringBuffer(url);
		int p = 0;
		while ((p = text1.indexOf("%", p)) != -1) {
			char c1 = ' ';
			char c2 = ' ';
			if (++p <= text1.length() - 2) {
				c1 = text1.charAt(p);
				c2 = text1.charAt(p + 1);
			}
			// If the percent sign is not followed by a two-digit hex value it's an invalid URL
			if (!(((c1 >= '0' && c1 <= '9') || (c1 >= 'a' && c1 <= 'f') || (c1 >= 'A' && c1 <= 'F')) && ((c2 >= '0' && c2 <= '9') || (c2 >= 'a' && c2 <= 'f') || (c2 >= 'A' && c2 <= 'F'))))
				return null;
		}

		String text2 = url;
		try {
			while (text2.indexOf("%") != -1)
				text2 = URLDecoder.decode(text2, ENCODING); // Unescape repeatedly until no more percent signs left
		} catch (UnsupportedEncodingException e) {
			throw new Exception("unescapeUTF8: Could not decode URL " + url + ". ErrMsg=" + e.toString());
		}
		return text2;
	}
	
	/** Returns the canonicalized form of a URL, core logic written by Henrik Sjostrand
	 * @author Henrik Sjostrand, Netvouz, http://www.netvouz.com/, info@netvouz.com
	 * @param queryURL
	 * @return
	 * @throws Exception
	 */
	
	public static String canonicalizeURL(String queryURL) throws Exception{
		if (queryURL == null)
			return null;

		String url = queryURL;
		//System.out.println("Original       : " + url);

		try {
			// Create a URL object and extract the fields we need
			URL theURL = new URL(url);
			String host = theURL.getHost();
			String path = theURL.getPath();
			String query = theURL.getQuery();
			String protocol = theURL.getProtocol();
			int port = theURL.getPort();
			String user = theURL.getUserInfo();

			//
			// 2. Process the hostname
			// 
			// 2a. Unescape until no more hex-encodings
			host = unescape(host);

			// 2b. Lower-case
			host = host.toLowerCase();

			// 2b. Escape non-standard characters (escape once).
			// Note: When escaping the hostname we have less characters allowed unescaped
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < host.length(); i++) {
				char c = host.charAt(i);
				if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || c == '.' || c == '-')
					sb.append(c);
				else
					sb.append(URLEncoder.encode(c + "", ENCODING)); // Escape using UTF-8
			}
			host = sb.toString();

			// 2c. Remove leading and trailing dots 
			while (host.startsWith("."))
				host = host.substring(1);
			while (host.endsWith("."))
				host = host.substring(0, host.length() - 1);

			// 2d. Replace consecutive dots with a single dot
			int p = 0;
			while ((p = host.indexOf("..")) != -1)
				host = host.substring(0, p + 1) + host.substring(p + 2);

			// 2e. Skip the IP address parsing of hostname

			// 2f. Add trailing slash if path is empty
			if ("".equals(path))
				host = host + "/";

			//
			// Process the path
			//
			// 3a. Unescape until no more hex-encodings
			path = unescape(path);

			// 3b. Remove consecutive slashes from path
			while ((p = path.indexOf("//")) != -1)
				path = path.substring(0, p + 1) + path.substring(p + 2);

			// 3b. Remove /./ occurences from path
			while ((p = path.indexOf("/./")) != -1)
				path = path.substring(0, p + 1) + path.substring(p + 3);

			// 3c. Resolve /../ occurences in path
			while ((p = path.indexOf("/../")) != -1) {
				int previousSlash = path.lastIndexOf("/", p-1);
				// if (previousSlash == -1) previousSlash = 0; // If path begins with /../
				path = path.substring(0, previousSlash) + path.substring(p + 3);
				p = previousSlash;
			}

			// 3d. Escape once
			path = escape(path);

			// 
			// Process the query
			//
			// 4a. Unescape until no more hex-encodings
			query = unescape(query);

			// 4b. Escape once
			query = escape(query);

			//
			// Rebuild the URL
			//
			sb.setLength(0);
			sb.append(protocol + ":");
			if (port != -1)
				sb.append(port);
			sb.append("//");
			if (user != null)
				sb.append(user + "@");
			sb.append(host);
			sb.append(path);
			if (query != null)
				sb.append("?" + query);

			url = sb.toString();

		} catch (Exception e) {
			throw new Exception("Could not canonicalise URL: " + queryURL, e);
		}

		return url;
	}
	
	/** Escapes a string by replacing characters having ASCII <=32, >=127, or % with their UTF-8-escaped codes 
	 * 
	 * @param url
	 * @return escaped url
	 * @author Henrik Sjostrand, Netvouz, http://www.netvouz.com/, info@netvouz.com
	 */
	private static String escape(String url) throws Exception {
		if (url == null)
			return null;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < url.length(); i++) {
			char c = url.charAt(i);
			if (c == ' ')
				sb.append("%20");
			else if (c <= 32 || c >= 127 || c == '%') {
				try {
					sb.append(URLEncoder.encode("" + c, ENCODING));
				} catch (UnsupportedEncodingException e) {
					throw new Exception("escapeUTF8: Could not encode character " + c + " at position " + i + " in " + url + ". ErrMsg=" + e.toString());
				}
			} else
				sb.append(c);
		}
		return sb.toString();
	}
	
	/** Parses the query URL into several different url strings, each of which should be looked up 
	 * @author Henrik Sjostrand, Netvouz, http://www.netvouz.com/, info@netvouz.com
	 * @param queryURL
	 * @return
	 */
	public static ArrayList<String> getLookupURLs(String queryURL) throws Exception {
		ArrayList<String> urls = new ArrayList<String>();
		if (queryURL != null) {
			try {
				String canonicalizedURL = canonicalizeURL(queryURL);
				if (canonicalizedURL != null) { // canonicalizeURL & unescapeUTF8 returns null on invalid strings, and then we don't add any URLs to the lookup list which means we consider this URL safe
					URL url = new URL(canonicalizedURL);
					String host = url.getHost();
					String path = url.getPath();
					String query = url.getQuery();
					if (query != null)
						query = "?" + query;

					// Generate a list of the hosts to test (exact hostname plus up to four truncated hostnames) 
					ArrayList<String> hosts = new ArrayList<String>();
					hosts.add(host); // Should always test the exact hostname
					String[] hostArray = host.split("\\.");
					StringBuffer sb = new StringBuffer();
					int start = (hostArray.length < 6 ? 1 : hostArray.length - 5);
					int stop = hostArray.length;
					for (int i = start; i < stop - 1; i++) {
						sb.setLength(0);
						for (int j = i; j < stop; j++)
							sb.append(hostArray[j] + ".");
						sb.setLength(sb.length() - 1); // Trim trailing dot
						hosts.add(sb.toString());
					}

					// Generate a list of paths to test
					ArrayList<String> paths = new ArrayList<String>();
					if (query != null)
						paths.add(path + query); // exact path including query
					paths.add(path); // exact path excluding query
					if (!paths.contains("/"))
						paths.add("/");

					int maxCount = (query == null ? 5 : 6);
					String pathElement = "/";
					StringTokenizer st = new StringTokenizer(path, "/");
					while (st.hasMoreTokens() && paths.size() < maxCount) {
						String thisToken = st.nextToken();
						pathElement = pathElement + thisToken + (thisToken.indexOf(".") == -1 ? "/" : "");
						if (!paths.contains(pathElement))
							paths.add(pathElement);
					}

					for (int i = 0; i < hosts.size(); i++) {
						for (int j = 0; j < paths.size(); j++)
							urls.add(new String(hosts.get(i).toString() + paths.get(j).toString()));
					}

				}
			} catch (Exception e) {
				throw new Exception("Could not generate lookup URLs");
			}
		}
		return urls;
	}
	
}
