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
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.ParallelFlowable;
import io.reactivex.flowable.internal.subscriptions.DeferredScalarSubscription;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;

/**
 * Reduces all 'rails' into a single value which then gets reduced into a single
 * Publisher sequence.
 *
 * @param <T> the value type
 */
public final class ParallelReduceFull<T> extends Flowable<T> {

    final ParallelFlowable<? extends T> source;

    final Function2<T, T, T> reducer;

    public ParallelReduceFull(ParallelFlowable<? extends T> source, Function2<T, T, T> reducer) {
        this.source = source;
        this.reducer = reducer;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        ParallelReduceFullMainSubscriber<T> parent = new ParallelReduceFullMainSubscriber<T>(s, source.parallelism(), reducer);
        s.onSubscribe(parent);

        source.subscribe(parent.subscribers);
    }

    static final class ParallelReduceFullMainSubscriber<T> extends DeferredScalarSubscription<T> {


        private static final long serialVersionUID = -5370107872170712765L;

        final ParallelReduceFullInnerSubscriber<T>[] subscribers;

        final Function2<T, T, T> reducer;

        final AtomicReference<SlotPair<T>> current = new AtomicReference<SlotPair<T>>();

        final AtomicInteger remaining = new AtomicInteger();

        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        ParallelReduceFullMainSubscriber(Subscriber<? super T> subscriber, int n, Function2<T, T, T> reducer) {
            super(subscriber);
            @SuppressWarnings("unchecked")
            ParallelReduceFullInnerSubscriber<T>[] a = new ParallelReduceFullInnerSubscriber[n];
            for (int i = 0; i < n; i++) {
                a[i] = new ParallelReduceFullInnerSubscriber<T>(this, reducer);
            }
            this.subscribers = a;
            this.reducer = reducer;
            remaining.lazySet(n);
        }

        SlotPair<T> addValue(T value) {
            for (;;) {
                SlotPair<T> curr = current.get();

                if (curr == null) {
                    curr = new SlotPair<T>();
                    if (!current.compareAndSet(null, curr)) {
                        continue;
                    }
                }

                int c = curr.tryAcquireSlot();
                if (c < 0) {
                    current.compareAndSet(curr, null);
                    continue;
                }
                if (c == 0) {
                    curr.first = value;
                } else {
                    curr.second = value;
                }

                if (curr.releaseSlot()) {
                    current.compareAndSet(curr, null);
                    return curr;
                }
                return null;
            }
        }

        @Override
        public void cancel() {
            for (ParallelReduceFullInnerSubscriber<T> inner : subscribers) {
                inner.cancel();
            }
        }

        void innerError(Throwable ex) {
            if (error.compareAndSet(null, ex)) {
                cancel();
                actual.onError(ex);
            } else {
                if (ex != error.get()) {
                    RxJavaCommonPlugins.onError(ex);
                }
            }
        }

        void innerComplete(T value) {
            if (value != null) {
                for (;;) {
                    SlotPair<T> sp = addValue(value);

                    if (sp != null) {

                        try {
                            value = ObjectHelper.requireNonNull(reducer.invoke(sp.first, sp.second), "The reducer returned a null value");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            innerError(ex);
                            return;
                        }

                    } else {
                        break;
                    }
                }
            }

            if (remaining.decrementAndGet() == 0) {
                SlotPair<T> sp = current.get();
                current.lazySet(null);

                if (sp != null) {
                    complete(sp.first);
                } else {
                    actual.onComplete();
                }
            }
        }
    }

    static final class ParallelReduceFullInnerSubscriber<T>
    extends AtomicReference<Subscription>
    implements RelaxedSubscriber<T> {

        private static final long serialVersionUID = -7954444275102466525L;

        final ParallelReduceFullMainSubscriber<T> parent;

        final Function2<T, T, T> reducer;

        T value;

        boolean done;

        ParallelReduceFullInnerSubscriber(ParallelReduceFullMainSubscriber<T> parent, Function2<T, T, T> reducer) {
            this.parent = parent;
            this.reducer = reducer;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                T v = value;

                if (v == null) {
                    value = t;
                } else {

                    try {
                        v = ObjectHelper.requireNonNull(reducer.invoke(v, t), "The reducer returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        get().cancel();
                        onError(ex);
                        return;
                    }

                    value = v;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaCommonPlugins.onError(t);
                return;
            }
            done = true;
            parent.innerError(t);
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                parent.innerComplete(value);
            }
        }

        void cancel() {
            SubscriptionHelper.cancel(this);
        }
    }

    static final class SlotPair<T> extends AtomicInteger {

        private static final long serialVersionUID = 473971317683868662L;

        T first;

        T second;

        final AtomicInteger releaseIndex = new AtomicInteger();

        int tryAcquireSlot() {
            for (;;) {
                int acquired = get();
                if (acquired >= 2) {
                    return -1;
                }

                if (compareAndSet(acquired, acquired + 1)) {
                    return acquired;
                }
            }
        }

        boolean releaseSlot() {
            return releaseIndex.incrementAndGet() == 2;
        }
    }
}
