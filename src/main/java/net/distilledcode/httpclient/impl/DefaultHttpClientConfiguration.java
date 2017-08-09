package net.distilledcode.httpclient.impl;

import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.Hashtable;
import java.util.Map;

@Component(
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        configurationPid = DefaultHttpClientConfiguration.DEFAULT_HTTP_CLIENT_CONFIG_PID
)
public class DefaultHttpClientConfiguration extends HttpClientConfiguration {

    public static final String DEFAULT_HTTP_CLIENT_CONFIG_PID = HTTP_CLIENT_CONFIG_FACTORY_PID + ".default";

    protected Hashtable<String, Object> prepareConfiguration(final Hashtable<String, Object> properties) {
        properties.put(Constants.SERVICE_RANKING, 100);
        return properties;
    }

    //--- the overridden methods below are required for correct SCR XML generation ---//

    @Reference(target = ORIGINAL_CLIENT_BUILDER_FACTORY_SERVICE_PID)
    protected void bindHttpClientBuilderFactory(HttpClientBuilderFactory factory) {
        super.bindHttpClientBuilderFactory(factory);
    }

    protected void unbindHttpClientBuilderFactory(HttpClientBuilderFactory factory) {
        super.unbindHttpClientBuilderFactory(factory);
    }

    @Activate
    protected void activate(BundleContext ctx, Map<String, Object> configuration) {
        super.activate(ctx, configuration);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

}
