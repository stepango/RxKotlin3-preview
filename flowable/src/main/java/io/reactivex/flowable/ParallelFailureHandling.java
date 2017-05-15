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

import io.reactivex.common.annotations.Experimental;
import kotlin.jvm.functions.Function2;

/**
 * Enumerations for handling failure within a parallel operator.
 * @since 2.0.8 - experimental
 */
@Experimental
public enum ParallelFailureHandling implements Function2<Long, Throwable, ParallelFailureHandling> {
    /**
     * The current rail is stopped and the error is dropped.
     */
    STOP,
    /**
     * The current rail is stopped and the error is signalled.
     */
    ERROR,
    /**
     * The current value and error is ignored and the rail resumes with the next item.
     */
    SKIP,
    /**
     * Retry the current value.
     */
    RETRY;

    @Override
    public ParallelFailureHandling invoke(Long t1, Throwable t2) {
        return this;
    }
}
