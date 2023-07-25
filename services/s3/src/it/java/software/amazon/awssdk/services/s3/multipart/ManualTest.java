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

package software.amazon.awssdk.services.s3.multipart;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * run:
 * <p>
 *     {@code ada credentials update --account <YOUR_PERSONAL_ACCOUNT>} --provider=isengard --role=Admin --once}
 * </p>
 * first then run the test in intellij or with maven:
 * <p>
 *     {@code mvn clean test -Dtest=ManualTest -pl :s3}
 * </p>
 *
 *
 */
class ManualTest {

    private static final String TEST_FILE_PATH = //// change me
        "/Users/olapplin/Develop/GitHub/aws-sdk-java-v2/services/s3/src/main/resources/codegen-resources/service-2.json";
    private static final String TEST_BUCKET = //// change me
        "olapplin-test-bucket";

    @Test
    void testWithFullConfig() throws Exception {
        S3AsyncClient client = S3AsyncClient
            .builder()
            .multipartConfiguration(c -> c.multipartEnabled(true) // default value
                                          .minimumPartSizeInBytes(8L * 1024 * 1024) // default value
                                          .thresholdInBytes(8L * 1024 * 1024) // default value
                                          .maximumMemoryUsageInBytes(16L * 1024 * 1024) // default value
                                          .multipartDownloadType(MultipartDownloadType.PART)) // unused
            .region(Region.US_WEST_2)
            .build();

        CompletableFuture<PutObjectResponse> responseFuture = client.putObject(
            req -> req.bucket(TEST_BUCKET).key("test-multipart-upload.txt"),
            Paths.get(TEST_FILE_PATH));

        PutObjectResponse response = responseFuture.get();
        System.out.println(response.toString());
    }

    @Test
    void testMinimumConfig() throws Exception {
        S3AsyncClient client = S3AsyncClient
            .builder()
            .multipartConfiguration(MultipartConfiguration.create()) // will enable multipart
            // .multipartConfiguration(c -> c.multipartEnabled(true)) // does the same
            .region(Region.US_WEST_2)
            .build();

        CompletableFuture<CopyObjectResponse> responseFuture = client.copyObject(
            c -> c.sourceBucket(TEST_BUCKET).destinationBucket("olapplin-tmp-upload")
                .sourceKey("test-multipart-upload.txt").destinationKey("test-multipart-upload-copy.txt"));

        CopyObjectResponse response = responseFuture.get();
        System.out.println(response.toString());
    }

   @Test
   void getObject_isDisabled() {
       S3AsyncClient client = S3AsyncClient
           .builder()
           .multipartConfiguration(MultipartConfiguration.create()) // will enable multipart
           // .multipartConfiguration(c -> c.multipartEnabled(true)) // does the same
           .region(Region.US_WEST_2)
           .build();


   }
}
