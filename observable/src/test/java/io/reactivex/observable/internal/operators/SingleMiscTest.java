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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.common.Disposables;
import io.reactivex.common.Schedulers;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.SingleTransformer;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class SingleMiscTest {
    @Test
    public void never() {
        Single.never()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void timer() throws Exception {
        Single.timer(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L);
    }

    @Test
    public void wrap() {
        assertSame(Single.never(), Single.wrap(Single.never()));

        Single.wrap(new SingleSource<Object>() {
            @Override
            public void subscribe(SingleObserver<? super Object> s) {
                s.onSubscribe(Disposables.empty());
                s.onSuccess(1);
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void cast() {
        Single<Number> source = Single.just(1d)
                .cast(Number.class);
        source.test()
        .assertResult((Number)1d);
    }

    @Test
    public void contains() {
        Single.just(1).contains(1).test().assertResult(true);

        Single.just(2).contains(1).test().assertResult(false);
    }

    @Test
    public void compose() {

        Single.just(1)
        .compose(new SingleTransformer<Integer, Object>() {
            @Override
            public SingleSource<Object> apply(Single<Integer> f) {
                return f.map(new Function1<Integer, Object>() {
                    @Override
                    public Object invoke(Integer v) {
                        return v + 1;
                    }
                });
            }
        })
        .test()
        .assertResult(2);
    }

    @Test
    public void hide() {
        assertNotSame(Single.never(), Single.never().hide());
    }

    @Test
    public void onErrorResumeNext() {
        Single.<Integer>error(new TestException())
        .onErrorResumeNext(Single.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void onErrorReturnValue() {
        Single.<Integer>error(new TestException())
        .onErrorReturnItem(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void repeat() {
        Single.just(1).repeat().take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatTimes() {
        Single.just(1).repeat(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatUntil() {
        final AtomicBoolean flag = new AtomicBoolean();

        Single.just(1)
                .doOnSuccess(new Function1<Integer, kotlin.Unit>() {
            int c;
            @Override
            public Unit invoke(Integer v) {
                if (++c == 5) {
                    flag.set(true);
                }
                return Unit.INSTANCE;
            }
        })
                .repeatUntil(new Function0() {
            @Override
            public Boolean invoke() {
                return flag.get();
            }
        })
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void retry() {
        Single.fromCallable(new Callable<Object>() {
            int c;
            @Override
            public Object call() throws Exception {
                if (++c != 5) {
                    throw new TestException();
                }
                return 1;
            }
        })
        .retry()
        .test()
        .assertResult(1);
    }

    @Test
    public void retryBiPredicate() {
        Single.fromCallable(new Callable<Object>() {
            int c;
            @Override
            public Object call() throws Exception {
                if (++c != 5) {
                    throw new TestException();
                }
                return 1;
            }
        })
                .retry(new Function2<Integer, Throwable, Boolean>() {
            @Override
            public Boolean invoke(Integer i, Throwable e) {
                return true;
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void retryTimes() {
        Single.fromCallable(new Callable<Object>() {
            int c;
            @Override
            public Object call() throws Exception {
                if (++c != 5) {
                    throw new TestException();
                }
                return 1;
            }
        })
        .retry(5)
        .test()
        .assertResult(1);
    }

    @Test
    public void retryPredicate() {
        Single.fromCallable(new Callable<Object>() {
            int c;
            @Override
            public Object call() throws Exception {
                if (++c != 5) {
                    throw new TestException();
                }
                return 1;
            }
        })
                .retry(new Function1<Throwable, Boolean>() {
            @Override
            public Boolean invoke(Throwable e) {
                return true;
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void timeout() throws Exception {
        Single.never().timeout(100, TimeUnit.MILLISECONDS, Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void timeoutOther() throws Exception {
        Single.never()
        .timeout(100, TimeUnit.MILLISECONDS, Schedulers.io(), Single.just(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void toCompletable() {
        Single.just(1)
        .toCompletable()
        .test()
        .assertResult();

        Single.error(new TestException())
        .toCompletable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void toObservable() {
        Single.just(1)
        .toObservable()
        .test()
        .assertResult(1);

        Single.error(new TestException())
        .toObservable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void equals() {
        Single.equals(Single.just(1), Single.just(1).hide())
        .test()
        .assertResult(true);

        Single.equals(Single.just(1), Single.just(2))
        .test()
        .assertResult(false);
    }

}
