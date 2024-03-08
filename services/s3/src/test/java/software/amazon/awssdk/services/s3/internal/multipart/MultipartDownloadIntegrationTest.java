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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Checksum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.internal.checksums.factory.SdkCrc32;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.MapUtils;

// TODO(multipart download): remove before release
// WIP - please ignore for now, only used in manually testing
class MultipartDownloadIntegrationTest {
    private static final Logger log = Logger.loggerFor(MultipartDownloadIntegrationTest.class);

    // CHANGE ME TO:
    //   24
    // OR
    //   238
    static final int fileTestSize = 24;

    static final String bucket = "olapplin-test-bucket";
    static final String key = String.format("debug-test-%smb", fileTestSize);

    private S3AsyncClient s3;

    @BeforeEach
    void init() {
        this.s3 = S3AsyncClient.builder()
                               .region(Region.US_WEST_2)
                               .multipartEnabled(true)
                               .multipartConfiguration(c -> c.apiCallBufferSizeInBytes(1024L * 32))
                               .credentialsProvider(ProfileCredentialsProvider.create())
                               .httpClient(NettyNioAsyncHttpClient.create())
                               .build();
    }

    @Test
    void testByteAsyncResponseTransformer() {
        CompletableFuture<ResponseBytes<GetObjectResponse>> response = s3.getObject(
            r -> r.bucket(bucket).key(key),
            AsyncResponseTransformer.toBytes());
        ResponseBytes<GetObjectResponse> res = response.join();
        log.info(() -> "complete");
        byte[] bytes = res.asByteArray();
        log.info(() -> String.format("Byte len: %s", bytes.length));
        assertThat(bytes).hasSize(fileTestSize * 1024 * 1024);
    }

    @Test
    void testFileAsyncResponseTransformer() throws Exception {
        Path path = Paths.get("/Users/olapplin/Develop/tmp", key);
        CompletableFuture<GetObjectResponse> future = s3.getObject(
            r -> r.bucket(bucket).key(key),
            AsyncResponseTransformer.toFile(path,
                b -> b.fileWriteOption(FileTransformerConfiguration.FileWriteOption.CREATE_OR_REPLACE_EXISTING)
                    .failureBehavior(FileTransformerConfiguration.FailureBehavior.LEAVE)));
        log.info(() -> "complete");
        assertTrue(path.toFile().exists());
        assertThat(path.toFile()).hasSize(fileTestSize * 1024 * 1024);
    }

    @Test
    void testPublisherAsyncResponseTransformer() {
        CompletableFuture<ResponsePublisher<GetObjectResponse>> future = s3.getObject(
            r -> r.bucket(bucket).key(key),
            AsyncResponseTransformer.toPublisher());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger total = new AtomicInteger(0);
        List<Byte> bytes = new ArrayList<>();
        future.whenComplete((res, e) -> {
            log.info(() -> "complete");
            res.subscribe(new Subscriber<ByteBuffer>() {
                Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                    this.subscription = s;
                    s.request(1);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    total.addAndGet(byteBuffer.remaining());
                    while (byteBuffer.hasRemaining()) {
                        bytes.add(byteBuffer.get());
                    }
                    subscription.request(1);
                }
                @Override
                public void onError(Throwable t) {
                    fail("unexpected error in test", t);
                    latch.countDown();
                }

                @Override
                public void onComplete() {
                    log.info(() -> "complete subscriber");
                    latch.countDown();
                }
            });
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertThat(total).hasValue(fileTestSize * 1024 * 1024);
    }

    @Test
    void testBlockingInputStreamResponseTransformer() {
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = s3.getObject(
            r -> r.bucket(bucket).key(key),
            AsyncResponseTransformer.toBlockingInputStream());
        ResponseInputStream<GetObjectResponse> res = future.join();
        List<Byte> bytes = new ArrayList<>();
        log.info(() -> "complete");
        int total = 0;
        try {
            int b;
            while ((b = res.read()) != -1) {
                bytes.add((byte)b);
                total++;
            }
        } catch (IOException e) {
            fail(e);
        }
        assertThat(total).isEqualTo(fileTestSize * 1024 * 1024);
    }

}
