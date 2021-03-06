package example1;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static example2.ArrayPublisher.generate;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Alexey Druzik on 4/3/2020
 */
public class ArrayPublisherTest extends PublisherVerification<Long> {

    public ArrayPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return new example5.ArrayPublisher<>(generate(elements));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }

    @Test
    public void signalsShouldBeEmittedInTheRightOrder() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        ArrayList<Integer> order = new ArrayList<>();
        long toRequest = 5L;
        Long[] array = generate(toRequest);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                order.add(0);
                s.request(toRequest);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);

                if (!order.contains(1)) {
                    order.add(1);
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                order.add(2);
                latch.countDown();
            }
        });

        latch.await(1, SECONDS);

        Assertions.assertThat(order).containsExactly(0, 1, 2);
        Assertions.assertThat(collected).containsExactly(array);
    }

    @Test
    public void mustSupportBackpressureControl() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 5L;
        Long[] array = example3.ArrayPublisher.generate(toRequest);
        example2.ArrayPublisher<Long> publisher = new example2.ArrayPublisher<>(array);
        Subscription[] subscription = new Subscription[1];

        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscription[0] = s;
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        Assertions.assertThat(collected).isEmpty();

        subscription[0].request(1);
        Assertions.assertThat(collected).containsExactly(0L);

        subscription[0].request(1);
        Assertions.assertThat(collected).containsExactly(0L, 1L);

        subscription[0].request(2);
        Assertions.assertThat(collected).containsExactly(0L, 1L, 2L, 3L);

        subscription[0].request(20);

        Assertions.assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();

        Assertions.assertThat(collected).containsExactly(array);
    }

    @Test
    public void mustSendNPENormally() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Long[] array = new Long[] { null };
        AtomicReference<Throwable> error = new AtomicReference<>();
        example3.ArrayPublisher<Long> publisher = new example3.ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(4);
            }

            @Override
            public void onNext(Long aLong) {
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
                latch.countDown();
            }

            @Override
            public void onComplete() {
            }
        });

        latch.await(1, SECONDS);

        Assertions.assertThat(error.get()).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldNotDieInStackOverflow() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 1000L;
        Long[] array = generate(toRequest);
        example4.ArrayPublisher<Long> publisher = new example4.ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);

                s.request(1);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        latch.await(5, SECONDS);

        Assertions.assertThat(collected).containsExactly(array);
    }

    @Test
    public void shouldBePossibleToCancelSubscription() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 1000L;
        Long[] array = generate(toRequest);
        example5.ArrayPublisher<Long> publisher = new example5.ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {

            @Override
            public void onSubscribe(Subscription s) {
                s.cancel();
                s.request(toRequest);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        latch.await(1, SECONDS);

        Assertions.assertThat(collected).isEmpty();
    }

    @Test
    public void multithreadingTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        final int n = 5000;
        Long[] array = generate(n);
        example6.ArrayPublisher<Long> publisher = new example6.ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {
            private Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                for (int i = 0; i < n; i++) {
                    commonPool().execute(() -> s.request(1));
                }
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        latch.await(2, SECONDS);

        Assertions.assertThat(collected).containsExactly(array);
    }
}
