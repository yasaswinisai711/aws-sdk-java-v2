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

import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.utils.async.SequentialSubscriber;

class DemandCapturingSubscriberTckTest extends SubscriberWhiteboxVerification<Object> {
    protected DemandCapturingSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<Object> createSubscriber(WhiteboxSubscriberProbe<Object> probe) {
        Subscriber<Object> delegate = new SequentialSubscriber<>(o -> {
        }, new CompletableFuture<>());
        return new DemandCapturingSubscriber<Object>(delegate) {
            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                probe.registerOnSubscribe(new SubscriberPuppet() {
                    @Override
                    public void triggerRequest(long l) {
                        s.request(l);
                    }

                    @Override
                    public void signalCancel() {
                        s.cancel();
                    }
                });
            }

            @Override
            public void onNext(Object bb) {
                super.onNext(bb);
                probe.registerOnNext(bb);
            }

            @Override
            public void onError(Throwable t) {
                super.onError(t);
                probe.registerOnError(t);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                probe.registerOnComplete();
            }

        };
    }

    @Override
    public Object createElement(int element) {
        return new Object();
    }
}