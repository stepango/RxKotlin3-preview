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

import java.util.concurrent.Callable;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.DeferredScalarSubscription;
import io.reactivex.flowable.internal.subscriptions.EmptySubscription;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import kotlin.jvm.functions.Function2;

public final class FlowableCollect<T, U> extends AbstractFlowableWithUpstream<T, U> {

    final Callable<? extends U> initialSupplier;
    final Function2<? super U, ? super T, kotlin.Unit> collector;

    public FlowableCollect(Flowable<T> source, Callable<? extends U> initialSupplier, Function2<? super U, ? super T, kotlin.Unit> collector) {
        super(source);
        this.initialSupplier = initialSupplier;
        this.collector = collector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        U u;
        try {
            u = ObjectHelper.requireNonNull(initialSupplier.call(), "The initial value supplied is null");
        } catch (Throwable e) {
            EmptySubscription.error(e, s);
            return;
        }

        source.subscribe(new CollectSubscriber<T, U>(s, u, collector));
    }

    static final class CollectSubscriber<T, U> extends DeferredScalarSubscription<U> implements RelaxedSubscriber<T> {

        private static final long serialVersionUID = -3589550218733891694L;

        final Function2<? super U, ? super T, kotlin.Unit> collector;

        final U u;

        Subscription s;

        boolean done;

        CollectSubscriber(Subscriber<? super U> actual, U u, Function2<? super U, ? super T, kotlin.Unit> collector) {
            super(actual);
            this.collector = collector;
            this.u = u;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            try {
                collector.invoke(u, t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.cancel();
                onError(e);
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
            complete(u);
        }

        @Override
        public void cancel() {
            super.cancel();
            s.cancel();
        }
    }
}
