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

import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.TestHelper;
import kotlin.jvm.functions.Function1;

public class MaybeFlattenTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Maybe.just(1).flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(2);
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function1<Maybe<Integer>, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Maybe<Integer> v) {
                return v.flatMap(new Function1<Integer, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> invoke(Integer v) {
                        return Maybe.just(2);
                    }
                });
            }
        });
    }

    @Test
    public void mainError() {
        Maybe.<Integer>error(new TestException())
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> invoke(Integer v) {
                        return Maybe.just(2);
                    }
                })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainEmpty() {
        Maybe.<Integer>empty()
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> invoke(Integer v) {
                        return Maybe.just(2);
                    }
                })
        .test()
        .assertResult();
    }

    @Test
    public void mapperThrows() {
        Maybe.just(1)
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> invoke(Integer v) {
                        throw new TestException();
                    }
                })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperReturnsNull() {
        Maybe.just(1)
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> invoke(Integer v) {
                        return null;
                    }
                })
        .test()
        .assertFailure(NullPointerException.class);
    }
}
