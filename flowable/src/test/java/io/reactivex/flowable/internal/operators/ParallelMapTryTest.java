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

import org.junit.Test;

import java.util.List;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.ParallelFailureHandling;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ParallelMapTryTest implements Function1<Object, kotlin.Unit> {

    volatile int calls;

    @Override
    public Unit invoke(Object t) {
        calls++;
        return Unit.INSTANCE;
    }

    @Test
    public void mapNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .map(Functions.identity(), e)
            .sequential()
            .test()
            .assertResult(1);
        }
    }
    @Test
    public void mapErrorNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .map(Functions.identity(), e)
            .sequential()
            .test()
            .assertFailure(TestException.class);
        }
    }

    @Test
    public void mapConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .map(Functions.identity(), e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertResult(1);
        }
    }
    @Test
    public void mapErrorConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .map(Functions.identity(), e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertFailure(TestException.class);
        }
    }

    @Test
    public void mapFailWithError() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, ParallelFailureHandling.ERROR)
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void mapFailWithStop() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, ParallelFailureHandling.STOP)
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void mapFailWithRetry() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            int count;
            @Override
            public Integer invoke(Integer v) {
                if (count++ == 1) {
                    return -1;
                }
                return 1 / v;
            }
        }, ParallelFailureHandling.RETRY)
        .sequential()
        .test()
        .assertResult(-1, 1);
    }

    @Test
    public void mapFailWithRetryLimited() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, new Function2<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling invoke(Long n, Throwable e) {
                return n < 5 ? ParallelFailureHandling.RETRY : ParallelFailureHandling.SKIP;
            }
        })
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void mapFailWithSkip() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, ParallelFailureHandling.SKIP)
        .sequential()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mapFailHandlerThrows() {
        TestSubscriber<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, new Function2<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling invoke(Long n, Throwable e) {
                throw new TestException();
            }
        })
        .sequential()
        .test()
        .assertFailure(CompositeException.class);

        TestHelper.assertCompositeExceptions(ts, ArithmeticException.class, TestException.class);
    }

    @Test
    public void mapWrongParallelism() {
        TestHelper.checkInvalidParallelSubscribers(
            Flowable.just(1).parallel(1)
            .map(Functions.identity(), ParallelFailureHandling.ERROR)
        );
    }

    @Test
    public void mapInvalidSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .map(Functions.identity(), ParallelFailureHandling.ERROR)
            .sequential()
            .test();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void mapFailWithErrorConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, ParallelFailureHandling.ERROR)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void mapFailWithStopConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, ParallelFailureHandling.STOP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void mapFailWithRetryConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            int count;
            @Override
            public Integer invoke(Integer v) {
                if (count++ == 1) {
                    return -1;
                }
                return 1 / v;
            }
        }, ParallelFailureHandling.RETRY)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(-1, 1);
    }

    @Test
    public void mapFailWithRetryLimitedConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, new Function2<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling invoke(Long n, Throwable e) {
                return n < 5 ? ParallelFailureHandling.RETRY : ParallelFailureHandling.SKIP;
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void mapFailWithSkipConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, ParallelFailureHandling.SKIP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mapFailHandlerThrowsConditional() {
        TestSubscriber<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1 / v;
            }
        }, new Function2<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling invoke(Long n, Throwable e) {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(CompositeException.class);

        TestHelper.assertCompositeExceptions(ts, ArithmeticException.class, TestException.class);
    }

    @Test
    public void mapWrongParallelismConditional() {
        TestHelper.checkInvalidParallelSubscribers(
            Flowable.just(1).parallel(1)
            .map(Functions.identity(), ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
        );
    }

    @Test
    public void mapInvalidSourceConditional() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .map(Functions.identity(), ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
