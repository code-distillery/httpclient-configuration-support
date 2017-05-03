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

import org.apache.http.client.HttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.Map;

import static net.distilledcode.httpclient.impl.DefaultOsgiHttpClient.DEFAULT_HTTP_CLIENT_PID;

@Component(
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        configurationPid = DEFAULT_HTTP_CLIENT_PID,
        scope = ServiceScope.PROTOTYPE,
        service = HttpClient.class,
        property = {
                Constants.SERVICE_RANKING + ":Integer=100"
        }
)
public class DefaultOsgiHttpClient extends OsgiHttpClient {

    public static final String DEFAULT_HTTP_CLIENT_PID = "org.apache.http.client.HttpClient.default";

    private static final String DEFAULT_HTTP_CLIENT_ID = "default";

    @Reference @SuppressWarnings("unused")
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @Activate @SuppressWarnings("unused")
    private void activate(final Map<String, Object> conf) {
        doActivate(DEFAULT_HTTP_CLIENT_ID, httpClientBuilderFactory, conf);
    }

    @Deactivate @Override
    protected void deactivate() {
        super.deactivate();
    }
}
