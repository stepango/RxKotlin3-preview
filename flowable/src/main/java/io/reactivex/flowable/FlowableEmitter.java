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

package io.reactivex.flowable;

import io.reactivex.common.Disposable;
import io.reactivex.common.Emitter;
import io.reactivex.common.annotations.NonNull;
import io.reactivex.common.annotations.Nullable;
import kotlin.jvm.functions.Function0;

/**
 * Abstraction over a Reactive Streams {@link org.reactivestreams.Subscriber} that allows associating
 * a resource with it and exposes the current number of downstream
 * requested amount.
 * <p>
 * The onNext, onError and onComplete methods should be called
 * in a sequential manner, just like the Subscriber's methods.
 * Use {@link #serialize()} if you want to ensure this.
 * The other methods are thread-safe.
 *
 * @param <T> the value type to emit
 */
public interface FlowableEmitter<T> extends Emitter<T> {

    /**
     * Sets a Disposable on this emitter; any previous Disposable
     * or Cancellation will be disposed/cancelled.
     * @param s the disposable, null is allowed
     */
    void setDisposable(@Nullable Disposable s);

    /**
     * Sets a Cancellable on this emitter; any previous Disposable
     * or Cancellation will be disposed/cancelled.
     * @param c the cancellable resource, null is allowed
     */
    void setCancellable(@Nullable Function0 c);

    /**
     * The current outstanding request amount.
     * <p>This method is thread-safe.
     * @return the current outstanding request amount
     */
    long requested();

    /**
     * Returns true if the downstream cancelled the sequence.
     * <p>This method is thread-safe.
     * @return true if the downstream cancelled the sequence
     */
    boolean isCancelled();

    /**
     * Ensures that calls to onNext, onError and onComplete are properly serialized.
     * @return the serialized FlowableEmitter
     */
    @NonNull
    FlowableEmitter<T> serialize();
}
