package net.distilledcode.httpclient.impl;

import net.distilledcode.testing.osgi.ServiceUtils;
import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static net.distilledcode.testing.osgi.PaxUtils.debug;
import static net.distilledcode.testing.osgi.PaxUtils.failOnUnresolvedBundle;
import static net.distilledcode.testing.osgi.PaxUtils.findProjectBundle;
import static net.distilledcode.testing.osgi.PaxUtils.logback;
import static net.distilledcode.testing.osgi.PaxUtils.webconsole;
import static net.distilledcode.testing.osgi.PaxUtils.workingDirectory;
import static net.distilledcode.testing.osgi.ServiceUtils.EVENT_TYPE_ANY;
import static net.distilledcode.testing.osgi.ServiceUtils.awaitServiceEvent;
import static net.distilledcode.testing.osgi.ServiceUtils.properties;
import static net.distilledcode.testing.osgi.ServiceUtils.withEachService;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HttpClientConfigurationIT {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientConfigurationIT.class);

    private static final String HTTP_CLIENT_FILTER = objectClassFilter(HttpClient.class);

    @Inject
    private BundleContext bundleContext;

    @Inject
    private HttpClient defaultHttpClient;

    @Inject
    private ConfigurationAdmin configurationAdmin;

    @org.ops4j.pax.exam.Configuration
    public Option[] config() throws IOException, URISyntaxException {
        final File projectJarFile = findProjectBundle("httpclient-configuration-support-*.jar");
        return options(
                bootClasspathLibrary(toPath(org.ops4j.pax.url.link.Handler.class)), // support for link: protocol
                bootClasspathLibrary(toPath(org.ops4j.pax.url.classpath.Handler.class)), // support for classpath: protocol
                failOnUnresolvedBundle(),
                workingDirectory(getClass()),
                logback(),
                bundle(projectJarFile.toURI().toString()),
                linkBundle("org.apache.felix.configadmin"),
                linkBundle("org.apache.felix.eventadmin"),
                linkBundle("org.apache.felix.metatype"),
                linkBundle("org.apache.felix.scr"),
                linkBundle("org.apache.httpcomponents.httpclient"),
                linkBundle("org.apache.httpcomponents.httpcore"),
                linkBundle("org.apache.commons.lang"),

                // only required for the test setup
                junitBundles(),
                linkBundle("org.apache.commons.io"),
                cleanCaches(),
                debug(Integer.getInteger("debug.port", 0))
                        .useOptions(webconsole(Integer.getInteger("http.port", 8080)))
        );
    }

    private String toPath(final Class<?> clazz) throws URISyntaxException {
        return clazz.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
    }

    @Test
    public void defaultClientRegistration() throws Exception {
        assertThat(bundleContext, notNullValue());
        assertThat("Expected a HttpClient instance", defaultHttpClient, notNullValue());
        assertThat("DefaultOsgiHttpClient", equalTo(defaultHttpClient.getClass().getSimpleName()));
    }

    @Test
    public void defaultClientRegistrationWithConfiguration() throws Exception {
        withEachService(bundleContext, HttpClient.class, new ServiceUtils.ServiceAction<HttpClient>() {
            @Override
            public void perform(final HttpClient service) {
                assertThat("Expected a HttpClient instance", service, notNullValue());
                assertThat("DefaultOsgiHttpClient", equalTo(service.getClass().getSimpleName()));
                assertThat(service, sameInstance(defaultHttpClient));
            }
        });

        final Configuration configuration = configFor(DefaultOsgiHttpClient.DEFAULT_HTTP_CLIENT_PID);
        assertThat(configuration.getProperties(), nullValue());

        awaitServiceEvent(bundleContext, HTTP_CLIENT_FILTER, ServiceEvent.REGISTERED, new ServiceUtils.Action() {
            @Override
            public void perform() throws IOException {
                LOG.trace("Updating config {}", configuration);
                configuration.update(properties("request.config.socket.timeout", 2000));
            }
        });

        withEachService(bundleContext, HttpClient.class, new ServiceUtils.ServiceAction<HttpClient>() {
            @Override
            public void perform(final HttpClient service) {
                assertThat("Expected a HttpClient instance", service, notNullValue());
                assertThat(service.getClass().getSimpleName(), equalTo("DefaultOsgiHttpClient"));
                assertThat(service, not(sameInstance(defaultHttpClient)));
            }
        });

        awaitServiceEvent(bundleContext, new ServiceUtils.Action() {
            @Override
            public void perform() throws IOException {
                configuration.delete();
            }
        });
    }

    @Test
    public void clientRegistrationWithFactoryConfiguration() throws Exception {
        withEachService(bundleContext, HttpClient.class, "(http.client.id=test-client)", new ServiceUtils.ServiceAction<HttpClient>() {
            @Override
            public void perform(final HttpClient service) {
                assertThat("HttpClient(http.client.id=test-client) should require configuration",
                        service, nullValue());
            }
        });

        final Configuration configuration = configurationAdmin.createFactoryConfiguration(OsgiHttpClient.HTTP_CLIENT_FACTORY_PID, null);

        awaitServiceEvent(bundleContext, HTTP_CLIENT_FILTER, EVENT_TYPE_ANY, new ServiceUtils.Action() {
            @Override
            public void perform() throws IOException {
                LOG.trace("Updating config {}", configuration);
                configuration.update(properties(
                        "http.client.id", "test-client",
                        "request.config.socket.timeout", 5000
                ));
            }
        });

        withEachService(bundleContext, HttpClient.class, "(http.client.id=test-client)", new ServiceUtils.ServiceAction<HttpClient>() {
            @Override
            public void perform(final HttpClient service) {
                assertThat("Expected a HttpClient instance", service, notNullValue());
                assertThat(service.getClass().getSimpleName(), equalTo("OsgiHttpClient"));
                assertThat(service.toString(), containsString("test-client"));
                assertThat(service, not(sameInstance(defaultHttpClient)));
            }
        });

        awaitServiceEvent(bundleContext, HTTP_CLIENT_FILTER, ServiceEvent.UNREGISTERING, new ServiceUtils.Action() {
            @Override
            public void perform() throws IOException {
                configuration.delete();
            }
        });

        withEachService(bundleContext, HttpClient.class, "(http.client.id=test-client)", new ServiceUtils.ServiceAction<HttpClient>() {
            @Override
            public void perform(final HttpClient service) {
                assertThat("HttpClient(http.client.id=test-client) should require configuration",
                        service, nullValue());
            }
        });
    }

    private Configuration configFor(final String name) throws IOException {
        return configurationAdmin.getConfiguration(name, null);
    }

    private static String objectClassFilter(final Class<?> clazz) {
        return "(objectClass=" + clazz.getName() + ")";
    }
}
