package net.distilledcode.httpclient.impl.metatype;

import net.distilledcode.httpclient.impl.metatype.reflection.GetterAdapter;
import net.distilledcode.httpclient.impl.metatype.reflection.Invokers.Invoker;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.distilledcode.httpclient.impl.DefaultOsgiHttpClient.DEFAULT_HTTP_CLIENT_PID;
import static net.distilledcode.httpclient.impl.OsgiHttpClient.HTTP_CLIENT_FACTORY_PID;
import static net.distilledcode.httpclient.impl.metatype.MetaTypeBeanUtil.attributeDefinitions;
import static net.distilledcode.httpclient.impl.metatype.MetaTypeBeanUtil.join;
import static net.distilledcode.httpclient.impl.metatype.MetaTypeBeanUtil.attributeDefinition;
import static net.distilledcode.httpclient.impl.metatype.reflection.Invokers.beanGetters;
import static net.distilledcode.httpclient.impl.metatype.reflection.Invokers.beanSetters;
import static net.distilledcode.httpclient.impl.metatype.MetaTypeBeanUtil.camelToDotted;
import static net.distilledcode.httpclient.impl.metatype.reflection.Invokers.conditionalNoArgsSetter;
import static net.distilledcode.httpclient.impl.metatype.reflection.Invokers.defaultArgumentSetter;

@Component(
        property = {
                MetaTypeProvider.METATYPE_PID + "=" + DEFAULT_HTTP_CLIENT_PID,
                MetaTypeProvider.METATYPE_FACTORY_PID + "=" + HTTP_CLIENT_FACTORY_PID
        }
)
@SuppressWarnings("unused")
public class HttpClientMetaType implements MetaTypeProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientMetaType.class);

    public static final String REQUEST_CONFIG_NAMESPACE = "request.config";

    public static final Map<String, Invoker<?>> SETTERS_REQUEST_CONFIG_BUILDER =
            Collections.unmodifiableMap(beanSetters(RequestConfig.Builder.class));

    public static final Map<String, Invoker<?>> SETTERS_HTTP_CLIENT_BUILDER;
    static {
        final Map<String, Invoker<?>> invokers = new HashMap<>();
        invokers.putAll(beanSetters(HttpClientBuilder.class));
        String prefix = "disable";
        for (final Method method : HttpClientBuilder.class.getDeclaredMethods()) {
            String name = method.getName();
            if (name.startsWith(prefix) && method.getParameterTypes().length == 0) {
                String camelName = name.substring(prefix.length());
                String propertyName = camelToDotted(camelName) + ".enabled";
                invokers.put(propertyName, conditionalNoArgsSetter(method, false));
            }
        }
        try {
            invokers.put("evict.expired.connections",
                    conditionalNoArgsSetter(HttpClientBuilder.class.getMethod("evictExpiredConnections"), true));
            invokers.put("evict.idle.connections.ms",
                    defaultArgumentSetter(
                            HttpClientBuilder.class.getMethod("evictIdleConnections", long.class, TimeUnit.class),
                            null, TimeUnit.MILLISECONDS));

        } catch (NoSuchMethodException e) {
            LOG.warn("Failed setting up configuration option due to missing method", e);
        }
        SETTERS_HTTP_CLIENT_BUILDER = Collections.unmodifiableMap(invokers);
    }

    private static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = join(
            attributeDefinitions(REQUEST_CONFIG_NAMESPACE, SETTERS_REQUEST_CONFIG_BUILDER, new GetterAdapter(RequestConfig.DEFAULT, beanGetters(RequestConfig.class))),
            attributeDefinitions("", SETTERS_HTTP_CLIENT_BUILDER)
    );

    private static final ObjectClassDefinition DEFAULT_OBJECT_CLASS_DEFINITION = MetaTypeBeanUtil.createObjectClassDefinition(
            DEFAULT_HTTP_CLIENT_PID,
            "Apache HTTP Components Default HTTP Client Configuration",
            "Configuration to provide pre-configured HttpClient instances via the service registry.",
            ATTRIBUTE_DEFINITIONS
    );

    private static final ObjectClassDefinition FACTORY_OBJECT_CLASS_DEFINITION = MetaTypeBeanUtil.createObjectClassDefinition(
            HTTP_CLIENT_FACTORY_PID,
            "Apache HTTP Components HTTP Client Configuration",
            "Configuration to provide pre-configured HttpClient instances via the service registry.",
            join(
                    attributeDefinition("http.client.id", "HttpClient ID", String.class),
                    attributeDefinition("webconsole.configurationFactory.nameHint", null, String.class, "HttpClient ID: {http.client.id}"),
                    ATTRIBUTE_DEFINITIONS
            )
    );

    private static final Map<String, ObjectClassDefinition> DEFINITIONS;
    static {
        Map<String, ObjectClassDefinition> defs = new HashMap<>();
        defs.put(DEFAULT_HTTP_CLIENT_PID, DEFAULT_OBJECT_CLASS_DEFINITION);
        defs.put(HTTP_CLIENT_FACTORY_PID, FACTORY_OBJECT_CLASS_DEFINITION);
        DEFINITIONS = Collections.unmodifiableMap(defs);
    }

    @Override
    public ObjectClassDefinition getObjectClassDefinition(final String id, final String locale) {
        return DEFINITIONS.get(id);
    }

    @Override
    public String[] getLocales() {
        return null;
    }
}
