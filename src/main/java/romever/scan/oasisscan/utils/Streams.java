package romever.scan.oasisscan.utils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public abstract class Streams {

    // TRANSFORM

    /**
     * iterable -> list
     *
     * @param iterable
     * @param mapper
     *            T -> R
     * @return
     */
    public static <T, R> List<R> map(Iterable<T> iterable, Function<? super T, ? extends R> mapper) {
        return StreamSupport.stream(iterable.spliterator(), false).map(mapper).collect(toList());
    }

    public static <T, R> List<R> mapParallel(Iterable<T> iterable, Function<? super T, ? extends R> mapper) {
        return StreamSupport.stream(iterable.spliterator(), true).map(mapper).filter(e -> e != null).collect(toList());
    }

    /**
     * iterable -> map
     *
     * @param iterable
     * @param keyMapper
     *            T -> K
     * @param valueMapper
     *            T -> V
     * @return
     */
    public static <T, K, V> Map<K, V> map(Iterable<T> iterable, Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(toMap(keyMapper, valueMapper));
    }

    /**
     * map -> map
     *
     * @param map
     * @param keyMapper
     *            K1 -> K2
     * @param valueMapper
     *            V1 -> V2
     * @return
     */
    public static <K1, V1, K2, V2> Map<K2, V2> map(Map<K1, V1> map, Function<? super K1, ? extends K2> keyMapper,
            Function<? super V1, ? extends V2> valueMapper) {
        return map.entrySet().stream()
                .collect(toMap(e -> keyMapper.apply(e.getKey()), e -> valueMapper.apply(e.getValue())));
    }

    /**
     * map -> map
     *
     * @param map
     * @param valueMapper
     *            V1 -> V2
     * @return
     */
    public static <K, V1, V2> Map<K, V2> mapValue(Map<K, V1> map, Function<? super V1, ? extends V2> valueMapper) {
        return map.entrySet().stream().collect(toMap(e -> e.getKey(), e -> valueMapper.apply(e.getValue())));
    }

    // FILTER

    /**
     * iterable -> filter -> list
     *
     * @param iterable
     * @param predicate
     * @return
     */
    public static <T> List<T> filter(Iterable<T> iterable, Predicate<? super T> predicate) {
        return StreamSupport.stream(iterable.spliterator(), false).filter(predicate).collect(toList());
    }

    /**
     * iterable -> filter -> list
     *
     * @param iterable
     * @param predicate
     * @return
     */
    public static <T, R> List<R> filter(Iterable<T> iterable, Predicate<? super T> predicate,
            Function<? super T, ? extends R> mapper) {
        return StreamSupport.stream(iterable.spliterator(), false).filter(predicate).map(mapper).collect(toList());
    }

    /**
     * map -> filter -> map
     *
     * @param map
     * @param predicate
     * @return
     */
    public static <K, V> Map<K, V> filterValue(Map<K, V> map, Predicate<? super V> predicate) {
        return map.entrySet().stream().filter(e -> predicate.test(e.getValue()))
                .collect(toMap(e -> e.getKey(), e -> e.getValue()));
    }

    // JOIN

    /**
     * collection -> string -> join
     *
     * @param coll
     * @param mapper
     * @param delimiter
     * @return
     */
    public static <T> String join(Collection<T> coll, Function<? super T, ? extends CharSequence> mapper,
            CharSequence delimiter) {
        return coll.stream().map(mapper).collect(joining(delimiter));
    }

    /**
     * collection -> toString -> join
     *
     * @param coll
     * @param delimiter
     * @return
     */
    public static <T> String join(Collection<T> coll, CharSequence delimiter) {
        return join(coll, T::toString, delimiter);
    }

    /**
     * collection -> toString -> join
     *
     * @param coll
     * @param delimiter
     * @return
     */
    public static <T> String filterJoin(Collection<T> coll, Predicate<? super T> predicate, CharSequence delimiter) {
        return coll.stream().filter(predicate).map(T::toString).collect(joining(delimiter));
    }

    // SORT

    /**
     * collection sort to list by comparator
     *
     * @param coll
     * @param comparator
     * @return
     */
    public static <T> List<T> sort(Collection<T> coll, Comparator<T> comparator) {
        return coll.stream().sorted(comparator).collect(toList());
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        }, LinkedHashMap::new);
    }

    public static <T, R> R tryDo(T t, Function<T, R> func) {
        try {
            return func.apply(t);
        } catch (Exception e) {
            e.printStackTrace(); // for log
            throw new RuntimeException(e.getCause());
        }
    }

    public static <T> void tryDo(T t, Consumer<T> func) {
        try {
            func.accept(t);
        } catch (Exception e) {
            e.printStackTrace(); // for log
            throw new RuntimeException(e.getCause());
        }
    }

    public static void tryDo(Supplier func) {
        try {
            func.get();
        } catch (Exception e) {
            e.printStackTrace(); // for log
            throw new RuntimeException(e.getCause());
        }
    }
//    public static void asynchronous(Runnable func) {
//        try {
//            Executors.defaultCachedThreadPool.execute(func);
//        } catch (Exception e) {
//            e.printStackTrace(); // for log
//            throw new RuntimeException(e.getCause());
//        }
//    }
}
