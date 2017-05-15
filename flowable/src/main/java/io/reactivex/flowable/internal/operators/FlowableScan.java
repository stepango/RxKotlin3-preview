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

import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;

public final class FlowableScan<T> extends AbstractFlowableWithUpstream<T, T> {
    final Function2<T, T, T> accumulator;
    public FlowableScan(Flowable<T> source, Function2<T, T, T> accumulator) {
        super(source);
        this.accumulator = accumulator;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new ScanSubscriber<T>(s, accumulator));
    }

    static final class ScanSubscriber<T> implements RelaxedSubscriber<T>, Subscription {
        final Subscriber<? super T> actual;
        final Function2<T, T, T> accumulator;

        Subscription s;

        T value;

        boolean done;

        ScanSubscriber(Subscriber<? super T> actual, Function2<T, T, T> accumulator) {
            this.actual = actual;
            this.accumulator = accumulator;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            final Subscriber<? super T> a = actual;
            T v = value;
            if (v == null) {
                value = t;
                a.onNext(t);
            } else {
                T u;

                try {
                    u = ObjectHelper.requireNonNull(accumulator.invoke(v, t), "The value returned by the accumulator is null");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    s.cancel();
                    onError(e);
                    return;
                }

                value = u;
                a.onNext(u);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaCommonPlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
