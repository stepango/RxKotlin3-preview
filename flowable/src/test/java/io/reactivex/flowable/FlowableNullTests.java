/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.flowable;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.Emitter;
import io.reactivex.common.Notification;
import io.reactivex.common.Scheduler;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.processors.AsyncProcessor;
import io.reactivex.flowable.processors.BehaviorProcessor;
import io.reactivex.flowable.processors.FlowableProcessor;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.processors.ReplayProcessor;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.fail;

/**
 * Verifies the operators handle null values properly by emitting/throwing NullPointerExceptions.
 */
public class FlowableNullTests {

    Flowable<Integer> just1 = Flowable.just(1);

    //***********************************************************
    // Static methods
    //***********************************************************

    @Test(expected = NullPointerException.class)
    public void ambVarargsNull() {
        Flowable.ambArray((Publisher<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void ambVarargsOneIsNull() {
        Flowable.ambArray(Flowable.never(), null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void ambIterableNull() {
        Flowable.amb((Iterable<Publisher<Object>>)null);
    }

    @Test
    public void ambIterableIteratorNull() {
        Flowable.amb(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }).test().assertError(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterableOneIsNull() {
        Flowable.amb(Arrays.asList(Flowable.never(), null))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestVarargsNull() {
        Flowable.combineLatestDelayError(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, (Publisher<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestVarargsOneIsNull() {
        Flowable.combineLatestDelayError(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, Flowable.never(), null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableNull() {
        Flowable.combineLatestDelayError((Iterable<Publisher<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableIteratorNull() {
        Flowable.combineLatestDelayError(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestIterableOneIsNull() {
        Flowable.combineLatestDelayError(Arrays.asList(Flowable.never(), null), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestVarargsFunctionNull() {
        Flowable.combineLatestDelayError(null, Flowable.never());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestVarargsFunctionReturnsNull() {
        Flowable.combineLatestDelayError(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }, just1).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestIterableFunctionNull() {
        Flowable.combineLatestDelayError(Arrays.asList(just1), null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestIterableFunctionReturnsNull() {
        Flowable.combineLatestDelayError(Arrays.asList(just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableNull() {
        Flowable.concat((Iterable<Publisher<Object>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Flowable.concat(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void concatIterableOneIsNull() {
        Flowable.concat(Arrays.asList(just1, null)).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatPublisherNull() {
        Flowable.concat((Publisher<Publisher<Object>>)null);

    }

    @Test(expected = NullPointerException.class)
    public void concatArrayNull() {
        Flowable.concatArray((Publisher<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void concatArrayOneIsNull() {
        Flowable.concatArray(just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void createNull() {
        Flowable.unsafeCreate(null);
    }

    @Test(expected = NullPointerException.class)
    public void deferFunctionNull() {
        Flowable.defer(null);
    }

    @Test(expected = NullPointerException.class)
    public void deferFunctionReturnsNull() {
        Flowable.defer(new Callable<Publisher<Object>>() {
            @Override
            public Publisher<Object> call() {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void errorFunctionNull() {
        Flowable.error((Callable<Throwable>)null);
    }

    @Test(expected = NullPointerException.class)
    public void errorFunctionReturnsNull() {
        Flowable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void errorThrowableNull() {
        Flowable.error((Throwable)null);
    }

    @Test(expected = NullPointerException.class)
    public void fromArrayNull() {
        Flowable.fromArray((Object[])null);
    }

    @Test(expected = NullPointerException.class)
    public void fromArrayOneIsNull() {
        Flowable.fromArray(1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableNull() {
        Flowable.fromCallable(null);
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableReturnsNull() {
        Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureNull() {
        Flowable.fromFuture(null);
    }

    @Test
    public void fromFutureReturnsNull() {
        FutureTask<Object> f = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        f.run();

        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        Flowable.fromFuture(f).subscribe(ts);
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedFutureNull() {
        Flowable.fromFuture(null, 1, TimeUnit.SECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedUnitNull() {
        Flowable.fromFuture(new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null), 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedSchedulerNull() {
        Flowable.fromFuture(new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null), 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedReturnsNull() {
      FutureTask<Object> f = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        f.run();
        Flowable.fromFuture(f, 1, TimeUnit.SECONDS).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureSchedulerNull() {
        Flowable.fromFuture(new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null), null);
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableNull() {
        Flowable.fromIterable(null);
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableIteratorNull() {
        Flowable.fromIterable(new Iterable<Object>() {
            @Override
            public Iterator<Object> iterator() {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableValueNull() {
        Flowable.fromIterable(Arrays.asList(1, null)).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromPublisherNull() {
        Flowable.fromPublisher(null);
    }

    @Test(expected = NullPointerException.class)
    public void generateConsumerNull() {
        Flowable.generate(null);
    }

    @Test(expected = NullPointerException.class)
    public void generateConsumerEmitsNull() {
        Flowable.generate(new Function1<Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Emitter<Object> s) {
                s.onNext(null);
                return Unit.INSTANCE;
            }
        }).blockingLast();
    }

    //TODO reimplement
//    @Test(expected = NullPointerException.class)
//    public void generateStateConsumerInitialStateNull() {
//        Function2<Unit, Emitter<Integer>, Unit> generator = new Function2<Integer, Emitter<Integer>, Unit>() {
//            @Override
//            public Unit invoke(Integer s, Emitter<Integer> o) {
//                o.onNext(1);
//                return Unit.INSTANCE;
//            }
//        };
//        Flowable.generate(null, generator);
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void generateStateFunctionInitialStateNull() {
//        Flowable.generate(null, new Function2<Object, Emitter<Object>, Object>() {
//            @Override
//            public Object invoke(Object s, Emitter<Object> o) {
//                o.onNext(1); return s;
//            }
//        });
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void generateStateConsumerNull() {
//        Flowable.generate(new Callable<Integer>() {
//            @Override
//            public Integer call() {
//                return 1;
//            }
//        }, (Function2<Integer, Emitter<Object>, kotlin.Unit>) null);
//    }
//
//    @Test
//    public void generateConsumerStateNullAllowed() {
//        Function2<Integer, Emitter<Integer>, kotlin.Unit> generator = new Function2<Integer, Emitter<Integer>, kotlin.Unit>() {
//            @Override
//            public Unit invoke(Integer s, Emitter<Integer> o) {
//                o.onComplete();
//                return Unit.INSTANCE;
//            }
//        };
//        Flowable.generate(new Callable<Integer>() {
//            @Override
//            public Integer call() {
//                return null;
//            }
//        }, generator).blockingSubscribe();
//    }

    @Test
    public void generateFunctionStateNullAllowed() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }, new Function2<Object, Emitter<Object>, Object>() {
            @Override
            public Object invoke(Object s, Emitter<Object> o) {
                o.onComplete(); return s;
            }
        }).blockingSubscribe();
    }

    //TODO reimplement
//    @Test(expected = NullPointerException.class)
//    public void generateConsumerDisposeNull() {
//        Function2<Integer, Emitter<Integer>, kotlin.Unit> generator = new Function2<Integer, Emitter<Integer>, kotlin.Unit>() {
//            @Override
//            public Unit invoke(Integer s, Emitter<Integer> o) {
//                o.onNext(1);
//                return Unit.INSTANCE;
//            }
//        };
//        Flowable.generate(new Callable<Integer>() {
//            @Override
//            public Integer call() {
//                return 1;
//            }
//        }, generator, null);
//    }

    @Test(expected = NullPointerException.class)
    public void generateFunctionDisposeNull() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function2<Object, Emitter<Object>, Object>() {
            @Override
            public Object invoke(Object s, Emitter<Object> o) {
                o.onNext(1); return s;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalUnitNull() {
        Flowable.interval(1, null);
    }

    public void intervalSchedulerNull() {
        Flowable.interval(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalPeriodUnitNull() {
        Flowable.interval(1, 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalPeriodSchedulerNull() {
        Flowable.interval(1, 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalRangeUnitNull() {
        Flowable.intervalRange(1,1, 1, 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalRangeSchedulerNull() {
        Flowable.intervalRange(1, 1, 1, 1, TimeUnit.SECONDS, null);
    }

    @Test
    public void justNull() throws Exception {
        @SuppressWarnings("rawtypes")
        Class<Flowable> clazz = Flowable.class;
        for (int argCount = 1; argCount < 10; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount];
                Arrays.fill(params, Object.class);

                Object[] values = new Object[argCount];
                Arrays.fill(values, 1);
                values[argNull - 1] = null;

                Method m = clazz.getMethod("just", params);

                try {
                    m.invoke(null, values);
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableNull() {
        Flowable.merge((Iterable<Publisher<Object>>)null, 128, 128);
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableIteratorNull() {
        Flowable.merge(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, 128, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeIterableOneIsNull() {
        Flowable.merge(Arrays.asList(just1, null), 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeArrayNull() {
        Flowable.mergeArray(128, 128, (Publisher<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeArrayOneIsNull() {
        Flowable.mergeArray(128, 128, just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableNull() {
        Flowable.mergeDelayError((Iterable<Publisher<Object>>)null, 128, 128);
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableIteratorNull() {
        Flowable.mergeDelayError(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, 128, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableOneIsNull() {
        Flowable.mergeDelayError(Arrays.asList(just1, null), 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorArrayNull() {
        Flowable.mergeArrayDelayError(128, 128, (Publisher<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorArrayOneIsNull() {
        Flowable.mergeArrayDelayError(128, 128, just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void sequenceEqualFirstNull() {
        Flowable.sequenceEqual(null, just1);
    }

    @Test(expected = NullPointerException.class)
    public void sequenceEqualSecondNull() {
        Flowable.sequenceEqual(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void sequenceEqualComparatorNull() {
        Flowable.sequenceEqual(just1, just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void switchOnNextNull() {
        Flowable.switchOnNext(null);
    }

    @Test(expected = NullPointerException.class)
    public void timerUnitNull() {
        Flowable.timer(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timerSchedulerNull() {
        Flowable.timer(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void usingResourceSupplierNull() {
        Flowable.using(null, new Function1<Object, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Object d) {
                return just1;
            }
        }, Functions.emptyConsumer());
    }

    @Test(expected = NullPointerException.class)
    public void usingFlowableSupplierNull() {
        Flowable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null, Functions.emptyConsumer());
    }

    @Test(expected = NullPointerException.class)
    public void usingFlowableSupplierReturnsNull() {
        Flowable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function1<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Object d) {
                return null;
            }
        }, Functions.emptyConsumer()).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void usingDisposeNull() {
        Flowable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function1<Object, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Object d) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableNull() {
        Flowable.zip((Iterable<Publisher<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableIteratorNull() {
        Flowable.zip(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableFunctionNull() {
        Flowable.zip(Arrays.asList(just1, just1), null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableFunctionReturnsNull() {
        Flowable.zip(Arrays.asList(just1, just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipPublisherNull() {
        Flowable.zip((Publisher<Publisher<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipPublisherFunctionNull() {
        Flowable.zip((Flowable.just(just1)), null);
    }

    @Test(expected = NullPointerException.class)
    public void zipPublisherFunctionReturnsNull() {
        Flowable.zip((Flowable.just(just1)), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2Null() {
        Flowable.zipIterable((Iterable<Publisher<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return 1;
            }
        }, true, 128);
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2IteratorNull() {
        Flowable.zipIterable(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return 1;
            }
        }, true, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterable2FunctionNull() {
        Flowable.zipIterable(Arrays.asList(just1, just1), null, true, 128);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterable2FunctionReturnsNull() {
        Flowable.zipIterable(Arrays.asList(just1, just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return null;
            }
        }, true, 128).blockingLast();
    }

    //*************************************************************
    // Instance methods
    //*************************************************************

    @Test(expected = NullPointerException.class)
    public void allPredicateNull() {
        just1.all(null);
    }

    @Test(expected = NullPointerException.class)
    public void ambWithNull() {
        just1.ambWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void anyPredicateNull() {
        just1.any(null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferSupplierNull() {
        just1.buffer(1, 1, (Callable<List<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferSupplierReturnsNull() {
        just1.buffer(1, 1, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferTimedUnitNull() {
        just1.buffer(1L, 1L, null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferTimedSchedulerNull() {
        just1.buffer(1L, 1L, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferTimedSupplierNull() {
        just1.buffer(1L, 1L, TimeUnit.SECONDS, Schedulers.single(), null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferTimedSupplierReturnsNull() {
        just1.buffer(1L, 1L, TimeUnit.SECONDS, Schedulers.single(), new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferOpenCloseOpenNull() {
        just1.buffer(null, new Function1<Object, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Object o) {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void bufferOpenCloseCloseNull() {
        just1.buffer(just1, (Function1<Integer, Publisher<Object>>) null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferOpenCloseCloseReturnsNull() {
        just1.buffer(just1, new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundaryNull() {
        just1.buffer((Publisher<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplierNull() {
        just1.buffer(just1, (Callable<List<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplierReturnsNull() {
        just1.buffer(just1, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplier2Null() {
        just1.buffer((Callable<Publisher<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplier2ReturnsNull() {
        just1.buffer(new Callable<Publisher<Object>>() {
            @Override
            public Publisher<Object> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplier2SupplierNull() {
        just1.buffer(new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplier2SupplierReturnsNull() {
        just1.buffer(new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                return just1;
            }
        }, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void castNull() {
        just1.cast(null);
    }

    @Test(expected = NullPointerException.class)
    public void collectInitialSupplierNull() {
        just1.collect((Callable<Integer>) null, new Function2<Integer, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer a, Integer b) {
                return Unit.INSTANCE;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void collectInitialSupplierReturnsNull() {
        just1.collect(new Callable<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }, new Function2<Object, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Object a, Integer b) {
                return Unit.INSTANCE;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void collectInitialCollectorNull() {
        just1.collect(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void collectIntoInitialNull() {
        just1.collectInto(null, new Function2<Object, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Object a, Integer b) {
                return Unit.INSTANCE;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void collectIntoCollectorNull() {
        just1.collectInto(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void composeNull() {
        just1.compose(null);
    }

    @Test(expected = NullPointerException.class)
    public void concatMapNull() {
        just1.concatMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void concatMapReturnsNull() {
        just1.concatMap(new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatMapIterableNull() {
        just1.concatMapIterable(null);
    }

    @Test(expected = NullPointerException.class)
    public void concatMapIterableReturnNull() {
        just1.concatMapIterable(new Function1<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatMapIterableIteratorNull() {
        just1.concatMapIterable(new Function1<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> invoke(Integer v) {
                return new Iterable<Object>() {
                    @Override
                    public Iterator<Object> iterator() {
                        return null;
                    }
                };
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatWithNull() {
        just1.concatWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void containsNull() {
        just1.contains(null);
    }

    @Test(expected = NullPointerException.class)
    public void debounceFunctionNull() {
        just1.debounce(null);
    }

    @Test(expected = NullPointerException.class)
    public void debounceFunctionReturnsNull() {
        just1.debounce(new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void debounceTimedUnitNull() {
        just1.debounce(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void debounceTimedSchedulerNull() {
        just1.debounce(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void defaultIfEmptyNull() {
        just1.defaultIfEmpty(null);
    }

    @Test(expected = NullPointerException.class)
    public void delayWithFunctionNull() {
        just1.delay(null);
    }

    @Test(expected = NullPointerException.class)
    public void delayWithFunctionReturnsNull() {
        just1.delay(new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void delayTimedUnitNull() {
        just1.delay(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void delayTimedSchedulerNull() {
        just1.delay(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySubscriptionTimedUnitNull() {
        just1.delaySubscription(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySubscriptionTimedSchedulerNull() {
        just1.delaySubscription(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySubscriptionSupplierNull() {
        just1.delaySubscription((Publisher<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySubscriptionFunctionNull() {
        just1.delaySubscription((Publisher<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void delayBothInitialSupplierNull() {
        just1.delay(null, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void delayBothInitialSupplierReturnsNull() {
        just1.delay(null, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return just1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void delayBothItemSupplierNull() {
        just1.delay(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void delayBothItemSupplierReturnsNull() {
        just1.delay(just1, new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void distinctFunctionNull() {
        just1.distinct(null);
    }

    @Test(expected = NullPointerException.class)
    public void distinctSupplierNull() {
        just1.distinct(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void distinctSupplierReturnsNull() {
        just1.distinct(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void distinctFunctionReturnsNull() {
        just1.distinct(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void distinctUntilChangedFunctionNull() {
        just1.distinctUntilChanged((Function1<Integer, Integer>) null);
    }

    @Test(expected = NullPointerException.class)
    public void distinctUntilChangedBiPredicateNull() {
        just1.distinctUntilChanged((Function2<Integer, Integer, Boolean>) null);
    }

    @Test
    public void distinctUntilChangedFunctionReturnsNull() {
        Flowable.range(1, 2).distinctUntilChanged(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).test().assertResult(1);
    }

    @Test(expected = NullPointerException.class)
    public void doOnCancelNull() {
        just1.doOnCancel(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnCompleteNull() {
        just1.doOnComplete(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnEachSupplierNull() {
        just1.doOnEach((Function1<Notification<Integer>, kotlin.Unit>) null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnEachSubscriberNull() {
        just1.doOnEach((Subscriber<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnErrorNull() {
        just1.doOnError(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnLifecycleOnSubscribeNull() {
        just1.doOnLifecycle(null, new Function1<Long, Unit>() {
            @Override
            public Unit invoke(Long v) {
                return Unit.INSTANCE;
            }
        }, new Function0() {
            @Override
            public kotlin.Unit invoke() {
                return Unit.INSTANCE;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void doOnLifecycleOnRequestNull() {
        just1.doOnLifecycle(new Function1<Subscription, kotlin.Unit>() {
            @Override
            public Unit invoke(Subscription s) {
                return Unit.INSTANCE;
            }
        }, null, new Function0() {
            @Override
            public kotlin.Unit invoke() {
                return Unit.INSTANCE;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void doOnLifecycleOnCancelNull() {
        just1.doOnLifecycle(new Function1<Subscription, kotlin.Unit>() {
            @Override
            public Unit invoke(Subscription s) {
                return Unit.INSTANCE;
            }
        }, new Function1<Long, Unit>() {
            @Override
            public Unit invoke(Long v) {
                return Unit.INSTANCE;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnNextNull() {
        just1.doOnNext(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnRequestNull() {
        just1.doOnRequest(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnSubscribeNull() {
        just1.doOnSubscribe(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnTerminatedNull() {
        just1.doOnTerminate(null);
    }

    @Test(expected = NullPointerException.class)
    public void elementAtNull() {
        just1.elementAt(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void filterNull() {
        just1.filter(null);
    }

    @Test(expected = NullPointerException.class)
    public void doAfterTerminateNull() {
        just1.doAfterTerminate(null);
    }

    @Test(expected = NullPointerException.class)
    public void firstNull() {
        just1.first(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNull() {
        just1.flatMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapFunctionReturnsNull() {
        just1.flatMap(new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnNextNull() {
        just1.flatMap(null, new Function1<Throwable, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Throwable e) {
                return just1;
            }
        }, new Callable<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> call() {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnNextReturnsNull() {
        just1.flatMap(new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return null;
            }
        }, new Function1<Throwable, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Throwable e) {
                return just1;
            }
        }, new Callable<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> call() {
                return just1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnErrorNull() {
        just1.flatMap(new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return just1;
            }
        }, null, new Callable<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> call() {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnErrorReturnsNull() {
        Flowable.error(new TestException()).flatMap(new Function1<Object, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Object v) {
                return just1;
            }
        }, new Function1<Throwable, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Throwable e) {
                return null;
            }
        }, new Callable<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> call() {
                return just1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnCompleteNull() {
        just1.flatMap(new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return just1;
            }
        }, new Function1<Throwable, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Throwable e) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnCompleteReturnsNull() {
        just1.flatMap(new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return just1;
            }
        }, new Function1<Throwable, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Throwable e) {
                return just1;
            }
        }, new Callable<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerMapperNull() {
        just1.flatMap(null, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerMapperReturnsNull() {
        just1.flatMap(new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerCombinerNull() {
        just1.flatMap(new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerCombinerReturnsNull() {
        just1.flatMap(new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return just1;
            }
        }, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperNull() {
        just1.flatMapIterable(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperReturnsNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperIteratorNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> invoke(Integer v) {
                return new Iterable<Object>() {
                    @Override
                    public Iterator<Object> iterator() {
                        return null;
                    }
                };
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperIterableOneNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(1, null);
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableCombinerNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(1);
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableCombinerReturnsNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(1);
            }
        }, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void forEachNull() {
        just1.forEach(null);
    }

    @Test(expected = NullPointerException.class)
    public void forEachWhileNull() {
        just1.forEachWhile(null);
    }

    @Test(expected = NullPointerException.class)
    public void forEachWhileOnErrorNull() {
        just1.forEachWhile(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void forEachWhileOnCompleteNull() {
        just1.forEachWhile(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }, new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                return Unit.INSTANCE;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void groupByNull() {
        just1.groupBy(null);
    }

    public void groupByKeyNull() {
        just1.groupBy(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void groupByValueNull() {
        just1.groupBy(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void groupByValueReturnsNull() {
        just1.groupBy(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void lastNull() {
        just1.last(null);
    }

    @Test(expected = NullPointerException.class)
    public void liftNull() {
        just1.lift(null);
    }

    @Test(expected = NullPointerException.class)
    public void liftReturnsNull() {
        just1.lift(new FlowableOperator<Object, Integer>() {
            @Override
            public Subscriber<? super Integer> apply(Subscriber<? super Object> s) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void mapNull() {
        just1.map(null);
    }

    @Test(expected = NullPointerException.class)
    public void mapReturnsNull() {
        just1.map(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void mergeWithNull() {
        just1.mergeWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void observeOnNull() {
        just1.observeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void ofTypeNull() {
        just1.ofType(null);
    }

    @Test(expected = NullPointerException.class)
    public void onBackpressureBufferOverflowNull() {
        just1.onBackpressureBuffer(10, null);
    }

    @Test(expected = NullPointerException.class)
    public void onBackpressureDropActionNull() {
        just1.onBackpressureDrop(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextFunctionNull() {
        just1.onErrorResumeNext((Function1<Throwable, Publisher<Integer>>) null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextFunctionReturnsNull() {
        Flowable.error(new TestException()).onErrorResumeNext(new Function1<Throwable, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Throwable e) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextPublisherNull() {
        just1.onErrorResumeNext((Publisher<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnFunctionNull() {
        just1.onErrorReturn(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnValueNull() {
        just1.onErrorReturnItem(null);
    }

    @Test
    public void onErrorReturnFunctionReturnsNull() {
        try {
            Flowable.error(new TestException()).onErrorReturn(new Function1<Throwable, Object>() {
                @Override
                public Object invoke(Throwable e) {
                    return null;
                }
            }).blockingSubscribe();
            fail("Should have thrown");
        } catch (CompositeException ex) {
            List<Throwable> errors = TestCommonHelper.compositeList(ex);

            TestCommonHelper.assertError(errors, 0, TestException.class);
            TestCommonHelper.assertError(errors, 1, NullPointerException.class, "The valueSupplier returned a null value");
        }
    }

    @Test(expected = NullPointerException.class)
    public void onExceptionResumeNext() {
        just1.onExceptionResumeNext(null);
    }

    @Test(expected = NullPointerException.class)
    public void publishFunctionNull() {
        just1.publish(null);
    }

    @Test(expected = NullPointerException.class)
    public void publishFunctionReturnsNull() {
        just1.publish(new Function1<Flowable<Integer>, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Flowable<Integer> v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void reduceFunctionNull() {
        just1.reduce(null);
    }

    @Test(expected = NullPointerException.class)
    public void reduceFunctionReturnsNull() {
        Flowable.just(1, 1).reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void reduceSeedNull() {
        just1.reduce(null, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void reduceSeedFunctionNull() {
        just1.reduce(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void reduceSeedFunctionReturnsNull() {
        just1.reduce(1, new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void reduceWithSeedNull() {
        just1.reduceWith(null, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void reduceWithSeedReturnsNull() {
        just1.reduceWith(new Callable<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void repeatUntilNull() {
        just1.repeatUntil(null);
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenNull() {
        just1.repeatWhen(null);
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenFunctionReturnsNull() {
        just1.repeatWhen(new Function1<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Flowable<Object> v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorNull() {
        just1.replay((Function1<Flowable<Integer>, Flowable<Integer>>) null);
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorReturnsNull() {
        just1.replay(new Function1<Flowable<Integer>, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Flowable<Integer> o) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedSelectorNull() {
        just1.replay((Function1<Flowable<Integer>, Flowable<Integer>>) null, 1, 1, TimeUnit.SECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedSelectorReturnsNull() {
        just1.replay(new Function1<Flowable<Integer>, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Flowable<Integer> v) {
                return null;
            }
        }, 1, 1, TimeUnit.SECONDS).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySchedulerNull() {
        just1.replay((Scheduler)null);
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedUnitNull() {
        just1.replay(new Function1<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Flowable<Integer> v) {
                return v;
            }
        }, 1, 1, null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedSchedulerNull() {
        just1.replay(new Function1<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Flowable<Integer> v) {
                return v;
            }
        }, 1, 1, TimeUnit.SECONDS, null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedSelectorNull() {
        just1.replay(null, 1, TimeUnit.SECONDS, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedSelectorReturnsNull() {
        just1.replay(new Function1<Flowable<Integer>, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Flowable<Integer> v) {
                return null;
            }
        }, 1, TimeUnit.SECONDS, Schedulers.single()).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorTimeBoundedUnitNull() {
        just1.replay(new Function1<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Flowable<Integer> v) {
                return v;
            }
        }, 1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorTimeBoundedSchedulerNull() {
        just1.replay(new Function1<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Flowable<Integer> v) {
                return v;
            }
        }, 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeSizeBoundedUnitNull() {
        just1.replay(1, 1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeSizeBoundedSchedulerNull() {
        just1.replay(1, 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void replayBufferSchedulerNull() {
        just1.replay(1, (Scheduler)null);
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedUnitNull() {
        just1.replay(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedSchedulerNull() {
        just1.replay(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void retryFunctionNull() {
        just1.retry((Function2<Integer, Throwable, Boolean>) null);
    }

    @Test(expected = NullPointerException.class)
    public void retryCountFunctionNull() {
        just1.retry(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void retryPredicateNull() {
        just1.retry((Function1<Throwable, Boolean>) null);
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenFunctionNull() {
        just1.retryWhen(null);
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenFunctionReturnsNull() {
        Flowable.error(new TestException()).retryWhen(new Function1<Flowable<? extends Throwable>, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Flowable<? extends Throwable> f) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void retryUntil() {
        just1.retryUntil(null);
    }

    @Test(expected = NullPointerException.class)
    public void safeSubscribeNull() {
        just1.safeSubscribe(null);
    }

    @Test(expected = NullPointerException.class)
    public void sampleUnitNull() {
        just1.sample(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void sampleSchedulerNull() {
        just1.sample(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void samplePublisherNull() {
        just1.sample(null);
    }

    @Test(expected = NullPointerException.class)
    public void scanFunctionNull() {
        just1.scan(null);
    }

    @Test(expected = NullPointerException.class)
    public void scanFunctionReturnsNull() {
        Flowable.just(1, 1).scan(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedNull() {
        just1.scan(null, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedFunctionNull() {
        just1.scan(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedFunctionReturnsNull() {
        just1.scan(1, new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierNull() {
        just1.scanWith(null, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierReturnsNull() {
        just1.scanWith(new Callable<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierFunctionNull() {
        just1.scanWith(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierFunctionReturnsNull() {
        just1.scanWith(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void singleNull() {
        just1.single(null);
    }

    @Test(expected = NullPointerException.class)
    public void skipTimedUnitNull() {
        just1.skip(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void skipTimedSchedulerNull() {
        just1.skip(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void skipLastTimedUnitNull() {
        just1.skipLast(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void skipLastTimedSchedulerNull() {
        just1.skipLast(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void skipUntilNull() {
        just1.skipUntil(null);
    }

    @Test(expected = NullPointerException.class)
    public void skipWhileNull() {
        just1.skipWhile(null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithIterableNull() {
        just1.startWith((Iterable<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithIterableIteratorNull() {
        just1.startWith(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void startWithIterableOneNull() {
        just1.startWith(Arrays.asList(1, null)).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void startWithSingleNull() {
        just1.startWith((Integer)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithPublisherNull() {
        just1.startWith((Publisher<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithArrayNull() {
        just1.startWithArray((Integer[])null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithArrayOneNull() {
        just1.startWithArray(1, null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnNextNull() {
        just1.subscribe((Function1<Integer, kotlin.Unit>) null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnErrorNull() {
        just1.subscribe(Functions.emptyConsumer(), null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnCompleteNull() {
        just1.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer(), null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnSubscribeNull() {
        just1.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer(), Functions.EMPTY_ACTION, null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeNull() {
        just1.subscribe((Subscriber<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeNull2() {
        just1.subscribe((RelaxedSubscriber<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnNull() {
        just1.subscribeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void switchIfEmptyNull() {
        just1.switchIfEmpty(null);
    }

    @Test(expected = NullPointerException.class)
    public void switchMapNull() {
        just1.switchMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void switchMapFunctionReturnsNull() {
        just1.switchMap(new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void takeTimedUnitNull() {
        just1.take(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void takeTimedSchedulerNull() {
        just1.take(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void takeLastTimedUnitNull() {
        just1.takeLast(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void takeLastSizeTimedUnitNull() {
        just1.takeLast(1, 1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void takeLastTimedSchedulerNull() {
        just1.takeLast(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void takeLastSizeTimedSchedulerNull() {
        just1.takeLast(1, 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void takeUntilPredicateNull() {
        just1.takeUntil((Function1<Integer, Boolean>) null);
    }

    @Test(expected = NullPointerException.class)
    public void takeUntilPublisherNull() {
        just1.takeUntil((Publisher<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void takeWhileNull() {
        just1.takeWhile(null);
    }

    @Test(expected = NullPointerException.class)
    public void throttleFirstUnitNull() {
        just1.throttleFirst(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void throttleFirstSchedulerNull() {
        just1.throttleFirst(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void throttleLastUnitNull() {
        just1.throttleLast(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void throttleLastSchedulerNull() {
        just1.throttleLast(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void throttleWithTimeoutUnitNull() {
        just1.throttleWithTimeout(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void throttleWithTimeoutSchedulerNull() {
        just1.throttleWithTimeout(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeIntervalUnitNull() {
        just1.timeInterval(null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void timeIntervalSchedulerNull() {
        just1.timeInterval(TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSelectorNull() {
        just1.timeout(null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSelectorReturnsNull() {
        just1.timeout(new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSelectorOtherNull() {
        just1.timeout(new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutUnitNull() {
        just1.timeout(1, null, Schedulers.single(), just1);
    }

    @Test(expected = NullPointerException.class)
    public void timeouOtherNull() {
        just1.timeout(1, TimeUnit.SECONDS, Schedulers.single(), null);
    }

    @Test(expected = NullPointerException.class)
    public void timeouSchedulerNull() {
        just1.timeout(1, TimeUnit.SECONDS, null, just1);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutFirstNull() {
        just1.timeout((Publisher<Integer>) null, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void timeoutFirstItemNull() {
        just1.timeout(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutFirstItemReturnsNull() {
        just1.timeout(just1, new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void timestampUnitNull() {
        just1.timestamp(null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void timestampSchedulerNull() {
        just1.timestamp(TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void toNull() {
        just1.to(null);
    }

    @Test(expected = NullPointerException.class)
    public void toListNull() {
        just1.toList(null);
    }

    @Test(expected = NullPointerException.class)
    public void toListSupplierReturnsNull() {
        just1.toList(new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void toListSupplierReturnsNullSingle() {
        just1.toList(new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void toSortedListNull() {
        just1.toSortedList(null);
    }

    @Test(expected = NullPointerException.class)
    public void toMapKeyNullAllowed() {
        just1.toMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void toMapValueNull() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test
    public void toMapValueSelectorReturnsNull() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void toMapMapSupplierNull() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void toMapMapSupplierReturnsNull() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Callable<Map<Object, Object>>() {
            @Override
            public Map<Object, Object> call() {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapKeyNull() {
        just1.toMultimap(null);
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapValueNull() {
        just1.toMultimap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test
    public void toMultiMapValueSelectorReturnsNullAllowed() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapMapSupplierNull() {
        just1.toMultimap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapSupplierReturnsNull() {
        just1.toMultimap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Callable<Map<Object, Collection<Object>>>() {
            @Override
            public Map<Object, Collection<Object>> call() {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapMapCollectionSupplierNull() {
        just1.toMultimap(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return v;
            }
        }, new Callable<Map<Integer, Collection<Integer>>>() {
            @Override
            public Map<Integer, Collection<Integer>> call() {
                return new HashMap<Integer, Collection<Integer>>();
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapCollectionSupplierReturnsNull() {
        just1.toMultimap(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return v;
            }
        }, new Callable<Map<Integer, Collection<Integer>>>() {
            @Override
            public Map<Integer, Collection<Integer>> call() {
                return new HashMap<Integer, Collection<Integer>>();
            }
        }, new Function1<Integer, Collection<Integer>>() {
            @Override
            public Collection<Integer> invoke(Integer v) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void unsubscribeOnNull() {
        just1.unsubscribeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void windowTimedUnitNull() {
        just1.window(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void windowSizeTimedUnitNull() {
        just1.window(1, null, Schedulers.single(), 1);
    }

    @Test(expected = NullPointerException.class)
    public void windowTimedSchedulerNull() {
        just1.window(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void windowSizeTimedSchedulerNull() {
        just1.window(1, TimeUnit.SECONDS, null, 1);
    }

    @Test(expected = NullPointerException.class)
    public void windowBoundaryNull() {
        just1.window((Publisher<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void windowOpenCloseOpenNull() {
        just1.window(null, new Function1<Object, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Object v) {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void windowOpenCloseCloseNull() {
        just1.window(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void windowOpenCloseCloseReturnsNull() {
        Flowable.never().window(just1, new Function1<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void windowBoundarySupplierNull() {
        just1.window((Callable<Publisher<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void windowBoundarySupplierReturnsNull() {
        just1.window(new Callable<Publisher<Object>>() {
            @Override
            public Publisher<Object> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void withLatestFromOtherNull() {
        just1.withLatestFrom(null, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void withLatestFromCombinerNull() {
        just1.withLatestFrom(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void withLatestFromCombinerReturnsNull() {
        just1.withLatestFrom(just1, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableNull() {
        just1.zipWith((Iterable<Integer>)null, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableCombinerNull() {
        just1.zipWith(Arrays.asList(1), null);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableCombinerReturnsNull() {
        just1.zipWith(Arrays.asList(1), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableIteratorNull() {
        just1.zipWith(new Iterable<Object>() {
            @Override
            public Iterator<Object> iterator() {
                return null;
            }
        }, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableOneIsNull() {
        Flowable.just(1, 2).zipWith(Arrays.asList(1, null), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return 1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithPublisherNull() {
        just1.zipWith((Publisher<Integer>)null, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return 1;
            }
        });
    }


    @Test(expected = NullPointerException.class)
    public void zipWithCombinerNull() {
        just1.zipWith(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithCombinerReturnsNull() {
        just1.zipWith(just1, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    //*********************************************
    // Subject null tests
    //*********************************************

    @Test(expected = NullPointerException.class)
    public void asyncSubjectOnNextNull() {
        FlowableProcessor<Integer> subject = AsyncProcessor.create();
        subject.onNext(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void asyncSubjectOnErrorNull() {
        FlowableProcessor<Integer> subject = AsyncProcessor.create();
        subject.onError(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void behaviorSubjectOnNextNull() {
        FlowableProcessor<Integer> subject = BehaviorProcessor.create();
        subject.onNext(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void behaviorSubjectOnErrorNull() {
        FlowableProcessor<Integer> subject = BehaviorProcessor.create();
        subject.onError(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void publishSubjectOnNextNull() {
        FlowableProcessor<Integer> subject = PublishProcessor.create();
        subject.onNext(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void publishSubjectOnErrorNull() {
        FlowableProcessor<Integer> subject = PublishProcessor.create();
        subject.onError(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaycSubjectOnNextNull() {
        FlowableProcessor<Integer> subject = ReplayProcessor.create();
        subject.onNext(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySubjectOnErrorNull() {
        FlowableProcessor<Integer> subject = ReplayProcessor.create();
        subject.onError(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void serializedcSubjectOnNextNull() {
        FlowableProcessor<Integer> subject = PublishProcessor.<Integer>create().toSerialized();
        subject.onNext(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void serializedSubjectOnErrorNull() {
        FlowableProcessor<Integer> subject = PublishProcessor.<Integer>create().toSerialized();
        subject.onError(null);
        subject.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void doOnLifecycleOnDisposeNull() {
        just1.doOnLifecycle(new Function1<Subscription, kotlin.Unit>() {
            @Override
            public Unit invoke(Subscription s) {
                return Unit.INSTANCE;
            }
        },
                new Function1<Long, Unit>() {
            @Override
            public Unit invoke(Long v) {
                return Unit.INSTANCE;
            }
        },
        null);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithFlowableNull() {
        just1.zipWith((Flowable<Integer>)null, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void unsafeSubscribeNull() {
        just1.subscribe((RelaxedSubscriber<Object>)null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableFunctionReturnsNull() {
        Flowable.combineLatestDelayError(Arrays.asList(just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableFunctionNull() {
        Flowable.combineLatestDelayError(Arrays.asList(just1), null, 128);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorVarargsFunctionNull() {
        Flowable.combineLatestDelayError(null, 128, Flowable.never());
    }

    @Test(expected = NullPointerException.class)
    public void zipFlowableNull() {
        Flowable.zip((Flowable<Flowable<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipFlowableFunctionNull() {
        Flowable.zip((Flowable.just(just1)), null);
    }

    @Test(expected = NullPointerException.class)
    public void zipFlowableFunctionReturnsNull() {
        Flowable.zip((Flowable.just(just1)), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatFlowableNull() {
        Flowable.concat((Flowable<Flowable<Object>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorVarargsNull() {
        Flowable.combineLatestDelayError(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128, (Flowable<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorVarargsOneIsNull() {
        Flowable.combineLatestDelayError(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128, Flowable.never(), null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableNull() {
        Flowable.combineLatestDelayError((Iterable<Flowable<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableIteratorNull() {
        Flowable.combineLatestDelayError(new Iterable<Flowable<Object>>() {
            @Override
            public Iterator<Flowable<Object>> iterator() {
                return null;
            }
        }, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void doOnDisposeNull() {
        just1.doOnCancel(null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableOneIsNull() {
        Flowable.combineLatestDelayError(Arrays.asList(Flowable.never(), null), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void takeUntilFlowableNull() {
        just1.takeUntil((Flowable<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithFlowableNull() {
        just1.startWith((Flowable<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySubscriptionOtherNull() {
        just1.delaySubscription((Flowable<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void sampleFlowableNull() {
        just1.sample(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextFlowableNull() {
        just1.onErrorResumeNext((Flowable<Integer>)null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorVarargsFunctionReturnsNull() {
        Flowable.combineLatestDelayError(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }, 128, just1).blockingLast();
    }
}
