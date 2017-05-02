package net.distilledcode.httpclient.impl.metatype;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

import static net.distilledcode.httpclient.impl.OsgiHttpClient.HTTP_CLIENT_ID;
import static net.distilledcode.httpclient.impl.metatype.MetatypeBeanUtil.attributeDefinitionsFromSetters;
import static net.distilledcode.httpclient.impl.metatype.MetatypeBeanUtil.join;
import static net.distilledcode.httpclient.impl.metatype.MetatypeBeanUtil.attributeDefinition;

@Component(
        property = MetaTypeProvider.METATYPE_FACTORY_PID + "=" + HTTP_CLIENT_ID
)
@SuppressWarnings("unused")
public class HttpClientMetatype implements MetaTypeProvider {

    public static final String REQUEST_CONFIG_NAMESPACE = "request.config";

    private final ObjectClassDefinition objectClassDefinition = MetatypeBeanUtil.createObjectClassDefinition(
            HTTP_CLIENT_ID,
            "Apache HTTP Components HTTP Client Configuration",
            "Configuration to provide pre-configured HttpClient instances via the service registry.",
            join(
                    attributeDefinition("http.client.id", "HttpClient ID", String.class),
                    attributeDefinition("webconsole.configurationFactory.nameHint", null, String.class, "HttpClient ID: {http.client.id}"),
                    attributeDefinitionsFromSetters(REQUEST_CONFIG_NAMESPACE, RequestConfig.Builder.class, RequestConfig.DEFAULT),
                    attributeDefinitionsFromSetters(HttpClientBuilder.class, null)
            )
    );

    @Override
    public ObjectClassDefinition getObjectClassDefinition(final String id, final String locale) {
        if (HTTP_CLIENT_ID.equals(id)) {
            return objectClassDefinition;
        }
        return null;
    }

    @Override
    public String[] getLocales() {
        return null;
    }
}
