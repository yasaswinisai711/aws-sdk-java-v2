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

package software.amazon.awssdk.transfer.s3.progress;

import com.google.common.base.Stopwatch;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

class DebugTest {

    @Test
    void debugTest() throws Exception {
        S3TransferManager transferManager =
            S3TransferManager.builder()
                             .s3Client(S3AsyncClient
                                           .crtBuilder()
                                           .region(Region.US_WEST_2)
                                           .targetThroughputInGbps(2.0)
                                           .minimumPartSizeInBytes(8 * 1024 * 1024L)
                                           .build())
                             .build();

        Stopwatch stopwatch = Stopwatch.createStarted();
        UploadRequest uploadRequest = UploadRequest.builder()
                                                   .putObjectRequest(
                                                       b -> b.bucket("olapplin-test-bucket").key("debug-test-24mb"))
                                                   .addTransferListener(LoggingTransferListener.create(10))
                                                   .requestBody(AsyncRequestBody.fromFile(Paths.get("/Users/olapplin/24MB")))
                                                   .build();

        CompletableFuture<CompletedUpload> future = transferManager.upload(uploadRequest).completionFuture();
        CompletedUpload completedUpload = future.join();
        stopwatch.stop();
        System.out.printf("upload completed, interval: %s , result[eTag]: %s%n",
                          stopwatch.elapsed(),
                          completedUpload.response().eTag());
    }
}
