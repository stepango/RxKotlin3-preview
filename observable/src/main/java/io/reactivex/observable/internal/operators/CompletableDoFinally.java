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

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.annotations.Experimental;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.CompletableSource;
import kotlin.jvm.functions.Function0;

/**
 * Execute an action after an onError, onComplete or a dispose event.
 *
 * @since 2.0.1 - experimental
 */
@Experimental
public final class CompletableDoFinally extends Completable {

    final CompletableSource source;

    final Function0 onFinally;

    public CompletableDoFinally(CompletableSource source, Function0 onFinally) {
        this.source = source;
        this.onFinally = onFinally;
    }

    @Override
    protected void subscribeActual(CompletableObserver s) {
        source.subscribe(new DoFinallyObserver(s, onFinally));
    }

    static final class DoFinallyObserver extends AtomicInteger implements CompletableObserver, Disposable {

        private static final long serialVersionUID = 4109457741734051389L;

        final CompletableObserver actual;

        final Function0 onFinally;

        Disposable d;

        DoFinallyObserver(CompletableObserver actual, Function0 onFinally) {
            this.actual = actual;
            this.onFinally = onFinally;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
            runFinally();
        }

        @Override
        public void onComplete() {
            actual.onComplete();
            runFinally();
        }

        @Override
        public void dispose() {
            d.dispose();
            runFinally();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }

        void runFinally() {
            if (compareAndSet(0, 1)) {
                try {
                    onFinally.invoke();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaCommonPlugins.onError(ex);
                }
            }
        }
    }
}
