/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.http;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.PipelineEndpoints;
import org.hibernate.annotations.Type;

/**
 * Holds a RFC 2616 request-line.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_http_req_line", schema="events")
    public class RequestLine implements Serializable
    {
        private static final long serialVersionUID = -2183950932382112727L;

        private Long id;
        private HttpMethod method;
        private URI requestUri;
        private PipelineEndpoints pipelineEndpoints;
        private HttpRequestEvent httpRequestEvent; // Filled in after creation time.

        // constructors -----------------------------------------------------------

        public RequestLine() { }

        public RequestLine(PipelineEndpoints pe, HttpMethod method, URI requestUri)
        {
            this.pipelineEndpoints = pe;
            this.method = method;
            this.requestUri = requestUri;
        }

        // business methods -------------------------------------------------------

        @Transient
        public URL getUrl()
        {
            // XXX this shouldn't happen in practice
            String host = null == httpRequestEvent ? ""
                : httpRequestEvent.getHost();

            URL url;
            try {
                url = new URL("http", host, getRequestUri().toString());
            } catch (MalformedURLException exn) {
                throw new RuntimeException(exn); // should never happen
            }

            return url;
        }

        // accessors --------------------------------------------------------------

        @Id
        @Column(name="request_id")
        @GeneratedValue
        private Long getId()
        {
            return id;
        }

        private void setId(Long id)
        {
            this.id = id;
        }

        /**
         * Get the PipelineEndpoints.
         *
         * @return the PipelineEndpoints.
         */
        @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="pl_endp_id", nullable=false)
        public PipelineEndpoints getPipelineEndpoints()
        {
            return pipelineEndpoints;
        }

        public void setPipelineEndpoints(PipelineEndpoints pipelineEndpoints)
        {
            this.pipelineEndpoints = pipelineEndpoints;
        }

        /**
         * Request method.
         *
         * @return the request method.
         */
        @Type(type="com.untangle.node.http.HttpMethodUserType")
        public HttpMethod getMethod()
        {
            return method;
        }

        public void setMethod(HttpMethod method)
        {
            this.method = method;
        }

        /**
         * Request URI.
         *
         * @return the request URI.
         */
        @Column(name="uri")
        @Type(type="com.untangle.uvm.type.UriUserType")
        public URI getRequestUri()
        {
            return requestUri;
        }

        public void setRequestUri(URI requestUri)
        {
            this.requestUri = requestUri;
        }

        /**
         * The HttpRequestEvent that logged this item.
         *
         * @return the HttpRequestEvent.
         */
        @OneToOne(mappedBy="requestLine")
        public HttpRequestEvent getHttpRequestEvent()
        {
            return httpRequestEvent;
        }

        public void setHttpRequestEvent(HttpRequestEvent httpRequestEvent)
        {
            this.httpRequestEvent = httpRequestEvent;
        }

        // Object methods ---------------------------------------------------------

        public String toString()
        {
            return "RequestLine id: " + id + " length: "
                + requestUri.toString().length() + " (" + super.toString() + ")";
        }
    }
