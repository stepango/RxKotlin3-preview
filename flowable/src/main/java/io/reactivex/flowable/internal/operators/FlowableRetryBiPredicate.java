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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicInteger;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.SubscriptionArbiter;
import kotlin.jvm.functions.Function2;

public final class FlowableRetryBiPredicate<T> extends AbstractFlowableWithUpstream<T, T> {
    final Function2<? super Integer, ? super Throwable, Boolean> predicate;
    public FlowableRetryBiPredicate(
            Flowable<T> source,
            Function2<? super Integer, ? super Throwable, Boolean> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        SubscriptionArbiter sa = new SubscriptionArbiter();
        s.onSubscribe(sa);

        RetryBiSubscriber<T> rs = new RetryBiSubscriber<T>(s, predicate, sa, source);
        rs.subscribeNext();
    }

    // FIXME update to a fresh Rsc algorithm
    static final class RetryBiSubscriber<T> extends AtomicInteger implements RelaxedSubscriber<T> {

        private static final long serialVersionUID = -7098360935104053232L;

        final Subscriber<? super T> actual;
        final SubscriptionArbiter sa;
        final Publisher<? extends T> source;
        final Function2<? super Integer, ? super Throwable, Boolean> predicate;
        int retries;
        RetryBiSubscriber(Subscriber<? super T> actual,
                          Function2<? super Integer, ? super Throwable, Boolean> predicate, SubscriptionArbiter sa, Publisher<? extends T> source) {
            this.actual = actual;
            this.sa = sa;
            this.source = source;
            this.predicate = predicate;
        }

        @Override
        public void onSubscribe(Subscription s) {
            sa.setSubscription(s);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
            sa.produced(1L);
        }
        @Override
        public void onError(Throwable t) {
            boolean b;
            try {
                b = predicate.invoke(++retries, t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(new CompositeException(t, e));
                return;
            }
            if (!b) {
                actual.onError(t);
                return;
            }
            subscribeNext();
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    if (sa.isCancelled()) {
                        return;
                    }
                    source.subscribe(this);

                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }
}
