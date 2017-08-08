package net.distilledcode.testing.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ServiceUtils {
    
    public static final String SERVICE_FILTER_ANY = "(objectClass=*)";

    public static final int EVENT_TYPE_ANY =
            ServiceEvent.MODIFIED
            | ServiceEvent.REGISTERED
            | ServiceEvent.UNREGISTERING
            | ServiceEvent.MODIFIED_ENDMATCH;

    public static <T> void withEachService(final BundleContext bundleContext, Class<T> type, String filter, final ServiceAction<T> action)
            throws InvalidSyntaxException {
        withEachService(bundleContext, type, filter, new ServiceReferenceAction<T>() {
            @Override
            public void perform(final ServiceReference<T> serviceReference) {
                if (serviceReference == null) {
                    action.perform(null);
                } else {
                    try {
                        action.perform(bundleContext.getService(serviceReference));
                    } finally {
                        bundleContext.ungetService(serviceReference);
                    }
                }
            }
        });
    }

    public static <T> void withEachService(BundleContext bundleContext, Class<T> type, String filter, ServiceReferenceAction<T> action)
            throws InvalidSyntaxException {
        withEachService(bundleContext, type, filter, action, Integer.MAX_VALUE);
    }

    private static <T> void withEachService(BundleContext bundleContext, Class<T> type, String filter, ServiceReferenceAction<T> action, int maxCount)
            throws InvalidSyntaxException {
        final Collection<ServiceReference<T>> serviceReferences = bundleContext.getServiceReferences(type, filter);
        if (serviceReferences.isEmpty()) {
            try {
                action.perform(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        List<ServiceReference<T>> references = new ArrayList<>(serviceReferences);
        for (int i = 0; i < Math.min(references.size(), maxCount); i++) {
            try {
                action.perform(references.get(i));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <T> void withEachService(BundleContext bundleContext, Class<T> type, String filter, final ServicePropertiesAction action)
            throws InvalidSyntaxException {
        withEachService(bundleContext, type, filter, action, Integer.MAX_VALUE);
    }

    private static <T> void withEachService(BundleContext bundleContext, Class<T> type, String filter, final ServicePropertiesAction action, int maxCount)
            throws InvalidSyntaxException {
        withEachService(bundleContext, type, filter, new ServiceReferenceAction<T>() {
            @Override
            public void perform(final ServiceReference<T> serviceReference) {
                action.perform(serviceReference == null ? null : toMap(serviceReference));
            }
        }, maxCount);
    }

    /**
     * Same as {@link #awaitServiceEvent(BundleContext, String, int, Action)} but
     * matching any service events.
     *
     * @param bundleContext The {@code BundleContext} used to observe service events.
     * @param action The action that effects a change that should cause the expected ServiceEvent.
     */
    public static void awaitServiceEvent(final BundleContext bundleContext, final Action action)
            throws InvalidSyntaxException {
        awaitServiceEvent(bundleContext, null, EVENT_TYPE_ANY, action);
    }

    /**
     * Same as {@link #awaitServiceEvent(BundleContext, String, int, long, TimeUnit, Action)} but
     * with a default timeout of 1000 milliseconds.
     *
     * @param bundleContext The {@code BundleContext} used to observe service events.
     * @param serviceFilter A filter expression following the syntax of {@link Filter} (default: (objectClass=*))
     * @param eventTypes An integer bitmap of accepted ServiceEvent types (default: any).
     * @param action The action that effects a change that should cause the expected ServiceEvent.
     * @throws InvalidSyntaxException If the {@code serviceFilter} string cannot be parsed successfully
     */
    public static void awaitServiceEvent(final BundleContext bundleContext,
                                         final String serviceFilter,
                                         final int eventTypes,
                                         final Action action) throws InvalidSyntaxException {
        awaitServiceEvent(bundleContext, serviceFilter, eventTypes, 5000, TimeUnit.MILLISECONDS, action);
    }

    /**
     * Execute a change in a action and wait for a ServiceEvent to happen. The method only returns once
     * an appropriate event is matched. If no event matches within the specified timeout, an AssertionError
     * is thrown. The error message describes any non-matching events that may have happened for debugging
     * purposes.
     * @param bundleContext The {@code BundleContext} used to observe service events.
     * @param serviceFilter A filter expression following the syntax of {@link Filter} (default: (objectClass=*))
     * @param eventTypes An integer bitmap of accepted ServiceEvent types.
     * @param timeout A timeout value; the maximum time to wait for the service event. The unit depends on the {@code timeUnit} argument.
     * @param timeUnit The unit for the timeout value.
     * @param action The action that effects a change that should cause the expected ServiceEvent.
     */
    public static void awaitServiceEvent(final BundleContext bundleContext,
                                         final String serviceFilter,
                                         final int eventTypes,
                                         final long timeout,
                                         final TimeUnit timeUnit,
                                         final Action action) throws InvalidSyntaxException {
        final Filter filter = bundleContext.createFilter(serviceFilter == null ? SERVICE_FILTER_ANY : serviceFilter);
        final CountDownLatch latch = new CountDownLatch(1);
        final List<ServiceEvent> events = new CopyOnWriteArrayList<>();
        final ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceChanged(final ServiceEvent event) {
                events.add(event);
                if ((eventTypes & event.getType()) > 0 && filter.match(event.getServiceReference())) {
                    latch.countDown();
                }
            }
        };

        try {
            bundleContext.addServiceListener(listener);

            action.perform();

            if (!latch.await(timeout, timeUnit)) {
                throw new AssertionError("Exceeded timeout waiting for service event matching " +
                        String.format("[eventTypes: %s, filter: %s], " +
                                "got %d non matching events: %s", eventTypes, serviceFilter, events.size(), events));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            bundleContext.removeServiceListener(listener);
        }
    }

    public static Dictionary<String, Object> properties(final Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must have an even number of elements");
        }
        final Hashtable<String, Object> properties = new Hashtable<>();
        for (int i = 1; i < keyValues.length; i+=2) {
            Object rawKey = keyValues[i - 1];
            if (!(rawKey instanceof String)) {
                throw new IllegalArgumentException("The key at keyValues[" + (i - 1) +"] is a " + rawKey.getClass().getName() +", it must be a String");
            }
            properties.put((String)rawKey, keyValues[i]);
        }
        return properties;
    }

    public static Map<String, Object> toMap(ServiceReference<?> serviceReference) {
        final Map<String, Object> map = new HashMap<>();
        for (final String key : serviceReference.getPropertyKeys()) {
            map.put(key, serviceReference.getProperty(key));
        }
        return map;
    }

    public interface Action {
        void perform() throws Exception;
    }

    public interface ServiceAction<T> {
        void perform(T service);
    }

    public interface ServiceReferenceAction<T> extends ServiceAction<ServiceReference<T>> {
        void perform(ServiceReference<T> serviceReference);
    }

    public interface ServicePropertiesAction extends ServiceAction<Map<String,Object>> {
        void perform(final Map<String, Object> properties);
    }
}
