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

import java.util.concurrent.Callable;

import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import io.reactivex.observable.internal.operators.ObservableScalarXMap.ScalarDisposable;
import io.reactivex.observable.observers.TestObserver;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObservableScalarXMapTest {

    @Test
    public void utilityClass() {
        TestCommonHelper.checkUtilityClass(ObservableScalarXMap.class);
    }

    static final class CallablePublisher implements ObservableSource<Integer>, Callable<Integer> {
        @Override
        public void subscribe(Observer<? super Integer> s) {
            EmptyDisposable.error(new TestException(), s);
        }

        @Override
        public Integer call() throws Exception {
            throw new TestException();
        }
    }

    static final class EmptyCallablePublisher implements ObservableSource<Integer>, Callable<Integer> {
        @Override
        public void subscribe(Observer<? super Integer> s) {
            EmptyDisposable.complete(s);
        }

        @Override
        public Integer call() throws Exception {
            return null;
        }
    }

    static final class OneCallablePublisher implements ObservableSource<Integer>, Callable<Integer> {
        @Override
        public void subscribe(Observer<? super Integer> s) {
            ScalarDisposable<Integer> sd = new ScalarDisposable<Integer>(s, 1);
            s.onSubscribe(sd);
            sd.run();
        }

        @Override
        public Integer call() throws Exception {
            return 1;
        }
    }

    @Test
    public void tryScalarXMap() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        assertTrue(ObservableScalarXMap.tryScalarXMapSubscribe(new CallablePublisher(), ts, new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer f) {
                return Observable.just(1);
            }
        }));

        ts.assertFailure(TestException.class);
    }

    @Test
    public void emptyXMap() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        assertTrue(ObservableScalarXMap.tryScalarXMapSubscribe(new EmptyCallablePublisher(), ts, new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer f) {
                return Observable.just(1);
            }
        }));

        ts.assertResult();
    }

    @Test
    public void mapperCrashes() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        assertTrue(ObservableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer f) {
                throw new TestException();
            }
        }));

        ts.assertFailure(TestException.class);
    }

    @Test
    public void mapperToJust() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        assertTrue(ObservableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer f) {
                return Observable.just(1);
            }
        }));

        ts.assertResult(1);
    }

    @Test
    public void mapperToEmpty() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        assertTrue(ObservableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer f) {
                return Observable.empty();
            }
        }));

        ts.assertResult();
    }

    @Test
    public void mapperToCrashingCallable() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        assertTrue(ObservableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer f) {
                return new CallablePublisher();
            }
        }));

        ts.assertFailure(TestException.class);
    }

    @Test
    public void scalarMapToEmpty() {
        ObservableScalarXMap.scalarXMap(1, new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.empty();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void scalarMapToCrashingCallable() {
        ObservableScalarXMap.scalarXMap(1, new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return new CallablePublisher();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void scalarDisposableStateCheck() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        ScalarDisposable<Integer> sd = new ScalarDisposable<Integer>(to, 1);
        to.onSubscribe(sd);

        assertFalse(sd.isDisposed());

        assertTrue(sd.isEmpty());

        sd.run();

        assertTrue(sd.isDisposed());

        assertTrue(sd.isEmpty());

        to.assertResult(1);

        try {
            sd.offer(1);
            fail("Should have thrown");
        } catch (UnsupportedOperationException ex) {
            // expected
        }

        try {
            sd.offer(1, 2);
            fail("Should have thrown");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }

    @Test
    public void scalarDisposableRunDisposeRace() {
        for (int i = 0; i < 500; i++) {
            TestObserver<Integer> to = new TestObserver<Integer>();
            final ScalarDisposable<Integer> sd = new ScalarDisposable<Integer>(to, 1);
            to.onSubscribe(sd);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    sd.run();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    sd.dispose();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }
}
