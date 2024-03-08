/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core.internal.async;

import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.async.DelegatingSubscriber;

/**
 * A delegating subscriber that capture the amount of demand the delegate sends to the publisher
 * it subscribes to.
 */
@SdkInternalApi
public class DemandCapturingSubscriber<T> extends DelegatingSubscriber<T, T> {
    private final AtomicLong demand = new AtomicLong(0);

    public DemandCapturingSubscriber(Subscriber<? super T> subscriber) {
        super(subscriber);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscriber.onSubscribe(new DemandCapturingSubscription(subscription));
    }

    @Override
    public void onNext(T elem) {
        subscriber.onNext(elem);
        demand.decrementAndGet();
    }

    final class DemandCapturingSubscription implements Subscription {
        private final Subscription s;

        private DemandCapturingSubscription(Subscription s) {
            this.s = s;
        }

        @Override
        public void request(long n) {
            // log.info(() -> "received upstream demand: " + n);
            demand.updateAndGet(current -> {
                if (Long.MAX_VALUE - current < n) {
                    return Long.MAX_VALUE;
                }
                return current + n;
            });
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }
    }

    public long demand() {
        return demand.get();
    }

}
