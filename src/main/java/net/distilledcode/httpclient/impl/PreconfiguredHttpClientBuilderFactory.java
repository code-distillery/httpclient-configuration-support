package net.distilledcode.httpclient.impl;

import net.distilledcode.httpclient.impl.metatype.HttpClientConfigurationMetaType;
import net.distilledcode.httpclient.impl.metatype.MetaTypeBeanUtil;
import net.distilledcode.httpclient.impl.metatype.reflection.SetterAdapter;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;

import java.util.Map;

class PreconfiguredHttpClientBuilderFactory implements HttpClientBuilderFactory {

    private final HttpClientBuilderFactory httpClientBuilderFactory;

    private final Map<String, Object> effectiveConfiguration;

    public PreconfiguredHttpClientBuilderFactory(final HttpClientBuilderFactory factory, final Map<String, Object> effectiveConfiguration) {
        this.httpClientBuilderFactory = factory;
        this.effectiveConfiguration = effectiveConfiguration;
    }

    @Override
    public HttpClientBuilder newBuilder() {
        final HttpClientBuilder httpClientBuilder = httpClientBuilderFactory.newBuilder();
        configure(httpClientBuilder);
        return httpClientBuilder;
    }

    private void configure(HttpClientBuilder httpClientBuilder) {
        final RequestConfig.Builder requestConfigBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
        MetaTypeBeanUtil.applyConfiguration(HttpClientConfigurationMetaType.REQUEST_CONFIG_NAMESPACE, effectiveConfiguration,
                new SetterAdapter(requestConfigBuilder, HttpClientConfigurationMetaType.SETTERS_REQUEST_CONFIG_BUILDER));
        final RequestConfig requestConfig = requestConfigBuilder.build();
        MetaTypeBeanUtil.applyConfiguration("", effectiveConfiguration,
                new SetterAdapter(httpClientBuilder, HttpClientConfigurationMetaType.SETTERS_HTTP_CLIENT_BUILDER));
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
    }
}
