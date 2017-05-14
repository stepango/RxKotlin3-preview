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

import org.reactivestreams.Subscriber;

import hu.akarnokd.reactivestreams.extensions.ConditionalSubscriber;
import io.reactivex.common.annotations.Nullable;
import io.reactivex.common.functions.Function;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscribers.BasicFuseableConditionalSubscriber;
import io.reactivex.flowable.internal.subscribers.BasicFuseableSubscriber;
import kotlin.jvm.functions.Function2;

public final class FlowableDistinctUntilChanged<T, K> extends AbstractFlowableWithUpstream<T, T> {

    final Function<? super T, K> keySelector;

    final Function2<? super K, ? super K, Boolean> comparer;

    public FlowableDistinctUntilChanged(Flowable<T> source, Function<? super T, K> keySelector, Function2<? super K, ? super K, Boolean> comparer) {
        super(source);
        this.keySelector = keySelector;
        this.comparer = comparer;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            ConditionalSubscriber<? super T> cs = (ConditionalSubscriber<? super T>) s;
            source.subscribe(new DistinctUntilChangedConditionalSubscriber<T, K>(cs, keySelector, comparer));
        } else {
            source.subscribe(new DistinctUntilChangedSubscriber<T, K>(s, keySelector, comparer));
        }
    }

    static final class DistinctUntilChangedSubscriber<T, K> extends BasicFuseableSubscriber<T, T>
    implements ConditionalSubscriber<T> {


        final Function<? super T, K> keySelector;

        final Function2<? super K, ? super K, Boolean> comparer;

        K last;

        boolean hasValue;

        DistinctUntilChangedSubscriber(Subscriber<? super T> actual,
                                       Function<? super T, K> keySelector,
                                       Function2<? super K, ? super K, Boolean> comparer) {
            super(actual);
            this.keySelector = keySelector;
            this.comparer = comparer;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            if (sourceMode != NONE) {
                actual.onNext(t);
                return true;
            }

            K key;

            try {
                key = keySelector.apply(t);
                if (hasValue) {
                    boolean equal = comparer.invoke(last, key);
                    last = key;
                    if (equal) {
                        return false;
                    }
                } else {
                    hasValue = true;
                    last = key;
                }
            } catch (Throwable ex) {
               fail(ex);
               return true;
            }

            actual.onNext(t);
            return true;
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            for (;;) {
                T v = qs.poll();
                if (v == null) {
                    return null;
                }
                K key = keySelector.apply(v);
                if (!hasValue) {
                    hasValue = true;
                    last = key;
                    return v;
                }

                if (!comparer.invoke(last, key)) {
                    last = key;
                    return v;
                }
                last = key;
                if (sourceMode != SYNC) {
                    s.request(1);
                }
            }
        }

    }

    static final class DistinctUntilChangedConditionalSubscriber<T, K> extends BasicFuseableConditionalSubscriber<T, T> {

        final Function<? super T, K> keySelector;

        final Function2<? super K, ? super K, Boolean> comparer;

        K last;

        boolean hasValue;

        DistinctUntilChangedConditionalSubscriber(ConditionalSubscriber<? super T> actual,
                                                  Function<? super T, K> keySelector,
                                                  Function2<? super K, ? super K, Boolean> comparer) {
            super(actual);
            this.keySelector = keySelector;
            this.comparer = comparer;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            if (sourceMode != NONE) {
                return actual.tryOnNext(t);
            }

            K key;

            try {
                key = keySelector.apply(t);
                if (hasValue) {
                    boolean equal = comparer.invoke(last, key);
                    last = key;
                    if (equal) {
                        return false;
                    }
                } else {
                    hasValue = true;
                    last = key;
                }
            } catch (Throwable ex) {
               fail(ex);
               return true;
            }

            actual.onNext(t);
            return true;
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            for (;;) {
                T v = qs.poll();
                if (v == null) {
                    return null;
                }
                K key = keySelector.apply(v);
                if (!hasValue) {
                    hasValue = true;
                    last = key;
                    return v;
                }

                if (!comparer.invoke(last, key)) {
                    last = key;
                    return v;
                }
                last = key;
                if (sourceMode != SYNC) {
                    s.request(1);
                }
            }
        }

    }
}
