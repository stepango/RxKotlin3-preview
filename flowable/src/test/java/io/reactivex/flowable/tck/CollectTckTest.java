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

package io.reactivex.flowable.tck;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import java.util.List;

import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

@Test
public class CollectTckTest extends BaseTck<List<Integer>> {

    @Override
    public Publisher<List<Integer>> createPublisher(final long elements) {
        return
                Flowable.range(1, 1000).collect(Functions.<Integer>createArrayList(128), new Function2<List<Integer>, Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(List<Integer> a, Integer b) {
                        a.add(b);
                        return Unit.INSTANCE;
                    }
                })
            ;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1;
    }
}
