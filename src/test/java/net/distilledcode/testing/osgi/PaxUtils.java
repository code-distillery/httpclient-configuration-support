package net.distilledcode.testing.osgi;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.OptionalCompositeOption;
import org.ops4j.pax.exam.util.PathUtils;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

public class PaxUtils {

    public static Option workingDirectory(Class<?> type) {
        return CoreOptions.workingDirectory(PathUtils.getBaseDir() + "/target/pax-exam/" + type.getName());
    }

    public static File findProjectBundle(final String wildcard) {
        File target = new File(PathUtils.getBaseDir() + "/target");
        assertThat("HttpClientConfigurationIT assumes a \"target\" directory exists", target.isDirectory(), is(true));

        WildcardFileFilter wildcardFileFilter = new WildcardFileFilter(wildcard);
        Collection<File> files = FileUtils.listFiles(target, wildcardFileFilter, falseFileFilter());
        assertThat("No project jar file matching '" + wildcard + "' found", files.isEmpty(), is(false));
        assertThat("Found more than one candidate project jar files " + files, files.size(), is(1));

        return files.iterator().next();
    }

    public static Option failOnUnresolvedBundle() {
        return systemProperty("pax.exam.osgi.unresolved.fail").value("true");
    }

    public static URL resourceUrl(String path) {
        URL resource = PaxUtils.class.getClassLoader().getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Could not find resource '" + path + "'");
        }
        return resource;
    }

    public static Option logback() {
        final URL logbackConfigUrl = resourceUrl("logback-test.xml");
        return composite(
                linkBundle("slf4j.api"),
                linkBundle("ch.qos.logback.classic"),
                linkBundle("ch.qos.logback.core"),
                systemProperty("pax.exam.logging").value("none"),
                systemProperty("logback.configurationFile").value(logbackConfigUrl.toString())
                //systemProperty("logback.configurationFile").value("file:" + PathUtils.getBaseDir() + "/target/test-classes/logback-test.xml")
        );
    }

    public static OptionalCompositeOption debug(final int debugPort) {
        final int fiveMinutesInMs = 5 * 60 * 1000;
        return when(debugPort > 1024).useOptions(
                vmOption("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort),
                systemProperty("pax.exam.service.timeout").value(Integer.toString(fiveMinutesInMs)),
                systemTimeout(fiveMinutesInMs)
        );
    }

    public static Option jettyHttp(final int httpPort) {
        return composite(
                frameworkProperty("org.osgi.service.http.port").value(Integer.toString(httpPort)),
                linkBundle("org.apache.felix.http.jetty"),
                linkBundle("org.apache.felix.http.servlet-api"),
                linkBundle("org.apache.felix.http.whiteboard")
        );
    }

    public static Option webconsole(final int httpPort) {
        return composite(
                jettyHttp(httpPort),
                linkBundle("org.apache.felix.inventory"),
                linkBundle("org.apache.felix.webconsole"),
                linkBundle("org.apache.felix.webconsole.plugins.ds"),
                linkBundle("org.apache.felix.webconsole.plugins.event"),
                linkBundle("org.apache.commons.fileupload"),
                linkBundle("json")
        );
    }
}
