/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.flowable.internal.operators;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import hu.akarnokd.reactivestreams.extensions.FusedQueue;
import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.disposables.CompositeDisposable;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.exceptions.MissingBackpressureException;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.common.internal.utils.ExceptionHelper;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.queues.SpscLinkedArrayQueue;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import io.reactivex.flowable.internal.utils.BackpressureHelper;
import io.reactivex.flowable.processors.UnicastProcessor;
import kotlin.jvm.functions.Function1;

public final class FlowableGroupJoin<TLeft, TRight, TLeftEnd, TRightEnd, R> extends AbstractFlowableWithUpstream<TLeft, R> {

    final Publisher<? extends TRight> other;

    final Function1<? super TLeft, ? extends Publisher<TLeftEnd>> leftEnd;

    final Function1<? super TRight, ? extends Publisher<TRightEnd>> rightEnd;

    final Function2<? super TLeft, ? super Flowable<TRight>, ? extends R> resultSelector;

    public FlowableGroupJoin(
            Flowable<TLeft> source,
            Publisher<? extends TRight> other,
            Function1<? super TLeft, ? extends Publisher<TLeftEnd>> leftEnd,
            Function1<? super TRight, ? extends Publisher<TRightEnd>> rightEnd,
            Function2<? super TLeft, ? super Flowable<TRight>, ? extends R> resultSelector) {
        super(source);
        this.other = other;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
        this.resultSelector = resultSelector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {

        GroupJoinSubscription<TLeft, TRight, TLeftEnd, TRightEnd, R> parent =
                new GroupJoinSubscription<TLeft, TRight, TLeftEnd, TRightEnd, R>(s, leftEnd, rightEnd, resultSelector);

        s.onSubscribe(parent);

        LeftRightSubscriber left = new LeftRightSubscriber(parent, true);
        parent.disposables.add(left);
        LeftRightSubscriber right = new LeftRightSubscriber(parent, false);
        parent.disposables.add(right);

        source.subscribe(left);
        other.subscribe(right);
    }

    interface JoinSupport {

        void innerError(Throwable ex);

        void innerComplete(LeftRightSubscriber sender);

        void innerValue(boolean isLeft, Object o);

        void innerClose(boolean isLeft, LeftRightEndSubscriber index);

        void innerCloseError(Throwable ex);
    }

    static final class GroupJoinSubscription<TLeft, TRight, TLeftEnd, TRightEnd, R>
    extends AtomicInteger implements Subscription, JoinSupport {


        private static final long serialVersionUID = -6071216598687999801L;

        final Subscriber<? super R> actual;

        final AtomicLong requested;

        final SpscLinkedArrayQueue<Object> queue;

        final CompositeDisposable disposables;

        final Map<Integer, UnicastProcessor<TRight>> lefts;

        final Map<Integer, TRight> rights;

        final AtomicReference<Throwable> error;

        final Function1<? super TLeft, ? extends Publisher<TLeftEnd>> leftEnd;

        final Function1<? super TRight, ? extends Publisher<TRightEnd>> rightEnd;

        final Function2<? super TLeft, ? super Flowable<TRight>, ? extends R> resultSelector;

        final AtomicInteger active;

        int leftIndex;

        int rightIndex;

        volatile boolean cancelled;

        static final Integer LEFT_VALUE = 1;

        static final Integer RIGHT_VALUE = 2;

        static final Integer LEFT_CLOSE = 3;

        static final Integer RIGHT_CLOSE = 4;

        GroupJoinSubscription(Subscriber<? super R> actual, Function1<? super TLeft, ? extends Publisher<TLeftEnd>> leftEnd,
                              Function1<? super TRight, ? extends Publisher<TRightEnd>> rightEnd,
                              Function2<? super TLeft, ? super Flowable<TRight>, ? extends R> resultSelector) {
            this.actual = actual;
            this.requested = new AtomicLong();
            this.disposables = new CompositeDisposable();
            this.queue = new SpscLinkedArrayQueue<Object>(bufferSize());
            this.lefts = new LinkedHashMap<Integer, UnicastProcessor<TRight>>();
            this.rights = new LinkedHashMap<Integer, TRight>();
            this.error = new AtomicReference<Throwable>();
            this.leftEnd = leftEnd;
            this.rightEnd = rightEnd;
            this.resultSelector = resultSelector;
            this.active = new AtomicInteger(2);
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            cancelAll();
            if (getAndIncrement() == 0) {
                queue.clear();
            }
        }

        void cancelAll() {
            disposables.dispose();
        }

        void errorAll(Subscriber<?> a) {
            Throwable ex = ExceptionHelper.terminate(error);

            for (UnicastProcessor<TRight> up : lefts.values()) {
                up.onError(ex);
            }

            lefts.clear();
            rights.clear();

            a.onError(ex);
        }

        void fail(Throwable exc, Subscriber<?> a, FusedQueue<?> q) {
            Exceptions.throwIfFatal(exc);
            ExceptionHelper.addThrowable(error, exc);
            q.clear();
            cancelAll();
            errorAll(a);
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            SpscLinkedArrayQueue<Object> q = queue;
            Subscriber<? super R> a = actual;

            for (;;) {
                for (;;) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    Throwable ex = error.get();
                    if (ex != null) {
                        q.clear();
                        cancelAll();
                        errorAll(a);
                        return;
                    }

                    boolean d = active.get() == 0;

                    Integer mode = (Integer)q.poll();

                    boolean empty = mode == null;

                    if (d && empty) {
                        for (UnicastProcessor<?> up : lefts.values()) {
                            up.onComplete();
                        }

                        lefts.clear();
                        rights.clear();
                        disposables.dispose();

                        a.onComplete();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    Object val = q.poll();

                    if (mode == LEFT_VALUE) {
                        @SuppressWarnings("unchecked")
                        TLeft left = (TLeft)val;

                        UnicastProcessor<TRight> up = UnicastProcessor.<TRight>create();
                        int idx = leftIndex++;
                        lefts.put(idx, up);

                        Publisher<TLeftEnd> p;

                        try {
                            p = ObjectHelper.requireNonNull(leftEnd.invoke(left), "The leftEnd returned a null Publisher");
                        } catch (Throwable exc) {
                            fail(exc, a, q);
                            return;
                        }

                        LeftRightEndSubscriber end = new LeftRightEndSubscriber(this, true, idx);
                        disposables.add(end);

                        p.subscribe(end);

                        ex = error.get();
                        if (ex != null) {
                            q.clear();
                            cancelAll();
                            errorAll(a);
                            return;
                        }

                        R w;

                        try {
                            w = ObjectHelper.requireNonNull(resultSelector.invoke(left, up), "The resultSelector returned a null value");
                        } catch (Throwable exc) {
                            fail(exc, a, q);
                            return;
                        }

                        // TODO since only left emission calls the actual, it is possible to link downstream backpressure with left's source and not error out
                        if (requested.get() != 0L) {
                            a.onNext(w);
                            BackpressureHelper.produced(requested, 1);
                        } else {
                            fail(new MissingBackpressureException("Could not emit value due to lack of requests"), a, q);
                            return;
                        }

                        for (TRight right : rights.values()) {
                            up.onNext(right);
                        }
                    }
                    else if (mode == RIGHT_VALUE) {
                        @SuppressWarnings("unchecked")
                        TRight right = (TRight)val;

                        int idx = rightIndex++;

                        rights.put(idx, right);

                        Publisher<TRightEnd> p;

                        try {
                            p = ObjectHelper.requireNonNull(rightEnd.invoke(right), "The rightEnd returned a null Publisher");
                        } catch (Throwable exc) {
                            fail(exc, a, q);
                            return;
                        }

                        LeftRightEndSubscriber end = new LeftRightEndSubscriber(this, false, idx);
                        disposables.add(end);

                        p.subscribe(end);

                        ex = error.get();
                        if (ex != null) {
                            q.clear();
                            cancelAll();
                            errorAll(a);
                            return;
                        }

                        for (UnicastProcessor<TRight> up : lefts.values()) {
                            up.onNext(right);
                        }
                    }
                    else if (mode == LEFT_CLOSE) {
                        LeftRightEndSubscriber end = (LeftRightEndSubscriber)val;

                        UnicastProcessor<TRight> up = lefts.remove(end.index);
                        disposables.remove(end);
                        if (up != null) {
                            up.onComplete();
                        }
                    }
                    else if (mode == RIGHT_CLOSE) {
                        LeftRightEndSubscriber end = (LeftRightEndSubscriber)val;

                        rights.remove(end.index);
                        disposables.remove(end);
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void innerError(Throwable ex) {
            if (ExceptionHelper.addThrowable(error, ex)) {
                active.decrementAndGet();
                drain();
            } else {
                RxJavaCommonPlugins.onError(ex);
            }
        }

        @Override
        public void innerComplete(LeftRightSubscriber sender) {
            disposables.delete(sender);
            active.decrementAndGet();
            drain();
        }

        @Override
        public void innerValue(boolean isLeft, Object o) {
            synchronized (this) {
                queue.offer(isLeft ? LEFT_VALUE : RIGHT_VALUE, o);
            }
            drain();
        }

        @Override
        public void innerClose(boolean isLeft, LeftRightEndSubscriber index) {
            synchronized (this) {
                queue.offer(isLeft ? LEFT_CLOSE : RIGHT_CLOSE, index);
            }
            drain();
        }

        @Override
        public void innerCloseError(Throwable ex) {
            if (ExceptionHelper.addThrowable(error, ex)) {
                drain();
            } else {
                RxJavaCommonPlugins.onError(ex);
            }
        }
    }

    static final class LeftRightSubscriber
    extends AtomicReference<Subscription>
    implements RelaxedSubscriber<Object>, Disposable {

        private static final long serialVersionUID = 1883890389173668373L;

        final JoinSupport parent;

        final boolean isLeft;

        LeftRightSubscriber(JoinSupport parent, boolean isLeft) {
            this.parent = parent;
            this.isLeft = isLeft;
        }

        @Override
        public void dispose() {
            SubscriptionHelper.cancel(this);
        }

        @Override
        public boolean isDisposed() {
            return SubscriptionHelper.isCancelled(get());
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(Object t) {
            parent.innerValue(isLeft, t);
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(t);
        }

        @Override
        public void onComplete() {
            parent.innerComplete(this);
        }

    }

    static final class LeftRightEndSubscriber
    extends AtomicReference<Subscription>
    implements RelaxedSubscriber<Object>, Disposable {

        private static final long serialVersionUID = 1883890389173668373L;

        final JoinSupport parent;

        final boolean isLeft;

        final int index;

        LeftRightEndSubscriber(JoinSupport parent,
                boolean isLeft, int index) {
            this.parent = parent;
            this.isLeft = isLeft;
            this.index = index;
        }

        @Override
        public void dispose() {
            SubscriptionHelper.cancel(this);
        }

        @Override
        public boolean isDisposed() {
            return SubscriptionHelper.isCancelled(get());
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(Object t) {
            if (SubscriptionHelper.cancel(this)) {
                parent.innerClose(isLeft, this);
            }
        }

        @Override
        public void onError(Throwable t) {
            parent.innerCloseError(t);
        }

        @Override
        public void onComplete() {
            parent.innerClose(isLeft, this);
        }

    }

}
