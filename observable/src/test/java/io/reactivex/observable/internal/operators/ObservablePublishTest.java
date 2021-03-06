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

package io.reactivex.observable.internal.operators;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.TestScheduler;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.ConnectableObservable;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.HasUpstreamObservableSource;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObservablePublishTest {

    @Test
    public void testPublish() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        ConnectableObservable<String> o = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(final Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        observer.onNext("one");
                        observer.onComplete();
                    }
                }).start();
            }
        }).publish();

        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Function1<String, kotlin.Unit>() {

            @Override
            public Unit invoke(String v) {
                assertEquals("one", v);
                latch.countDown();
                return Unit.INSTANCE;
            }
        });

        // subscribe again
        o.subscribe(new Function1<String, kotlin.Unit>() {

            @Override
            public Unit invoke(String v) {
                assertEquals("one", v);
                latch.countDown();
                return Unit.INSTANCE;
            }
        });

        Disposable s = o.connect();
        try {
            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("subscriptions did not receive values");
            }
            assertEquals(1, counter.get());
        } finally {
            s.dispose();
        }
    }

    @Test
    public void testBackpressureFastSlow() {
        ConnectableObservable<Integer> is = Observable.range(1, Observable.bufferSize() * 2).publish();
        Observable<Integer> fast = is.observeOn(Schedulers.computation())
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                System.out.println("^^^^^^^^^^^^^ completed FAST");
                return Unit.INSTANCE;
            }
        });

        Observable<Integer> slow = is.observeOn(Schedulers.computation()).map(new Function1<Integer, Integer>() {
            int c;

            @Override
            public Integer invoke(Integer i) {
                if (c == 0) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
                c++;
                return i;
            }

        }).doOnComplete(new Function0() {

            @Override
            public kotlin.Unit invoke() {
                System.out.println("^^^^^^^^^^^^^ completed SLOW");
                return Unit.INSTANCE;
            }

        });

        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.merge(fast, slow).subscribe(ts);
        is.connect();
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 4, ts.valueCount());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStreamUsingSelector() {
        final AtomicInteger emitted = new AtomicInteger();
        Observable<Integer> xs = Observable.range(0, Observable.bufferSize() * 2).doOnNext(new Function1<Integer, kotlin.Unit>() {

            @Override
            public Unit invoke(Integer t1) {
                emitted.incrementAndGet();
                return Unit.INSTANCE;
            }

        });
        TestObserver<Integer> ts = new TestObserver<Integer>();
        xs.publish(new Function1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> invoke(Observable<Integer> xs) {
                return xs.takeUntil(xs.skipWhile(new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer i) {
                        return i <= 3;
                    }

                }));
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertValues(0, 1, 2, 3);
        assertEquals(5, emitted.get());
        System.out.println(ts.values());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStream() {
        Observable<Integer> xs = Observable.range(0, Observable.bufferSize() * 2);
        TestObserver<Integer> ts = new TestObserver<Integer>();
        ConnectableObservable<Integer> xsp = xs.publish();
        xsp.takeUntil(xsp.skipWhile(new Function1<Integer, Boolean>() {

            @Override
            public Boolean invoke(Integer i) {
                return i <= 3;
            }

        })).subscribe(ts);
        xsp.connect();
        System.out.println(ts.values());
    }

    @Test(timeout = 10000)
    public void testBackpressureTwoConsumers() {
        final AtomicInteger sourceEmission = new AtomicInteger();
        final AtomicBoolean sourceUnsubscribed = new AtomicBoolean();
        final Observable<Integer> source = Observable.range(1, 100)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Integer t1) {
                        sourceEmission.incrementAndGet();
                        return Unit.INSTANCE;
                    }
                })
                .doOnDispose(new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        sourceUnsubscribed.set(true);
                        return Unit.INSTANCE;
                    }
                }).share();

        final AtomicBoolean child1Unsubscribed = new AtomicBoolean();
        final AtomicBoolean child2Unsubscribed = new AtomicBoolean();

        final TestObserver<Integer> ts2 = new TestObserver<Integer>();

        final TestObserver<Integer> ts1 = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (valueCount() == 2) {
                    source.doOnDispose(new Function0() {
                        @Override
                        public kotlin.Unit invoke() {
                            child2Unsubscribed.set(true);
                            return Unit.INSTANCE;
                        }
                    }).take(5).subscribe(ts2);
                }
                super.onNext(t);
            }
        };

        source.doOnDispose(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                child1Unsubscribed.set(true);
                return Unit.INSTANCE;
            }
        }).take(5)
        .subscribe(ts1);

        ts1.awaitTerminalEvent();
        ts2.awaitTerminalEvent();

        ts1.assertNoErrors();
        ts2.assertNoErrors();

        assertTrue(sourceUnsubscribed.get());
        assertTrue(child1Unsubscribed.get());
        assertTrue(child2Unsubscribed.get());

        ts1.assertValues(1, 2, 3, 4, 5);
        ts2.assertValues(4, 5, 6, 7, 8);

        assertEquals(8, sourceEmission.get());
    }

    @Test
    public void testConnectWithNoSubscriber() {
        TestScheduler scheduler = new TestScheduler();
        ConnectableObservable<Long> co = Observable.interval(10, 10, TimeUnit.MILLISECONDS, scheduler).take(3).publish();
        co.connect();
        // Emit 0
        scheduler.advanceTimeBy(15, TimeUnit.MILLISECONDS);
        TestObserver<Long> to = new TestObserver<Long>();
        co.subscribe(to);
        // Emit 1 and 2
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        to.assertValues(1L, 2L);
        to.assertNoErrors();
        to.assertTerminated();
    }

    @Test
    public void testSubscribeAfterDisconnectThenConnect() {
        ConnectableObservable<Integer> source = Observable.just(1).publish();

        TestObserver<Integer> ts1 = new TestObserver<Integer>();

        source.subscribe(ts1);

        Disposable s = source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

        TestObserver<Integer> ts2 = new TestObserver<Integer>();

        source.subscribe(ts2);

        Disposable s2 = source.connect();

        ts2.assertValue(1);
        ts2.assertNoErrors();
        ts2.assertTerminated();

        System.out.println(s);
        System.out.println(s2);
    }

    @Test
    public void testNoSubscriberRetentionOnCompleted() {
        ObservablePublish<Integer> source = (ObservablePublish<Integer>)Observable.just(1).publish();

        TestObserver<Integer> ts1 = new TestObserver<Integer>();

        source.subscribe(ts1);

        ts1.assertNoValues();
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

        assertNull(source.current.get());
    }

    @Test
    public void testNonNullConnection() {
        ConnectableObservable<Object> source = Observable.never().publish();

        assertNotNull(source.connect());
        assertNotNull(source.connect());
    }

    @Test
    public void testNoDisconnectSomeoneElse() {
        ConnectableObservable<Object> source = Observable.never().publish();

        Disposable s1 = source.connect();
        Disposable s2 = source.connect();

        s1.dispose();

        Disposable s3 = source.connect();

        s2.dispose();

        assertTrue(checkPublishDisposed(s1));
        assertTrue(checkPublishDisposed(s2));
        assertFalse(checkPublishDisposed(s3));
    }

    @SuppressWarnings("unchecked")
    static boolean checkPublishDisposed(Disposable d) {
        return ((ObservablePublish.PublishObserver<Object>)d).isDisposed();
    }

    @Test
    public void testConnectIsIdempotent() {
        final AtomicInteger calls = new AtomicInteger();
        Observable<Integer> source = Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> t) {
                t.onSubscribe(Disposables.empty());
                calls.getAndIncrement();
            }
        });

        ConnectableObservable<Integer> conn = source.publish();

        assertEquals(0, calls.get());

        conn.connect();
        conn.connect();

        assertEquals(1, calls.get());

        conn.connect().dispose();

        conn.connect();
        conn.connect();

        assertEquals(2, calls.get());
    }
    @Test
    public void testObserveOn() {
        ConnectableObservable<Integer> co = Observable.range(0, 1000).publish();
        Observable<Integer> obs = co.observeOn(Schedulers.computation());
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestObserver<Integer>> tss = new ArrayList<TestObserver<Integer>>();
                for (int k = 1; k < j; k++) {
                    TestObserver<Integer> ts = new TestObserver<Integer>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }

                Disposable s = co.connect();

                for (TestObserver<Integer> ts : tss) {
                    ts.awaitTerminalEvent(2, TimeUnit.SECONDS);
                    ts.assertTerminated();
                    ts.assertNoErrors();
                    assertEquals(1000, ts.valueCount());
                }
                s.dispose();
            }
        }
    }

    @Test
    public void preNextConnect() {
        for (int i = 0; i < 500; i++) {

            final ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

            co.connect();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.test();
                }
            };

            TestCommonHelper.race(r1, r1);
        }
    }

    @Test
    public void connectRace() {
        for (int i = 0; i < 500; i++) {

            final ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.connect();
                }
            };

            TestCommonHelper.race(r1, r1);
        }
    }

    @Test
    public void selectorCrash() {
        Observable.just(1).publish(new Function1<Observable<Integer>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Observable<Integer> v) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void source() {
        Observable<Integer> o = Observable.never();

        assertSame(o, (((HasUpstreamObservableSource<?>)o.publish()).source()));
    }

    @Test
    public void connectThrows() {
        ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();
        try {
            co.connect(new Function1<Disposable, kotlin.Unit>() {
                @Override
                public Unit invoke(Disposable s) {
                    throw new TestException();
                }
            });
        } catch (TestException ex) {
            // expected
        }
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 500; i++) {

            final ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

            final TestObserver<Integer> to = co.test();

            final TestObserver<Integer> to2 = new TestObserver<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void disposeOnArrival() {
        ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

        co.test(true).assertEmpty();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.never().publish());

        TestHelper.checkDisposed(Observable.never().publish(Functions.<Observable<Object>>identity()));
    }

    @Test
    public void empty() {
        ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

        co.connect();
    }

    @Test
    public void take() {
        ConnectableObservable<Integer> co = Observable.range(1, 2).publish();

        TestObserver<Integer> to = co.take(1).test();

        co.connect();

        to.assertResult(1);
    }

    @Test
    public void just() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        ConnectableObservable<Integer> co = ps.publish();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ps.onComplete();
            }
        };

        co.subscribe(to);
        co.connect();

        ps.onNext(1);

        to.assertResult(1);
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < 500; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final ConnectableObservable<Integer> co = ps.publish();

            final TestObserver<Integer> to = co.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .publish()
            .autoConnect()
            .test()
            .assertResult(1);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void noErrorLoss() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            ConnectableObservable<Object> co = Observable.error(new TestException()).publish();

            co.connect();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void subscribeDisconnectRace() {
        for (int i = 0; i < 500; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final ConnectableObservable<Integer> co = ps.publish();

            final Disposable d = co.connect();
            final TestObserver<Integer> to = new TestObserver<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to);
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void selectorDisconnectsIndependentSource() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ps.publish(new Function1<Observable<Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Observable<Integer> v) {
                return Observable.range(1, 2);
            }
        })
        .test()
        .assertResult(1, 2);

        assertFalse(ps.hasObservers());
    }

    @Test(timeout = 5000)
    public void selectorLatecommer() {
        Observable.range(1, 5)
                .publish(new Function1<Observable<Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Observable<Integer> v) {
                return v.concatWith(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .publish(Functions.<Observable<Object>>identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void selectorInnerError() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ps.publish(new Function1<Observable<Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Observable<Integer> v) {
                return Observable.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void delayedUpstreamOnSubscribe() {
        final Observer<?>[] sub = { null };

        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> s) {
                sub[0] = s;
            }
        }
        .publish()
        .connect()
        .dispose();

        Disposable bs = Disposables.empty();

        sub[0].onSubscribe(bs);

        assertTrue(bs.isDisposed());
    }
}
