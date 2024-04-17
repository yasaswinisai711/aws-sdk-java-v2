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

package software.amazon.awssdk.s3benchmarks;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;

/**
 * A no-op {@link AsyncResponseTransformer}
 */
public class NoOpResponseTransformer<T> implements AsyncResponseTransformer<T, Object> {
    private static final Logger log = Logger.loggerFor(NoOpResponseTransformer.class);

    private CompletableFuture<Object> future;

    @Override
    public CompletableFuture<Object> prepare() {
        log.info(() -> "prepare()");
        future = new CompletableFuture<>();
        return future;
    }

    @Override
    public void onResponse(T response) {
        log.info(() -> "onResponse()");
        // do nothing
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        log.info(() -> "onStream()");
        publisher.subscribe(new NoOpSubscriber(future));
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        log.info(() -> "exceptionOccurred()");
        future.completeExceptionally(error);
    }

    static class NoOpSubscriber implements Subscriber<ByteBuffer> {
        private final CompletableFuture<Object> future;
        private Subscription subscription;

        NoOpSubscriber(CompletableFuture<Object> future) {
            this.future = future;
        }

        @Override
        public void onSubscribe(Subscription s) {
            log.info(() -> "onSubscribe()");
            this.subscription = s;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            log.info(() -> "onNext()");
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            log.info(() -> "onError()");
            future.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            log.info(() -> "onComplete()");
            future.complete(new Object());
        }
    }

}
