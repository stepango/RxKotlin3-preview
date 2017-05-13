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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.Observer;
import io.reactivex.observable.internal.observers.BlockingFirstObserver;
import io.reactivex.observable.observers.TestObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObservableBlockingTest {

    @Test
    public void blockingFirst() {
        assertEquals(1, Observable.range(1, 10)
                .subscribeOn(Schedulers.computation()).blockingFirst().intValue());
    }

    @Test
    public void blockingFirstDefault() {
        assertEquals(1, Observable.<Integer>empty()
                .subscribeOn(Schedulers.computation()).blockingFirst(1).intValue());
    }

    @Test
    public void blockingSubscribeConsumer() {
        final List<Integer> list = new ArrayList<Integer>();

        Observable.range(1, 5)
        .subscribeOn(Schedulers.computation())
                .blockingSubscribe(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumer() {
        final List<Object> list = new ArrayList<Object>();

        Observable.range(1, 5)
        .subscribeOn(Schedulers.computation())
                .blockingSubscribe(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        }, Functions.emptyConsumer());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerError() {
        final List<Object> list = new ArrayList<Object>();

        TestException ex = new TestException();

        Function1<Object, kotlin.Unit> cons = new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        };

        Observable.range(1, 5).concatWith(Observable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(cons, cons);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerAction() {
        final List<Object> list = new ArrayList<Object>();

        Function1<Object, kotlin.Unit> cons = new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        };

        Observable.range(1, 5)
        .subscribeOn(Schedulers.computation())
                .blockingSubscribe(cons, cons, new Function0() {
            @Override
            public kotlin.Unit invoke() {
                list.add(100);
                return Unit.INSTANCE;
            }
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void blockingSubscribeObserver() {
        final List<Object> list = new ArrayList<Object>();

        Observable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Observer<Object>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }

        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void blockingSubscribeObserverError() {
        final List<Object> list = new ArrayList<Object>();

        final TestException ex = new TestException();

        Observable.range(1, 5).concatWith(Observable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Observer<Object>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }

        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test(expected = TestException.class)
    public void blockingForEachThrows() {
        Observable.just(1)
                .blockingForEach(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer e) {
                throw new TestException();
            }
        });
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingFirstEmpty() {
        Observable.empty().blockingFirst();
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingLastEmpty() {
        Observable.empty().blockingLast();
    }

    @Test
    public void blockingFirstNormal() {
        assertEquals(1, Observable.just(1, 2).blockingFirst(3).intValue());
    }

    @Test
    public void blockingLastNormal() {
        assertEquals(2, Observable.just(1, 2).blockingLast(3).intValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingSingleEmpty() {
        Observable.empty().blockingSingle();
    }

    @Test
    public void utilityClass() {
        TestCommonHelper.checkUtilityClass(ObservableBlockingSubscribe.class);
    }

    @Test
    public void disposeUpFront() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.dispose();
        Observable.just(1).blockingSubscribe(to);

        to.assertEmpty();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void delayed() throws Exception {
        final TestObserver<Object> to = new TestObserver<Object>();
        final Observer[] s = { null };

        Schedulers.single().scheduleDirect(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                to.dispose();
                s[0].onNext(1);
            }
        }, 200, TimeUnit.MILLISECONDS);

        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                s[0] = observer;
            }
        }.blockingSubscribe(to);

        while (!to.isDisposed()) {
            Thread.sleep(100);
        }

        to.assertEmpty();
    }

    @Test
    public void interrupt() {
        TestObserver<Object> to = new TestObserver<Object>();
        Thread.currentThread().interrupt();
        Observable.never().blockingSubscribe(to);
    }

    @Test
    public void onCompleteDelayed() {
        TestObserver<Object> to = new TestObserver<Object>();

        Observable.empty().delay(100, TimeUnit.MILLISECONDS)
        .blockingSubscribe(to);

        to.assertResult();
    }

    @Test
    public void blockingCancelUpfront() {
        BlockingFirstObserver<Integer> o = new BlockingFirstObserver<Integer>();

        assertFalse(o.isDisposed());
        o.dispose();
        assertTrue(o.isDisposed());

        Disposable d = Disposables.empty();

        o.onSubscribe(d);

        assertTrue(d.isDisposed());

        Thread.currentThread().interrupt();
        try {
            o.blockingGet();
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }

        Thread.interrupted();

        o.onError(new TestException());

        try {
            o.blockingGet();
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }
    }
}
