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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Observable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ObservableIgnoreElementsTest {

    @Test
    public void testWithEmptyObservable() {
        assertTrue(Observable.empty().ignoreElements().toObservable().isEmpty().blockingGet());
    }

    @Test
    public void testWithNonEmptyObservable() {
        assertTrue(Observable.just(1, 2, 3).ignoreElements().toObservable().isEmpty().blockingGet());
    }

    @Test
    public void testUpstreamIsProcessedButIgnoredObservable() {
        final int num = 10;
        final AtomicInteger upstreamCount = new AtomicInteger();
        long count = Observable.range(1, num)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Integer t) {
                        upstreamCount.incrementAndGet();
                        return Unit.INSTANCE;
                    }
                })
                .ignoreElements()
                .toObservable()
                .count().blockingGet();
        assertEquals(num, upstreamCount.get());
        assertEquals(0, count);
    }

    @Test
    public void testCompletedOkObservable() {
        TestObserver<Object> ts = new TestObserver<Object>();
        Observable.range(1, 10).ignoreElements().toObservable().subscribe(ts);
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
    }

    @Test
    public void testErrorReceivedObservable() {
        TestObserver<Object> ts = new TestObserver<Object>();
        TestException ex = new TestException("boo");
        Observable.error(ex).ignoreElements().toObservable().subscribe(ts);
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
        ts.assertError(TestException.class);
        ts.assertErrorMessage("boo");
    }

    @Test
    public void testUnsubscribesFromUpstreamObservable() {
        final AtomicBoolean unsub = new AtomicBoolean();
        Observable.range(1, 10).concatWith(Observable.<Integer>never())
                .doOnDispose(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                unsub.set(true);
                return Unit.INSTANCE;
            }})
            .ignoreElements()
            .toObservable()
            .subscribe()
            .dispose();
        assertTrue(unsub.get());
    }

    @Test
    public void testWithEmpty() {
        assertNull(Observable.empty().ignoreElements().blockingGet());
    }

    @Test
    public void testWithNonEmpty() {
        assertNull(Observable.just(1, 2, 3).ignoreElements().blockingGet());
    }

    @Test
    public void testUpstreamIsProcessedButIgnored() {
        final int num = 10;
        final AtomicInteger upstreamCount = new AtomicInteger();
        Object count = Observable.range(1, num)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Integer t) {
                        upstreamCount.incrementAndGet();
                        return Unit.INSTANCE;
                    }
                })
                .ignoreElements()
                .blockingGet();
        assertEquals(num, upstreamCount.get());
        assertNull(count);
    }

    @Test
    public void testCompletedOk() {
        TestObserver<Object> ts = new TestObserver<Object>();
        Observable.range(1, 10).ignoreElements().subscribe(ts);
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
    }

    @Test
    public void testErrorReceived() {
        TestObserver<Object> ts = new TestObserver<Object>();
        TestException ex = new TestException("boo");
        Observable.error(ex).ignoreElements().subscribe(ts);
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
        ts.assertError(TestException.class);
        ts.assertErrorMessage("boo");
    }

    @Test
    public void testUnsubscribesFromUpstream() {
        final AtomicBoolean unsub = new AtomicBoolean();
        Observable.range(1, 10).concatWith(Observable.<Integer>never())
                .doOnDispose(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                unsub.set(true);
                return Unit.INSTANCE;
            }})
            .ignoreElements()
            .subscribe()
            .dispose();
        assertTrue(unsub.get());
    }

    @Test
    public void cancel() {

        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Integer> ts = pp.ignoreElements().<Integer>toObservable().test();

        assertTrue(pp.hasObservers());

        ts.cancel();

        assertFalse(pp.hasObservers());

        TestHelper.checkDisposed(pp.ignoreElements().<Integer>toObservable());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).ignoreElements());

        TestHelper.checkDisposed(Observable.just(1).ignoreElements().toObservable());
    }
}
