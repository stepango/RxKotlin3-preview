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

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleZipArrayTest {

    final Function2<Object, Object, Object> addString = new Function2<Object, Object, Object>() {
        @Override
        public Object invoke(Object a, Object b) {
            return "" + a + b;
        }
    };


    final Function3<Object, Object, Object, Object> addString3 = new Function3<Object, Object, Object, Object>() {
        @Override
        public Object invoke(Object a, Object b, Object c) {
            return "" + a + b + c;
        }
    };

    @Test
    public void firstError() {
        Single.zip(Single.error(new TestException()), Single.just(1), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void secondError() {
        Single.zip(Single.just(1), Single.<Integer>error(new TestException()), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Object> to = Single.zip(pp.single(0), pp.single(0), addString)
        .test();

        assertTrue(pp.hasObservers());

        to.cancel();

        assertFalse(pp.hasObservers());
    }

    @Test
    public void zipperThrows() {
        Single.zip(Single.just(1), Single.just(2), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void zipperReturnsNull() {
        Single.zip(Single.just(1), Single.just(2), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void middleError() {
        PublishSubject<Integer> pp0 = PublishSubject.create();
        PublishSubject<Integer> pp1 = PublishSubject.create();

        TestObserver<Object> to = Single.zip(pp0.single(0), pp1.single(0), pp0.single(0), addString3)
        .test();

        pp1.onError(new TestException());

        assertFalse(pp0.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void innerErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> pp0 = PublishSubject.create();
                final PublishSubject<Integer> pp1 = PublishSubject.create();

                final TestObserver<Object> to = Single.zip(pp0.single(0), pp1.single(0), addString)
                .test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp0.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }
    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipArrayOneIsNull() {
        Single.zipArray(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, Single.just(1), null)
        .blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void emptyArray() {
        Single.zipArray(new Function1<Object[], Object[]>() {
            @Override
            public Object[] invoke(Object[] a) {
                return a;
            }
        }, new SingleSource[0])
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void oneArray() {
        Single.zipArray(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return (Integer)a[0] + 1;
            }
        }, Single.just(1))
        .test()
        .assertResult(2);
    }
}
