package net.distilledcode.httpclient.impl;

import org.apache.http.client.HttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import static net.distilledcode.httpclient.impl.util.PredicateUtils.endsWith;
import static net.distilledcode.httpclient.impl.util.PredicateUtils.or;
import static net.distilledcode.httpclient.impl.util.PredicateUtils.startsWith;

@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        configurationPid = HttpClientConfiguration.HTTP_CLIENT_CONFIG_FACTORY_PID
)
public class HttpClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientConfiguration.class);

    public static final String HTTP_CLIENT_CONFIG_FACTORY_PID = "net.distilledcode.httpclient.Configuration";

    public static final String HTTP_CLIENT_CONFIG_NAME = "httpclient.config.name";

    protected static final String ORIGINAL_CLIENT_BUILDER_FACTORY_SERVICE_PID = "(service.pid=org.apache.http.httpclientfactory)";

    private static final Predicate<String> FORBIDDEN_PROPERTIES_PREDICATE = or(startsWith("service."), startsWith("component."), endsWith(".target"));

    @Reference(
            service = HttpClient.class,
            target = "(!(" + HttpClientConfiguration.HTTP_CLIENT_CONFIG_NAME +"=*))",
            policyOption = ReferencePolicyOption.GREEDY
    )
    @SuppressWarnings("unused")
    private Map<String, Object> defaultHttpClientConfig;

    private HttpClientBuilderFactory httpClientBuilderFactory;

    private ServiceRegistration<?> httpClientRegistration;

    private ServiceRegistration<HttpClientBuilderFactory> httpClientBuilderFactoryRegistration;

    @Reference(target = ORIGINAL_CLIENT_BUILDER_FACTORY_SERVICE_PID)
    protected void bindHttpClientBuilderFactory(final HttpClientBuilderFactory factory) {
        httpClientBuilderFactory = factory;
    }

    protected void unbindHttpClientBuilderFactory(final HttpClientBuilderFactory factory) {
        if (factory == httpClientBuilderFactory) {
            httpClientBuilderFactory = null;
        }
    }

    @Activate @SuppressWarnings("unused")
    protected void activate(final BundleContext ctx, final Map<String, Object> conf) {
        Hashtable<String, Object> properties = new Hashtable<>(conf);
        clean(properties, FORBIDDEN_PROPERTIES_PREDICATE);
        properties = prepareConfiguration(properties);
        HttpClientBuilderFactory factory = new PreconfiguredHttpClientBuilderFactory(httpClientBuilderFactory, properties);
        httpClientBuilderFactoryRegistration = ctx.registerService(
                HttpClientBuilderFactory.class,
                factory,
                properties
        );
        httpClientRegistration = ctx.registerService(
                HttpClient.class.getName(),
                new HttpClientPrototypeFactory(factory),
                properties
        );
        LOG.debug("Effective config for '{}': {}", conf.get(HTTP_CLIENT_CONFIG_NAME), properties);
    }

    @Deactivate @SuppressWarnings("unused")
    protected void deactivate() {
        if (httpClientRegistration != null) {
            httpClientRegistration.unregister();
        }

        if (httpClientBuilderFactoryRegistration != null) {
            httpClientBuilderFactoryRegistration.unregister();
        }
    }

    protected Hashtable<String, Object> prepareConfiguration(final Hashtable<String, Object> properties) {
        Hashtable<String, Object> props = new Hashtable<>(mergeMaps(properties, defaultHttpClientConfig));
        clean(props, FORBIDDEN_PROPERTIES_PREDICATE);
        return props;
    }

    private static void clean(final Map<String, Object> map, final Predicate<String> predicate) {
        for (final String key : new HashSet<>(map.keySet())) {
            if (predicate.test(key)) {
                map.remove(key);
            }
        }
    }

    @SafeVarargs
    private static Map<String, Object> mergeMaps(final Map<String, Object>... maps) {
        Map<String, Object> mergedMap = new HashMap<>();
        for (int i = maps.length - 1; 0 <= i; i--) {
            if (maps[i] != null) {
                mergedMap.putAll(maps[i]);
            }
        }
        return mergedMap;
    }
}
