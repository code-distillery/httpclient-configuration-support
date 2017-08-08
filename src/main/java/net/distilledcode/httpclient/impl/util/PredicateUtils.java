package net.distilledcode.httpclient.impl.util;

import org.osgi.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredicateUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PredicateUtils.class);

    @SafeVarargs
    public static <T> Predicate<T> or(final Predicate<T>... predicates) {
        return new Predicate<T>() {
            @Override
            public boolean test(final T t) {
                for (final Predicate<T> predicate : predicates) {
                    if (predicate.test(t)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static Predicate<String> startsWith(final String prefix) {
        return new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.startsWith(prefix);
            }
        };
    }

    public static Predicate<String> endsWith(final String postfix) {
        return new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.endsWith(postfix);
            }
        };
    }
}
