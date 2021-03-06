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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Scheduler.Worker;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.TestScheduler;
import io.reactivex.common.annotations.NonNull;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.ConnectableObservable;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.HasUpstreamObservableSource;
import io.reactivex.observable.internal.operators.ObservableReplay.BoundedReplayBuffer;
import io.reactivex.observable.internal.operators.ObservableReplay.Node;
import io.reactivex.observable.internal.operators.ObservableReplay.SizeAndTimeBoundReplayBuffer;
import io.reactivex.observable.internal.operators.ObservableReplay.SizeBoundReplayBuffer;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ObservableReplayTest {
    @Test
    public void testBufferedReplay() {
        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedWindowReplay() {
        PublishSubject<Integer> source = PublishSubject.create();
        TestScheduler scheduler = new TestScheduler();
        ConnectableObservable<Integer> co = source.replay(3, 100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onNext(5);
            scheduler.advanceTimeBy(90, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(4);

            inOrder.verify(observer1, times(1)).onNext(5);

            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(5);
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testWindowedReplay() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onComplete();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, never()).onNext(3);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testReplaySelector() {
        final Function1<Integer, Integer> dbl = new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer t1) {
                return t1 * 2;
            }

        };

        Function1<Observable<Integer>, Observable<Integer>> selector = new Function1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> invoke(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector);

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

    }

    @Test
    public void testBufferedReplaySelector() {

        final Function1<Integer, Integer> dbl = new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer t1) {
                return t1 * 2;
            }

        };

        Function1<Observable<Integer>, Observable<Integer>> selector = new Function1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> invoke(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector, 3);

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testWindowedReplaySelector() {

        final Function1<Integer, Integer> dbl = new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer t1) {
                return t1 * 2;
            }

        };

        Function1<Observable<Integer>, Observable<Integer>> selector = new Function1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> invoke(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector, 100, TimeUnit.MILLISECONDS, scheduler);

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onComplete();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedReplayError() {
        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onError(new RuntimeException("Forced failure"));

            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();
        }
    }

    @Test
    public void testWindowedReplayError() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onError(new RuntimeException("Forced failure"));
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();

        }
        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, never()).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();
        }
    }

    @Test
    public void testSynchronousDisconnect() {
        final AtomicInteger effectCounter = new AtomicInteger();
        Observable<Integer> source = Observable.just(1, 2, 3, 4)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                effectCounter.incrementAndGet();
                System.out.println("Sideeffect #" + v);
                return Unit.INSTANCE;
            }
        });

        Observable<Integer> result = source.replay(
                new Function1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Observable<Integer> o) {
                return o.take(2);
            }
        });

        for (int i = 1; i < 3; i++) {
            effectCounter.set(0);
            System.out.printf("- %d -%n", i);
            result.subscribe(new Function1<Integer, kotlin.Unit>() {

                @Override
                public Unit invoke(Integer t1) {
                    System.out.println(t1);
                    return Unit.INSTANCE;
                }

                             }, new Function1<Throwable, kotlin.Unit>() {

                @Override
                public Unit invoke(Throwable t1) {
                    t1.printStackTrace();
                    return Unit.INSTANCE;
                }
            },
                    new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    System.out.println("Done");
                    return Unit.INSTANCE;
                }
            });
            assertEquals(2, effectCounter.get());
        }
    }


    /*
     * test the basic expectation of OperatorMulticast via replay
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_UnsubscribeSource() throws Exception {
        // setup mocks
        Function1<Integer, kotlin.Unit> sourceNext = mock(Function1.class);
        Function0 sourceCompleted = mock(Function0.class);
        Function0 sourceUnsubscribed = mock(Function0.class);
        Observer<Integer> spiedSubscriberBeforeConnect = TestHelper.mockObserver();
        Observer<Integer> spiedSubscriberAfterConnect = TestHelper.mockObserver();

        // Observable under test
        Observable<Integer> source = Observable.just(1,2);

        ConnectableObservable<Integer> replay = source
                .doOnNext(sourceNext)
                .doOnDispose(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .replay();

        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.connect();
        replay.subscribe(spiedSubscriberAfterConnect);
        replay.subscribe(spiedSubscriberAfterConnect);

        verify(spiedSubscriberBeforeConnect, times(2)).onSubscribe((Disposable)any());
        verify(spiedSubscriberAfterConnect, times(2)).onSubscribe((Disposable)any());

        // verify interactions
        verify(sourceNext, times(1)).invoke(1);
        verify(sourceNext, times(1)).invoke(2);
        verify(sourceCompleted, times(1)).invoke();
        verifyObserverMock(spiedSubscriberBeforeConnect, 2, 4);
        verifyObserverMock(spiedSubscriberAfterConnect, 2, 4);

//        verify(sourceUnsubscribed, times(1)).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(spiedSubscriberBeforeConnect);
        verifyNoMoreInteractions(spiedSubscriberAfterConnect);

    }

    /**
     * Specifically test interaction with a Scheduler with subscribeOn.
     *
     * @throws Exception functional interfaces are declared with throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribe() throws Exception {
        // setup mocks
        Function1<Integer, kotlin.Unit> sourceNext = mock(Function1.class);
        Function0 sourceCompleted = mock(Function0.class);
        Function0 sourceUnsubscribed = mock(Function0.class);
        final TestScheduler mockScheduler = new TestScheduler();

        Observer<Integer> mockObserverBeforeConnect = TestHelper.mockObserver();
        Observer<Integer> mockObserverAfterConnect = TestHelper.mockObserver();

        // Observable under test
        ConnectableObservable<Integer> replay = Observable.just(1, 2, 3)
                .doOnNext(sourceNext)
                .doOnDispose(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);

        verify(mockObserverBeforeConnect).onSubscribe((Disposable)any());
        verify(mockObserverAfterConnect).onSubscribe((Disposable)any());

        mockScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        // verify interactions
        verify(sourceNext, times(1)).invoke(1);
        verify(sourceNext, times(1)).invoke(2);
        verify(sourceNext, times(1)).invoke(3);
        verify(sourceCompleted, times(1)).invoke();
        verifyObserverMock(mockObserverBeforeConnect, 1, 3);
        verifyObserverMock(mockObserverAfterConnect, 1, 3);

        // FIXME not supported
//        verify(spiedWorker, times(1)).isUnsubscribed();
        // FIXME publish calls cancel too
//        verify(sourceUnsubscribed, times(1)).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(mockObserverBeforeConnect);
        verifyNoMoreInteractions(mockObserverAfterConnect);
    }

    /**
     * Specifically test interaction with a Scheduler with subscribeOn.
     *
     * @throws Exception functional interfaces are declared with throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribeOnError() throws Exception {
        // setup mocks
        Function1<Integer, kotlin.Unit> sourceNext = mock(Function1.class);
        Function0 sourceCompleted = mock(Function0.class);
        Function1<Throwable, kotlin.Unit> sourceError = mock(Function1.class);
        Function0 sourceUnsubscribed = mock(Function0.class);
        final TestScheduler mockScheduler = new TestScheduler();
        Observer<Integer> mockObserverBeforeConnect = TestHelper.mockObserver();
        Observer<Integer> mockObserverAfterConnect = TestHelper.mockObserver();

        // Observable under test
        Function1<Integer, Integer> mockFunc = mock(Function1.class);
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(mockFunc.invoke(1)).thenReturn(1);
        when(mockFunc.invoke(2)).thenThrow(illegalArgumentException);
        ConnectableObservable<Integer> replay = Observable.just(1, 2, 3).map(mockFunc)
                .doOnNext(sourceNext)
                .doOnDispose(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .doOnError(sourceError)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);

        verify(mockObserverBeforeConnect).onSubscribe((Disposable)any());
        verify(mockObserverAfterConnect).onSubscribe((Disposable)any());


        mockScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // verify interactions
        verify(sourceNext, times(1)).invoke(1);
        verify(sourceError, times(1)).invoke(illegalArgumentException);
        verifyObserver(mockObserverBeforeConnect, 1, 1, illegalArgumentException);
        verifyObserver(mockObserverAfterConnect, 1, 1, illegalArgumentException);

        // FIXME no longer supported
//        verify(spiedWorker, times(1)).isUnsubscribed();
        // FIXME publish also calls cancel
//        verify(sourceUnsubscribed, times(1)).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceError);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(mockObserverBeforeConnect);
        verifyNoMoreInteractions(mockObserverAfterConnect);
    }

    private static void verifyObserverMock(Observer<Integer> mock, int numSubscriptions, int numItemsExpected) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onComplete();
        verifyNoMoreInteractions(mock);
    }

    private static void verifyObserver(Observer<Integer> mock, int numSubscriptions, int numItemsExpected, Throwable error) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onError(error);
        verifyNoMoreInteractions(mock);
    }

    public static Worker workerSpy(final Disposable mockDisposable) {
        return spy(new InprocessWorker(mockDisposable));
    }


    static class InprocessWorker extends Worker {
        private final Disposable mockDisposable;
        public boolean unsubscribed;

        InprocessWorker(Disposable mockDisposable) {
            this.mockDisposable = mockDisposable;
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable action) {
            action.run();
            return mockDisposable; // this subscription is returned but discarded
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable action, long delayTime, @NonNull TimeUnit unit) {
            action.run();
            return mockDisposable;
        }

        @Override
        public void dispose() {
            unsubscribed = true;
        }

        @Override
        public boolean isDisposed() {
            return unsubscribed;
        }
    }

    @Test
    public void testBoundedReplayBuffer() {
        BoundedReplayBuffer<Integer> buf = new BoundedReplayBuffer<Integer>() {
            private static final long serialVersionUID = -5182053207244406872L;

            @Override
            void truncate() {
            }
        };
        buf.addLast(new Node(1));
        buf.addLast(new Node(2));
        buf.addLast(new Node(3));
        buf.addLast(new Node(4));
        buf.addLast(new Node(5));

        List<Integer> values = new ArrayList<Integer>();
        buf.collect(values);

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), values);

        buf.removeSome(2);
        buf.removeFirst();
        buf.removeSome(2);

        values.clear();
        buf.collect(values);
        Assert.assertTrue(values.isEmpty());

        buf.addLast(new Node(5));
        buf.addLast(new Node(6));
        buf.collect(values);

        Assert.assertEquals(Arrays.asList(5, 6), values);

    }

    @Test
    public void testTimedAndSizedTruncation() {
        TestScheduler test = new TestScheduler();
        SizeAndTimeBoundReplayBuffer<Integer> buf = new SizeAndTimeBoundReplayBuffer<Integer>(2, 2000, TimeUnit.MILLISECONDS, test);
        List<Integer> values = new ArrayList<Integer>();

        buf.next(1);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.next(2);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(2), values);

        buf.next(3);
        buf.next(4);
        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(3, 4), values);

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.next(5);

        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(5), values);
        Assert.assertFalse(buf.hasCompleted());

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.complete();

        values.clear();
        buf.collect(values);
        Assert.assertTrue(values.isEmpty());

        Assert.assertEquals(1, buf.size);
        Assert.assertTrue(buf.hasCompleted());
        Assert.assertFalse(buf.hasError());
    }

    @Test
    public void testTimedAndSizedTruncationError() {
        TestScheduler test = new TestScheduler();
        SizeAndTimeBoundReplayBuffer<Integer> buf = new SizeAndTimeBoundReplayBuffer<Integer>(2, 2000, TimeUnit.MILLISECONDS, test);

        Assert.assertFalse(buf.hasCompleted());
        Assert.assertFalse(buf.hasError());

        List<Integer> values = new ArrayList<Integer>();

        buf.next(1);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.next(2);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(2), values);

        buf.next(3);
        buf.next(4);
        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(3, 4), values);

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.next(5);

        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(5), values);
        Assert.assertFalse(buf.hasCompleted());
        Assert.assertFalse(buf.hasError());

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.error(new TestException());

        values.clear();
        buf.collect(values);
        Assert.assertTrue(values.isEmpty());

        Assert.assertEquals(1, buf.size);
        Assert.assertFalse(buf.hasCompleted());
        Assert.assertTrue(buf.hasError());
    }

    @Test
    public void testSizedTruncation() {
        SizeBoundReplayBuffer<Integer> buf = new SizeBoundReplayBuffer<Integer>(2);
        List<Integer> values = new ArrayList<Integer>();

        buf.next(1);
        buf.next(2);
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(1, 2), values);

        buf.next(3);
        buf.next(4);
        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(3, 4), values);

        buf.next(5);

        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(4, 5), values);
        Assert.assertFalse(buf.hasCompleted());

        buf.complete();

        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(4, 5), values);

        Assert.assertEquals(3, buf.size);
        Assert.assertTrue(buf.hasCompleted());
        Assert.assertFalse(buf.hasError());
    }

    @Test
    public void testColdReplayNoBackpressure() {
        Observable<Integer> source = Observable.range(0, 1000).replay().autoConnect();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        source.subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        List<Integer> onNextEvents = ts.values();
        assertEquals(1000, onNextEvents.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
    }

    @Test
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Observable<String> o = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(final Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published Observable being executed");
                        observer.onNext("one");
                        observer.onComplete();
                    }
                }).start();
            }
        }).replay().autoConnect();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Function1<String, kotlin.Unit>() {

            @Override
            public Unit invoke(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
                return Unit.INSTANCE;
            }
        });

        // subscribe again
        o.subscribe(new Function1<String, kotlin.Unit>() {

            @Override
            public Unit invoke(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
                return Unit.INSTANCE;
            }
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void testUnsubscribeSource() throws Exception {
        Function0 unsubscribe = mock(Function0.class);
        Observable<Integer> o = Observable.just(1).doOnDispose(unsubscribe).cache();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        verify(unsubscribe, times(1)).invoke();
    }

    @Test
    public void testTake() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        Observable<Integer> cached = Observable.range(1, 100).replay().autoConnect();
        cached.take(10).subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        // FIXME no longer assertable
//        ts.assertUnsubscribed();
    }

    @Test
    public void testAsync() {
        Observable<Integer> source = Observable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            TestObserver<Integer> ts1 = new TestObserver<Integer>();

            Observable<Integer> cached = source.replay().autoConnect();

            cached.observeOn(Schedulers.computation()).subscribe(ts1);

            ts1.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts1.assertNoErrors();
            ts1.assertTerminated();
            assertEquals(10000, ts1.values().size());

            TestObserver<Integer> ts2 = new TestObserver<Integer>();
            cached.observeOn(Schedulers.computation()).subscribe(ts2);

            ts2.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts2.assertNoErrors();
            ts2.assertTerminated();
            assertEquals(10000, ts2.values().size());
        }
    }
    @Test
    public void testAsyncComeAndGo() {
        Observable<Long> source = Observable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        Observable<Long> cached = source.replay().autoConnect();

        Observable<Long> output = cached.observeOn(Schedulers.computation());

        List<TestObserver<Long>> list = new ArrayList<TestObserver<Long>>(100);
        for (int i = 0; i < 100; i++) {
            TestObserver<Long> ts = new TestObserver<Long>();
            list.add(ts);
            output.skip(i * 10).take(10).subscribe(ts);
        }

        List<Long> expected = new ArrayList<Long>();
        for (int i = 0; i < 10; i++) {
            expected.add((long)(i - 10));
        }
        int j = 0;
        for (TestObserver<Long> ts : list) {
            ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertTerminated();

            for (int i = j * 10; i < j * 10 + 10; i++) {
                expected.set(i - j * 10, (long)i);
            }

            ts.assertValueSequence(expected);

            j++;
        }
    }

    @Test
    public void testNoMissingBackpressureException() {
        final int m = 4 * 1000 * 1000;
        Observable<Integer> firehose = Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> t) {
                t.onSubscribe(Disposables.empty());
                for (int i = 0; i < m; i++) {
                    t.onNext(i);
                }
                t.onComplete();
            }
        });

        TestObserver<Integer> ts = new TestObserver<Integer>();
        firehose.replay().autoConnect().observeOn(Schedulers.computation()).takeLast(100).subscribe(ts);

        ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminated();

        assertEquals(100, ts.values().size());
    }

    @Test
    public void testValuesAndThenError() {
        Observable<Integer> source = Observable.range(1, 10)
                .concatWith(Observable.<Integer>error(new TestException()))
                .replay().autoConnect();


        TestObserver<Integer> ts = new TestObserver<Integer>();
        source.subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNotComplete();
        Assert.assertEquals(1, ts.errors().size());

        TestObserver<Integer> ts2 = new TestObserver<Integer>();
        source.subscribe(ts2);

        ts2.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts2.assertNotComplete();
        Assert.assertEquals(1, ts2.errors().size());
    }

    @Test
    @Ignore("onNext should not throw")
    public void unsafeChildThrows() {
        final AtomicInteger count = new AtomicInteger();

        Observable<Integer> source = Observable.range(1, 100)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer t) {
                count.getAndIncrement();
                return Unit.INSTANCE;
            }
        })
        .replay().autoConnect();

        TestObserver<Integer> ts = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };

        source.subscribe(ts);

        Assert.assertEquals(100, count.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void replayScheduler() {

        Observable.just(1).replay(Schedulers.computation())
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replayTime() {
        Observable.just(1).replay(1, TimeUnit.MINUTES)
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySizeScheduler() {

        Observable.just(1).replay(1, Schedulers.computation())
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySizeAndTime() {
        Observable.just(1).replay(1, 1, TimeUnit.MILLISECONDS)
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorSizeScheduler() {
        Observable.just(1).replay(Functions.<Observable<Integer>>identity(), 1, Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorScheduler() {
        Observable.just(1).replay(Functions.<Observable<Integer>>identity(), Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorTime() {
        Observable.just(1).replay(Functions.<Observable<Integer>>identity(), 1, TimeUnit.MINUTES)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replayMaxInt() {
        Observable.range(1, 2)
        .replay(Integer.MAX_VALUE)
        .autoConnect()
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void source() {
        Observable<Integer> source = Observable.range(1, 3);

        assertSame(source, (((HasUpstreamObservableSource<?>)source.replay())).source());
    }

    @Test
    public void connectRace() {
        for (int i = 0; i < 500; i++) {
            final ConnectableObservable<Integer> co = Observable.range(1, 3).replay();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    co.connect();
                }
            };

            TestCommonHelper.race(r, r);
        }
    }

    @Test
    public void subscribeRace() {
        for (int i = 0; i < 500; i++) {
            final ConnectableObservable<Integer> co = Observable.range(1, 3).replay();

            final TestObserver<Integer> to1 = new TestObserver<Integer>();
            final TestObserver<Integer> to2 = new TestObserver<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to2);
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 500; i++) {
            final ConnectableObservable<Integer> co = Observable.range(1, 3).replay();

            final TestObserver<Integer> to1 = new TestObserver<Integer>();
            final TestObserver<Integer> to2 = new TestObserver<Integer>();

            co.subscribe(to1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to2);
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void cancelOnArrival() {
        Observable.range(1, 2)
        .replay(Integer.MAX_VALUE)
        .autoConnect()
        .test(true)
        .assertEmpty();
    }

    @Test
    public void cancelOnArrival2() {
        ConnectableObservable<Integer> co = PublishSubject.<Integer>create()
        .replay(Integer.MAX_VALUE);

        co.test();

        co
        .autoConnect()
        .test(true)
        .assertEmpty();
    }

    @Test
    public void connectConsumerThrows() {
        ConnectableObservable<Integer> co = Observable.range(1, 2)
        .replay();

        try {
            co.connect(new Function1<Disposable, kotlin.Unit>() {
                @Override
                public Unit invoke(Disposable t) {
                    throw new TestException();
                }
            });
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }

        co.test().assertEmpty().cancel();

        co.connect();

        co.test().assertResult(1, 2);
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onError(new TestException("First"));
                    observer.onNext(1);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }.replay()
            .autoConnect()
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void subscribeOnNextRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final ConnectableObservable<Integer> co = ps.replay();

            final TestObserver<Integer> to1 = new TestObserver<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        ps.onNext(j);
                    }
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void unsubscribeOnNextRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final ConnectableObservable<Integer> co = ps.replay();

            final TestObserver<Integer> to1 = new TestObserver<Integer>();

            co.subscribe(to1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to1.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        ps.onNext(j);
                    }
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void unsubscribeReplayRace() {
        for (int i = 0; i < 500; i++) {
            final ConnectableObservable<Integer> co = Observable.range(1, 1000).replay();

            final TestObserver<Integer> to1 = new TestObserver<Integer>();

            co.connect();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to1.dispose();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void reentrantOnNext() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    ps.onNext(2);
                    ps.onComplete();
                }
                super.onNext(t);
            }
        };

        ps.replay().autoConnect().subscribe(to);

        ps.onNext(1);

        to.assertResult(1, 2);
    }

    @Test
    public void reentrantOnNextBound() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    ps.onNext(2);
                    ps.onComplete();
                }
                super.onNext(t);
            }
        };

        ps.replay(10).autoConnect().subscribe(to);

        ps.onNext(1);

        to.assertResult(1, 2);
    }

    @Test
    public void reentrantOnNextCancel() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    ps.onNext(2);
                    cancel();
                }
                super.onNext(t);
            }
        };

        ps.replay().autoConnect().subscribe(to);

        ps.onNext(1);

        to.assertValues(1);
    }

    @Test
    public void reentrantOnNextCancelBounded() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    ps.onNext(2);
                    cancel();
                }
                super.onNext(t);
            }
        };

        ps.replay(10).autoConnect().subscribe(to);

        ps.onNext(1);

        to.assertValues(1);
    }

    @Test
    public void delayedUpstreamOnSubscribe() {
        final Observer<?>[] sub = { null };

        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> s) {
                sub[0] = s;
            }
        }
        .replay()
        .connect()
        .dispose();

        Disposable bs = Disposables.empty();

        sub[0].onSubscribe(bs);

        assertTrue(bs.isDisposed());
    }

    @Test
    public void timedNoOutdatedData() {
        TestScheduler scheduler = new TestScheduler();

        Observable<Integer> source = Observable.just(1)
                .replay(2, TimeUnit.SECONDS, scheduler)
                .autoConnect();

        source.test().assertResult(1);

        source.test().assertResult(1);

        scheduler.advanceTimeBy(3, TimeUnit.SECONDS);

        source.test().assertResult();
    }
}
