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

package io.reactivex.flowable.internal.operators;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;
import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.exceptions.MissingBackpressureException;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.common.internal.utils.ExceptionHelper;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.SubscriberFusion;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FlowableFlattenIterableTest {

    @Test
    public void normal0() {

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 2)
        .reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return Math.max(a, b);
            }
        })

                .flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(v, v + 1);
            }
        })
        .subscribe(ts);

        ts.assertValues(2, 3)
        .assertNoErrors()
        .assertComplete();
    }

    final Function1<Integer, Iterable<Integer>> mapper = new Function1<Integer, Iterable<Integer>>() {
        @Override
        public Iterable<Integer> invoke(Integer v) {
            return Arrays.asList(v, v + 1);
        }
    };

    @Test
    public void normal() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5).concatMapIterable(mapper)
        .subscribe(ts);

        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalViaFlatMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5).flatMapIterable(mapper)
        .subscribe(ts);

        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);

        Flowable.range(1, 5).concatMapIterable(mapper)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(2);

        ts.assertValues(1, 2, 2);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(7);

        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void longRunning() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        int n = 1000 * 1000;

        Flowable.range(1, n).concatMapIterable(mapper)
        .subscribe(ts);

        ts.assertValueCount(n * 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void asIntermediate() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        int n = 1000 * 1000;

        Flowable.range(1, n).concatMapIterable(mapper).concatMap(new Function1<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Integer v) {
                return Flowable.just(v);
            }
        })
        .subscribe(ts);

        ts.assertValueCount(n * 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void just() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.just(1).concatMapIterable(mapper)
        .subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void justHidden() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.just(1).hide().concatMapIterable(mapper)
        .subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void empty() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.<Integer>empty().concatMapIterable(mapper)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void error() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.<Integer>just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .concatMapIterable(mapper)
        .subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorHasNextThrowsImmediately() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        throw new TestException();
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };

        Flowable.range(1, 2)
                .concatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return it;
            }
        })
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorHasNextThrowsImmediatelyJust() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        throw new TestException();
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };

        Flowable.just(1)
                .concatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return it;
            }
        })
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorHasNextThrowsSecondCall() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;
                    @Override
                    public boolean hasNext() {
                        if (++count >= 2) {
                            throw new TestException();
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };

        Flowable.range(1, 2)
                .concatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return it;
            }
        })
        .subscribe(ts);

        ts.assertValue(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorNextThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Integer next() {
                        throw new TestException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };

        Flowable.range(1, 2)
                .concatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return it;
            }
        })
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorNextThrowsAndUnsubscribes() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Integer next() {
                        throw new TestException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps
                .concatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return it;
            }
        })
        .subscribe(ts);

        ps.onNext(1);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();

        Assert.assertFalse("PublishProcessor has Subscribers?!", ps.hasSubscribers());
    }

    @Test
    public void mixture() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(0, 1000)
                .concatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return (v % 2) == 0 ? Collections.singleton(1) : Collections.<Integer>emptySet();
            }
        })
        .subscribe(ts);

        ts.assertValueCount(500);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void emptyInnerThenSingleBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1);

        Flowable.range(1, 2)
                .concatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return v == 2 ? Collections.singleton(1) : Collections.<Integer>emptySet();
            }
        })
        .subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void manyEmptyInnerThenSingleBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1);

        Flowable.range(1, 1000)
                .concatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return v == 1000 ? Collections.singleton(1) : Collections.<Integer>emptySet();
            }
        })
        .subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void hasNextIsNotCalledAfterChildUnsubscribedOnNext() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final AtomicInteger counter = new AtomicInteger();

        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        counter.getAndIncrement();
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps
                .concatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return it;
            }
        })
        .take(1)
        .subscribe(ts);

        ps.onNext(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();

        Assert.assertFalse("PublishProcessor has Subscribers?!", ps.hasSubscribers());
        Assert.assertEquals(1, counter.get());
    }

    @Test
    public void normalPrefetchViaFlatMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5).flatMapIterable(mapper, 2)
        .subscribe(ts);

        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void withResultSelectorMaxConcurrent() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.range(1, 5)
                .flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Collections.singletonList(1);
            }
        }, new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return a * 10 + b;
            }
        }, 2)
        .subscribe(ts)
        ;

        ts.assertValues(11, 21, 31, 41, 51);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void flatMapIterablePrefetch() {
        Flowable.just(1, 2)
                .flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer t) {
                return Arrays.asList(t * 10);
            }
        }, 1)
        .test()
        .assertResult(10, 20);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().flatMapIterable(new Function1<Object, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Object v) {
                return Arrays.asList(10, 20);
            }
        }));
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Integer>, Object>() {
            @Override
            public Object invoke(Flowable<Integer> o) {
                return o.flatMapIterable(new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        return Arrays.asList(10, 20);
                    }
                });
            }
        }, false, 1, 1, 10, 20);
    }

    @Test
    public void callableThrows() {
        Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        })
        .flatMapIterable(Functions.justFunction(Arrays.asList(1, 2, 3)))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusionMethods() {
        Flowable.just(1, 2)
        .flatMapIterable(Functions.justFunction(Arrays.asList(1, 2, 3)))
        .subscribe(new RelaxedSubscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                @SuppressWarnings("unchecked")
                FusedQueueSubscription<Integer> qs = (FusedQueueSubscription<Integer>)s;

                assertEquals(FusedQueueSubscription.SYNC, qs.requestFusion(FusedQueueSubscription.ANY));

                try {
                    assertFalse("Source reports being empty!", qs.isEmpty());

                    assertEquals(1, qs.poll().intValue());

                    assertFalse("Source reports being empty!", qs.isEmpty());

                    assertEquals(2, qs.poll().intValue());

                    assertFalse("Source reports being empty!", qs.isEmpty());

                    qs.clear();

                    assertTrue("Source reports not empty!", qs.isEmpty());

                    assertNull(qs.poll());
                } catch (Throwable ex) {
                    throw ExceptionHelper.wrapOrThrow(ex);
                }
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void smallPrefetch() {
        Flowable.just(1, 2, 3)
        .flatMapIterable(Functions.justFunction(Arrays.asList(1, 2, 3)), 1)
        .test()
        .assertResult(1, 2, 3, 1, 2, 3, 1, 2, 3);
    }

    @Test
    public void smallPrefetch2() {
        Flowable.just(1, 2, 3).hide()
        .flatMapIterable(Functions.justFunction(Collections.emptyList()), 1)
        .test()
        .assertResult();
    }

    @Test
    public void mixedInnerSource() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        Flowable.just(1, 2, 3)
                .flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                if ((v & 1) == 0) {
                    return Collections.emptyList();
                }
                return Arrays.asList(1, 2);
            }
        })
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.SYNC)
        .assertResult(1, 2, 1, 2);
    }

    @Test
    public void mixedInnerSource2() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        Flowable.just(1, 2, 3)
                .flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                if ((v & 1) == 1) {
                    return Collections.emptyList();
                }
                return Arrays.asList(1, 2);
            }
        })
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.SYNC)
        .assertResult(1, 2);
    }

    @Test
    public void fusionRejected() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        Flowable.just(1, 2, 3).hide()
                .flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(1, 2);
            }
        })
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.NONE)
        .assertResult(1, 2, 1, 2, 1, 2);
    }

    @Test
    public void fusedIsEmptyWithEmptySource() {
        Flowable.just(1, 2, 3)
                .flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                if ((v & 1) == 0) {
                    return Collections.emptyList();
                }
                return Arrays.asList(v);
            }
        })
        .subscribe(new RelaxedSubscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                @SuppressWarnings("unchecked")
                FusedQueueSubscription<Integer> qs = (FusedQueueSubscription<Integer>)s;

                assertEquals(FusedQueueSubscription.SYNC, qs.requestFusion(FusedQueueSubscription.ANY));

                try {
                    assertFalse("Source reports being empty!", qs.isEmpty());

                    assertEquals(1, qs.poll().intValue());

                    assertFalse("Source reports being empty!", qs.isEmpty());

                    assertEquals(3, qs.poll().intValue());

                    assertTrue("Source reports being non-empty!", qs.isEmpty());
                } catch (Throwable ex) {
                    throw ExceptionHelper.wrapOrThrow(ex);
                }
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void fusedSourceCrash() {
        Flowable.range(1, 3)
                .map(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                throw new TestException();
            }
        })
        .flatMapIterable(Functions.justFunction(Collections.emptyList()), 1)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void take() {
        Flowable.range(1, 3)
        .flatMapIterable(Functions.justFunction(Arrays.asList(1)), 1)
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void overflowSource() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onNext(2);
                s.onNext(3);
            }
        }
        .flatMapIterable(Functions.justFunction(Arrays.asList(1)), 1)
        .test(0L)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void oneByOne() {
        Flowable.range(1, 3).hide()
        .flatMapIterable(Functions.justFunction(Arrays.asList(1)), 1)
        .rebatchRequests(1)
        .test()
        .assertResult(1, 1, 1);
    }

    @Test
    public void cancelAfterHasNext() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 3).hide()
                .flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return new Iterable<Integer>() {
                    int count;
                    @Override
                    public Iterator<Integer> iterator() {
                        return new Iterator<Integer>() {

                            @Override
                            public boolean hasNext() {
                                if (++count == 2) {
                                    ts.cancel();
                                    ts.onComplete();
                                }
                                return true;
                            }

                            @Override
                            public Integer next() {
                                return 1;
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        })
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void doubleShare() {
        Iterable<Integer> it = Flowable.range(1, 300).blockingIterable();
            Flowable.just(it, it)
            .flatMapIterable(Functions.<Iterable<Integer>>identity())
            .share()
            .share()
            .count()
            .test()
            .assertResult(600L);
    }

    @Test
    public void multiShare() {
        Iterable<Integer> it = Flowable.range(1, 300).blockingIterable();
        for (int i = 0; i < 5; i++) {
            Flowable<Integer> f = Flowable.just(it, it)
            .flatMapIterable(Functions.<Iterable<Integer>>identity());

            for (int j = 0; j < i; j++) {
                f = f.share();
            }

            f
            .count()
            .test()
            .withTag("Share: " + i)
            .assertResult(600L);
        }
    }

    @Test
    public void multiShareHidden() {
        Iterable<Integer> it = Flowable.range(1, 300).blockingIterable();
        for (int i = 0; i < 5; i++) {
            Flowable<Integer> f = Flowable.just(it, it)
            .flatMapIterable(Functions.<Iterable<Integer>>identity())
            .hide();

            for (int j = 0; j < i; j++) {
                f = f.share();
            }

            f
            .count()
            .test()
            .withTag("Share: " + i)
            .assertResult(600L);
        }
    }
}
