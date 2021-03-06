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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.QueueDisposable;
import io.reactivex.observable.observers.ObserverFusion;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.UnicastSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableDoOnEachTest {

    Observer<String> subscribedObserver;
    Observer<String> sideEffectObserver;

    @Before
    public void before() {
        subscribedObserver = TestHelper.mockObserver();
        sideEffectObserver = TestHelper.mockObserver();
    }

    @Test
    public void testDoOnEach() {
        Observable<String> base = Observable.just("a", "b", "c");
        Observable<String> doOnEach = base.doOnEach(sideEffectObserver);

        doOnEach.subscribe(subscribedObserver);

        // ensure the leaf Observer is still getting called
        verify(subscribedObserver, never()).onError(any(Throwable.class));
        verify(subscribedObserver, times(1)).onNext("a");
        verify(subscribedObserver, times(1)).onNext("b");
        verify(subscribedObserver, times(1)).onNext("c");
        verify(subscribedObserver, times(1)).onComplete();

        // ensure our injected Observer is getting called
        verify(sideEffectObserver, never()).onError(any(Throwable.class));
        verify(sideEffectObserver, times(1)).onNext("a");
        verify(sideEffectObserver, times(1)).onNext("b");
        verify(sideEffectObserver, times(1)).onNext("c");
        verify(sideEffectObserver, times(1)).onComplete();
    }

    @Test
    public void testDoOnEachWithError() {
        Observable<String> base = Observable.just("one", "fail", "two", "three", "fail");
        Observable<String> errs = base.map(new Function1<String, String>() {
            @Override
            public String invoke(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                return s;
            }
        });

        Observable<String> doOnEach = errs.doOnEach(sideEffectObserver);

        doOnEach.subscribe(subscribedObserver);
        verify(subscribedObserver, times(1)).onNext("one");
        verify(subscribedObserver, never()).onNext("two");
        verify(subscribedObserver, never()).onNext("three");
        verify(subscribedObserver, never()).onComplete();
        verify(subscribedObserver, times(1)).onError(any(Throwable.class));

        verify(sideEffectObserver, times(1)).onNext("one");
        verify(sideEffectObserver, never()).onNext("two");
        verify(sideEffectObserver, never()).onNext("three");
        verify(sideEffectObserver, never()).onComplete();
        verify(sideEffectObserver, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void testDoOnEachWithErrorInCallback() {
        Observable<String> base = Observable.just("one", "two", "fail", "three");
        Observable<String> doOnEach = base.doOnNext(new Function1<String, kotlin.Unit>() {
            @Override
            public Unit invoke(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                return Unit.INSTANCE;
            }
        });

        doOnEach.subscribe(subscribedObserver);
        verify(subscribedObserver, times(1)).onNext("one");
        verify(subscribedObserver, times(1)).onNext("two");
        verify(subscribedObserver, never()).onNext("three");
        verify(subscribedObserver, never()).onComplete();
        verify(subscribedObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testIssue1451Case1() {
        // https://github.com/Netflix/RxJava/issues/1451
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < expectedCount; i++) {
            Observable
                    .just(Boolean.TRUE, Boolean.FALSE)
                    .takeWhile(new Function1<Boolean, Boolean>() {
                        @Override
                        public Boolean invoke(Boolean value) {
                            return value;
                        }
                    })
                    .toList()
                    .doOnSuccess(new Function1<List<Boolean>, kotlin.Unit>() {
                        @Override
                        public Unit invoke(List<Boolean> booleans) {
                            count.incrementAndGet();
                            return Unit.INSTANCE;
                        }
                    })
                    .subscribe();
        }
        assertEquals(expectedCount, count.get());
    }

    @Test
    public void testIssue1451Case2() {
        // https://github.com/Netflix/RxJava/issues/1451
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < expectedCount; i++) {
            Observable
                    .just(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE)
                    .takeWhile(new Function1<Boolean, Boolean>() {
                        @Override
                        public Boolean invoke(Boolean value) {
                            return value;
                        }
                    })
                    .toList()
                    .doOnSuccess(new Function1<List<Boolean>, kotlin.Unit>() {
                        @Override
                        public Unit invoke(List<Boolean> booleans) {
                            count.incrementAndGet();
                            return Unit.INSTANCE;
                        }
                    })
                    .subscribe();
        }
        assertEquals(expectedCount, count.get());
    }

    // FIXME crashing ObservableSource can't propagate to an Observer
//    @Test
//    public void testFatalError() {
//        try {
//            Observable.just(1, 2, 3)
//                    .flatMap(new Function<Integer, Observable<?>>() {
//                        @Override
//                        public Observable<?> apply(Integer integer) {
//                            return Observable.create(new ObservableSource<Object>() {
//                                @Override
//                                public void accept(Observer<Object> o) {
//                                    throw new NullPointerException("Test NPE");
//                                }
//                            });
//                        }
//                    })
//                    .doOnNext(new Consumer<Object>() {
//                        @Override
//                        public void accept(Object o) {
//                            System.out.println("Won't come here");
//                        }
//                    })
//                    .subscribe();
//            fail("should have thrown an exception");
//        } catch (OnErrorNotImplementedException e) {
//            assertTrue(e.getCause() instanceof NullPointerException);
//            assertEquals(e.getCause().getMessage(), "Test NPE");
//            System.out.println("Received exception: " + e);
//        }
//    }

    @Test
    public void onErrorThrows() {
        TestObserver<Object> ts = TestObserver.create();

        Observable.error(new TestException())
                .doOnError(new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                throw new TestException();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(CompositeException.class);

        CompositeException ex = (CompositeException)ts.errors().get(0);

        List<Throwable> exceptions = ex.getExceptions();
        assertEquals(2, exceptions.size());
        Assert.assertTrue(exceptions.get(0) instanceof TestException);
        Assert.assertTrue(exceptions.get(1) instanceof TestException);
    }

    @Test
    public void ignoreCancel() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Observable.wrap(new ObservableSource<Object>() {
                @Override
                public void subscribe(Observer<? super Object> s) {
                    s.onSubscribe(Disposables.empty());
                    s.onNext(1);
                    s.onNext(2);
                    s.onError(new IOException());
                    s.onComplete();
                }
            })
                    .doOnNext(new Function1<Object, kotlin.Unit>() {
                @Override
                public Unit invoke(Object e) {
                    throw new TestException();
                }
            })
            .test()
            .assertFailure(TestException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onErrorAfterCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Observable.wrap(new ObservableSource<Object>() {
                @Override
                public void subscribe(Observer<? super Object> s) {
                    s.onSubscribe(Disposables.empty());
                    s.onError(new TestException());
                }
            })
                    .doAfterTerminate(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            })
            .test()
            .assertFailure(TestException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onCompleteAfterCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Observable.wrap(new ObservableSource<Object>() {
                @Override
                public void subscribe(Observer<? super Object> s) {
                    s.onSubscribe(Disposables.empty());
                    s.onComplete();
                }
            })
                    .doAfterTerminate(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            })
            .test()
            .assertResult();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrash() {
        Observable.wrap(new ObservableSource<Object>() {
            @Override
            public void subscribe(Observer<? super Object> s) {
                s.onSubscribe(Disposables.empty());
                s.onComplete();
            }
        })
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                throw new TestException();
            }
        })
        .test()
                .assertFailure(TestException.class);
    }

    @Test
    public void ignoreCancelConditional() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Observable.wrap(new ObservableSource<Object>() {
                @Override
                public void subscribe(Observer<? super Object> s) {
                    s.onSubscribe(Disposables.empty());
                    s.onNext(1);
                    s.onNext(2);
                    s.onError(new IOException());
                    s.onComplete();
                }
            })
                    .doOnNext(new Function1<Object, kotlin.Unit>() {
                @Override
                public Unit invoke(Object e) {
                    throw new TestException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertFailure(TestException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onErrorAfterCrashConditional() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Observable.wrap(new ObservableSource<Object>() {
                @Override
                public void subscribe(Observer<? super Object> s) {
                    s.onSubscribe(Disposables.empty());
                    s.onError(new TestException());
                }
            })
                    .doAfterTerminate(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertFailure(TestException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onCompleteAfter() {
        final int[] call = { 0 };
        Observable.just(1)
                .doAfterTerminate(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(1, call[0]);
    }

    @Test
    public void onCompleteAfterCrashConditional() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Observable.wrap(new ObservableSource<Object>() {
                @Override
                public void subscribe(Observer<? super Object> s) {
                    s.onSubscribe(Disposables.empty());
                    s.onComplete();
                }
            })
                    .doAfterTerminate(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertResult();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrashConditional() {
        Observable.wrap(new ObservableSource<Object>() {
            @Override
            public void subscribe(Observer<? super Object> s) {
                s.onSubscribe(Disposables.empty());
                s.onComplete();
            }
        })
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
                .assertFailure(TestException.class);
    }

    @Test
    public void onErrorOnErrorCrashConditional() {
        TestObserver<Object> ts = Observable.error(new TestException("Outer"))
                .doOnError(new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                throw new TestException("Inner");
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(ts.errors().get(0));

        TestCommonHelper.assertError(errors, 0, TestException.class, "Outer");
        TestCommonHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    @Ignore("Fusion not supported yet") // TODO decide/implement fusion
    public void fused() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        final int[] call = { 0, 0 };

        Observable.range(1, 5)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                call[1]++;
                return Unit.INSTANCE;
            }
        })
        .subscribe(ts);

        ts.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.SYNC))
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    @Ignore("Fusion not supported yet") // TODO decide/implement fusion
    public void fusedOnErrorCrash() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        final int[] call = { 0 };

        Observable.range(1, 5)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                throw new TestException();
            }
        })
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
        .subscribe(ts);

        ts.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.SYNC))
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    @Ignore("Fusion not supported yet") // TODO decide/implement fusion
    public void fusedConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        final int[] call = { 0, 0 };

        Observable.range(1, 5)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                call[1]++;
                return Unit.INSTANCE;
            }
        })
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.SYNC))
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    @Ignore("Fusion not supported yet") // TODO decide/implement fusion
    public void fusedOnErrorCrashConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        final int[] call = { 0 };

        Observable.range(1, 5)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                throw new TestException();
            }
        })
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.SYNC))
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    @Ignore("Fusion not supported yet") // TODO decide/implement fusion
    public void fusedAsync() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        final int[] call = { 0, 0 };

        UnicastSubject<Integer> up = UnicastSubject.create();

        up
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                call[1]++;
                return Unit.INSTANCE;
            }
        })
        .subscribe(ts);

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        ts.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    @Ignore("Fusion not supported yet") // TODO decide/implement fusion
    public void fusedAsyncConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        final int[] call = { 0, 0 };

        UnicastSubject<Integer> up = UnicastSubject.create();

        up
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                call[1]++;
                return Unit.INSTANCE;
            }
        })
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        ts.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    @Ignore("Fusion not supported yet") // TODO decide/implement fusion
    public void fusedAsyncConditional2() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        final int[] call = { 0, 0 };

        UnicastSubject<Integer> up = UnicastSubject.create();

        up.hide()
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
                .doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                call[1]++;
                return Unit.INSTANCE;
            }
        })
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        ts.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.NONE))
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).doOnEach(new TestObserver<Integer>()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Observable<Object> o) {
                return o.doOnEach(new TestObserver<Object>());
            }
        });
    }
}
