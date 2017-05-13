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

import io.reactivex.common.Disposable;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompletableCacheTest implements Function1<Object, Unit>, Function0<Unit> {

    volatile int count;

    @Override
    public Unit invoke(Object t) {
        count++;
        return Unit.INSTANCE;
    }

    @Override
    public Unit invoke() {
        count++;
        return Unit.INSTANCE;
    }

    @Test
    public void normal() {
        Completable c = Completable.complete()
        .doOnSubscribe(this)
        .cache();

        assertEquals(0, count);

        c.test().assertResult();

        assertEquals(1, count);

        c.test().assertResult();

        assertEquals(1, count);

        c.test().assertResult();

        assertEquals(1, count);
    }

    @Test
    public void error() {
        Completable c = Completable.error(new TestException())
        .doOnSubscribe(this)
        .cache();

        assertEquals(0, count);

        c.test().assertFailure(TestException.class);

        assertEquals(1, count);

        c.test().assertFailure(TestException.class);

        assertEquals(1, count);

        c.test().assertFailure(TestException.class);

        assertEquals(1, count);
    }

    @Test
    public void crossDispose() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final TestObserver<Void> ts1 = new TestObserver<Void>();

        final TestObserver<Void> ts2 = new TestObserver<Void>() {
            @Override
            public void onComplete() {
                super.onComplete();
                ts1.cancel();
            }
        };

        Completable c = ps.ignoreElements().cache();

        c.subscribe(ts2);
        c.subscribe(ts1);

        ps.onComplete();

        ts1.assertEmpty();
        ts2.assertResult();
    }

    @Test
    public void crossDisposeOnError() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final TestObserver<Void> ts1 = new TestObserver<Void>();

        final TestObserver<Void> ts2 = new TestObserver<Void>() {
            @Override
            public void onError(Throwable ex) {
                super.onError(ex);
                ts1.cancel();
            }
        };

        Completable c = ps.ignoreElements().cache();

        c.subscribe(ts2);
        c.subscribe(ts1);

        ps.onError(new TestException());

        ts1.assertEmpty();
        ts2.assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> ps = PublishSubject.create();

        Completable c = ps.ignoreElements().cache();

        assertFalse(ps.hasObservers());

        TestObserver<Void> ts1 = c.test();

        assertTrue(ps.hasObservers());

        ts1.cancel();

        assertTrue(ps.hasObservers());

        TestObserver<Void> ts2 = c.test();

        TestObserver<Void> ts3 = c.test();
        ts3.cancel();

        TestObserver<Void> ts4 = c.test(true);
        ts3.cancel();

        ps.onComplete();

        ts1.assertEmpty();

        ts2.assertResult();

        ts3.assertEmpty();

        ts4.assertEmpty();
    }

    @Test
    public void subscribeRace() {
        for (int i = 0; i < 500; i++) {
            PublishSubject<Integer> ps = PublishSubject.create();

            final Completable c = ps.ignoreElements().cache();

            final TestObserver<Void> ts1 = new TestObserver<Void>();

            final TestObserver<Void> ts2 = new TestObserver<Void>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    c.subscribe(ts1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    c.subscribe(ts2);
                }
            };

            TestCommonHelper.race(r1, r2);

            ps.onComplete();

            ts1.assertResult();
            ts2.assertResult();
        }
    }

    @Test
    public void subscribeDisposeRace() {
        for (int i = 0; i < 500; i++) {
            PublishSubject<Integer> ps = PublishSubject.create();

            final Completable c = ps.ignoreElements().cache();

            final TestObserver<Void> ts1 = c.test();

            final TestObserver<Void> ts2 = new TestObserver<Void>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    c.subscribe(ts2);
                }
            };

            TestCommonHelper.race(r1, r2);

            ps.onComplete();

            ts1.assertEmpty();
            ts2.assertResult();
        }
    }

    @Test
    public void doubleDispose() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final TestObserver<Void> ts = new TestObserver<Void>();

        ps.ignoreElements().cache()
        .subscribe(new CompletableObserver() {

            @Override
            public void onSubscribe(Disposable d) {
                ts.onSubscribe(EmptyDisposable.INSTANCE);
                d.dispose();
                d.dispose();
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                ts.onError(e);
            }
        });

        ps.onComplete();

        ts.assertEmpty();
    }
}
