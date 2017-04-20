/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package net.distilledcode.httpclient.impl;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public abstract class DelegatingHttpClient implements HttpClient {

    protected abstract CloseableHttpClient getHttpClient();

    @Override
    public HttpResponse execute(final HttpHost target, final HttpRequest request, final HttpContext context)
            throws IOException {
        return getHttpClient().execute(target, request, context);
    }

    @Override
    public HttpResponse execute(final HttpUriRequest request, final HttpContext context)
            throws IOException {
        return getHttpClient().execute(request, context);
    }

    @Override
    public HttpResponse execute(final HttpUriRequest request)
            throws IOException {
        return getHttpClient().execute(request);
    }

    @Override
    public HttpResponse execute(final HttpHost target, final HttpRequest request)
            throws IOException {
        return getHttpClient().execute(target, request);
    }

    @Override
    public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler)
            throws IOException {
        return getHttpClient().execute(request, responseHandler);
    }

    @Override
    public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context)
            throws IOException {
        return getHttpClient().execute(request, responseHandler, context);
    }

    @Override
    public <T> T execute(final HttpHost target, final HttpRequest request, final ResponseHandler<? extends T> responseHandler)
            throws IOException {
        return getHttpClient().execute(target, request, responseHandler);
    }

    @Override
    public <T> T execute(final HttpHost target, final HttpRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context)
            throws IOException {
        return getHttpClient().execute(target, request, responseHandler, context);
    }

    @Override
    public HttpParams getParams() {
        return getHttpClient().getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return getHttpClient().getConnectionManager();
    }
}
