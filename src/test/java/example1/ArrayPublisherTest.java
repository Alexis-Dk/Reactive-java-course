package example1;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static example1.ArrayPublisher.generate;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Alexey Druzik on 4/3/2020
 */
public class ArrayPublisherTest {

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long toRequest = 555L;
        Long[] array = {0L, 1L, 2L};
        ArrayList<Long> collected = new ArrayList<>();
        ArrayList<Integer> order = new ArrayList<>();

        Subscriber<Long> subscriber = new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                order.add(0);
                s.request(toRequest);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {
                order.add(2);
                latch.countDown();
            }
        };

        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);
        publisher.subscribe(subscriber);
        latch.await(1, SECONDS);

        Assertions.assertThat(order).containsExactly(0, 2);
        Assertions.assertThat(collected).containsExactly(array);
    }

    @Test
    public void mustSupportBackpressureControl() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 5L;
        Long[] array = generate(toRequest);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);
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
}
