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

import org.junit.Test;

import io.reactivex.observable.Completable;
import io.reactivex.observable.Single;
import io.reactivex.observable.TestHelper;
import kotlin.jvm.functions.Function1;

public class SingleFlatMapCompletableTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).flatMapCompletable(new Function1<Integer, Completable>() {
            @Override
            public Completable invoke(Integer v) {
                return Completable.complete();
            }
        }));
    }
}
