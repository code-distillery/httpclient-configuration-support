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


import net.distilledcode.httpclient.impl.metatype.MetatypeBeanUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        scope = ServiceScope.PROTOTYPE,
        service = HttpClient.class,
        configurationPid = OsgiHttpClient.HTTP_CLIENT_ID
)
public class OsgiHttpClient extends DelegatingHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiHttpClient.class.getName());

    public static final String HTTP_CLIENT_ID = "org.apache.http.client.HttpClient";

    @Reference @SuppressWarnings("unused")
    private HttpClientBuilderFactory httpClientBuilderFactory;

    private String httpClientId;
    
    private CloseableHttpClient httpClient;

    @Activate @SuppressWarnings("unused")
    private void activate(final Map<String, Object> conf) {
        // TODO: detect and log/throw when ID is not unique
        httpClientId = conf.get("http.client.id").toString();
        final RequestConfig.Builder requestConfigBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
        MetatypeBeanUtil.applyConfiguration(conf, requestConfigBuilder);
        final RequestConfig requestConfig = requestConfigBuilder.build();

        final HttpClientBuilder httpClientBuilder = httpClientBuilderFactory.newBuilder();
        MetatypeBeanUtil.applyConfiguration(conf, httpClientBuilder);
        httpClient = httpClientBuilder.setDefaultRequestConfig(requestConfig).build();
        LOG.trace("Created HttpClient {}", httpClientId);
    }

    @Deactivate @SuppressWarnings("unused")
    private void deactivate() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOG.warn("Exception closing HttpClient {}", httpClientId, e);
            }
        }
    }

    @Override
    protected CloseableHttpClient getHttpClient() {
        return httpClient;
    }
}
