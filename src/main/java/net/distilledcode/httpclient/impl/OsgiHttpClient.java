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

import net.distilledcode.httpclient.impl.metatype.HttpClientMetaType;
import net.distilledcode.httpclient.impl.metatype.MetaTypeBeanUtil;
import net.distilledcode.httpclient.impl.metatype.reflection.SetterAdapter;
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
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        scope = ServiceScope.PROTOTYPE,
        service = HttpClient.class,
        configurationPid = OsgiHttpClient.HTTP_CLIENT_FACTORY_PID
)
public class OsgiHttpClient extends DelegatingHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiHttpClient.class.getName());

    public static final String HTTP_CLIENT_FACTORY_PID = "org.apache.http.client.HttpClient";

    private static final String HTTP_CLIENT_CONFIG_NAME = "httpclient.config.name";

    @Reference @SuppressWarnings("unused")
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @Reference(
            service = HttpClient.class,
            target = "(!(httpclient.config.name=*))",
            policyOption = ReferencePolicyOption.GREEDY)
    @SuppressWarnings("unused")
    private Map<String, Object> defaultHttpClientConfig;

    private String configName;

    private CloseableHttpClient httpClient;

    @Activate @SuppressWarnings("unused")
    private void activate(final Map<String, Object> conf) {
        if (!conf.containsKey(HTTP_CLIENT_CONFIG_NAME)) {
            throw new IllegalStateException("Configuration contains no " + HTTP_CLIENT_CONFIG_NAME + " property");
        }

        String id = conf.get(HTTP_CLIENT_CONFIG_NAME).toString();

        Map<String, Object> effectiveConfig = mergeMaps(conf, defaultHttpClientConfig);
        doActivate(id, httpClientBuilderFactory, effectiveConfig);
        LOG.debug("Effective config for '{}': {}", id, effectiveConfig);
    }

    @Deactivate @SuppressWarnings("unused")
    protected void deactivate() {
        LOG.trace("Shutting down HttpClient {}", this);
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOG.warn("Exception closing HttpClient {}", this, e);
            }
        }
    }

    @SafeVarargs
    private static Map<String, Object> mergeMaps(final Map<String, Object>... maps) {
        Map<String, Object> mergedMap = new HashMap<>();
        for (int i = maps.length - 1; 0 <= i; i--) {
            mergedMap.putAll(maps[i]);
        }
        return mergedMap;
    }

    void doActivate(final String configName, final HttpClientBuilderFactory factory, final Map<String, Object> conf) {
        this.configName = configName;
        final RequestConfig.Builder requestConfigBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
        MetaTypeBeanUtil.applyConfiguration(HttpClientMetaType.REQUEST_CONFIG_NAMESPACE, conf,
                new SetterAdapter(requestConfigBuilder, HttpClientMetaType.SETTERS_REQUEST_CONFIG_BUILDER));
        final RequestConfig requestConfig = requestConfigBuilder.build();

        if (factory == null) {
            throw new IllegalStateException("Please make sure an httpClientBuilderFactory is available");
        }

        final HttpClientBuilder httpClientBuilder = factory.newBuilder();
        MetaTypeBeanUtil.applyConfiguration("", conf, new SetterAdapter(httpClientBuilder, HttpClientMetaType.SETTERS_HTTP_CLIENT_BUILDER));
        httpClient = httpClientBuilder.setDefaultRequestConfig(requestConfig).build();
        LOG.trace("Created HttpClient {}", this);
    }

    @Override
    protected CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public String toString() {
        return super.toString() + "[httpclient.config.name=" + configName + "]";
    }
}
