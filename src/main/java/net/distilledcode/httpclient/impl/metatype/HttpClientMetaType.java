package net.distilledcode.httpclient.impl.metatype;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.distilledcode.httpclient.impl.DefaultOsgiHttpClient.DEFAULT_HTTP_CLIENT_PID;
import static net.distilledcode.httpclient.impl.OsgiHttpClient.HTTP_CLIENT_FACTORY_PID;
import static net.distilledcode.httpclient.impl.metatype.MetatypeBeanUtil.attributeDefinitionsFromSetters;
import static net.distilledcode.httpclient.impl.metatype.MetatypeBeanUtil.join;
import static net.distilledcode.httpclient.impl.metatype.MetatypeBeanUtil.attributeDefinition;

@Component(
        property = {
                MetaTypeProvider.METATYPE_PID + "=" + DEFAULT_HTTP_CLIENT_PID,
                MetaTypeProvider.METATYPE_FACTORY_PID + "=" + HTTP_CLIENT_FACTORY_PID
        }
)
@SuppressWarnings("unused")
public class HttpClientMetaType implements MetaTypeProvider {

    public static final String REQUEST_CONFIG_NAMESPACE = "request.config";

    private static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = join(
            attributeDefinitionsFromSetters(REQUEST_CONFIG_NAMESPACE, RequestConfig.Builder.class, RequestConfig.DEFAULT),
            attributeDefinitionsFromSetters(HttpClientBuilder.class, null)
    );

    private static final ObjectClassDefinition DEFAULT_OBJECT_CLASS_DEFINITION = MetatypeBeanUtil.createObjectClassDefinition(
            DEFAULT_HTTP_CLIENT_PID,
            "Apache HTTP Components Default HTTP Client Configuration",
            "Configuration to provide pre-configured HttpClient instances via the service registry.",
            ATTRIBUTE_DEFINITIONS
    );

    private static final ObjectClassDefinition FACTORY_OBJECT_CLASS_DEFINITION = MetatypeBeanUtil.createObjectClassDefinition(
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
