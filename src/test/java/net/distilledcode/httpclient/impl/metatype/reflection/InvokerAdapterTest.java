package net.distilledcode.httpclient.impl.metatype.reflection;

import org.apache.http.client.config.RequestConfig;
import org.junit.Test;

import static net.distilledcode.httpclient.impl.metatype.reflection.Invokers.beanGetters;
import static net.distilledcode.httpclient.impl.metatype.reflection.Invokers.beanSetters;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class InvokerAdapterTest {

    @Test
    public void setterAndGetter() throws Exception {
        Object maxRedirects = 17;

        RequestConfig.Builder builder = RequestConfig.custom();
        SetterAdapter setterAdapter = new SetterAdapter(builder, beanSetters(builder));
        setterAdapter.set("max.redirects", maxRedirects);

        RequestConfig requestConfig = builder.build();
        GetterAdapter getterAdapter = new GetterAdapter(requestConfig, beanGetters(requestConfig));
        assertThat(getterAdapter.get("max.redirects"), equalTo(maxRedirects));
    }
}
