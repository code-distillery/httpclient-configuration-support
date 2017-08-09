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

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class HttpClientPrototypeFactory implements PrototypeServiceFactory<CloseableHttpClient> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientPrototypeFactory.class.getName());

    private final HttpClientBuilderFactory httpClientBuilderFactory;

    public HttpClientPrototypeFactory(final HttpClientBuilderFactory httpClientBuilderFactory) {
        this.httpClientBuilderFactory = httpClientBuilderFactory;
    }

    @Override
    public CloseableHttpClient getService(final Bundle bundle, final ServiceRegistration<CloseableHttpClient> registration) {
        return httpClientBuilderFactory.newBuilder().build();
    }

    @Override
    public void ungetService(final Bundle bundle, final ServiceRegistration<CloseableHttpClient> registration, final CloseableHttpClient httpClient) {
        try {
            httpClient.close();
        } catch (IOException e) {
            LOG.error("Failed to close HttpClient", e);
        }
    }
}
