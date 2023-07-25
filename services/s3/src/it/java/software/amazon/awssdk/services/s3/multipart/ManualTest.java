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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    //// change these value for your setup
    private static final String TEST_FILE_PATH = //// change me
        // "/Users/olapplin/Develop/GitHub/aws-sdk-java-v2/services/s3/src/main/resources/codegen-resources/service-2.json";
        "/Users/olapplin/Develop/512M";

    private static final String TEST_FILE_PATH_DESTINATION = //// change me
        "/Users/olapplin/Develop/destination-key.txt";

    private static final String TEST_BUCKET = //// change me
        "olapplin-test-bucket";

    private static final String TEST_BUCKET_DESTINATION = //// change me
        "olapplin-test-2-bucket";

    @Test
    void putObject_withFullConfig() throws Exception {
        S3AsyncClient client = S3AsyncClient
            .builder()
            .multipartConfiguration(c -> c.multipartEnabled(true) // default value
                                          .minimumPartSizeInBytes(128L * 1024 * 1024)
                                          .thresholdInBytes(8L * 1024 * 1024)
                                          .maximumMemoryUsageInBytes(128L * 3 * 1024 * 1024)
                                          .multipartDownloadType(MultipartDownloadType.PART))
            .region(Region.US_WEST_2)
            .build();

        CompletableFuture<PutObjectResponse> responseFuture = client.putObject(
            req -> req.bucket(TEST_BUCKET).key("test-multipart-upload.txt"),
            Paths.get(TEST_FILE_PATH));

        PutObjectResponse response = responseFuture.get();
        System.out.println(response.toString());
    }

    @Test
    void copyObject_withMinimumConfig() throws Exception {
        S3AsyncClient client = S3AsyncClient
            .builder()
            .multipartConfiguration(MultipartConfiguration.create()) // will enable multipart
            // .multipartConfiguration(c -> c.multipartEnabled(true)) // does the same
            .region(Region.US_WEST_2)
            .build();

        CompletableFuture<CopyObjectResponse> responseFuture = client.copyObject(
            c -> c.sourceBucket(TEST_BUCKET).destinationBucket(TEST_BUCKET_DESTINATION)
                  .sourceKey("test-multipart-upload.txt").destinationKey("test-multipart-upload-copy.txt"));

        CopyObjectResponse response = responseFuture.get();
        System.out.println(response.toString());
    }

    @Test
    void getObject_shouldThrowException() {
        S3AsyncClient client = S3AsyncClient
            .builder()
            .multipartConfiguration(MultipartConfiguration.create()) // will enable multipart
            // .multipartConfiguration(c -> c.multipartEnabled(true)) // does the same
            .region(Region.US_WEST_2)
            .build();

        assertThatThrownBy(() -> client.getObject(
            b -> b.bucket(TEST_BUCKET)
                  .key("test-multipart-upload.txt"),
            Paths.get(TEST_FILE_PATH_DESTINATION))).as("Multipart download currently not supported.")
                                                   .isInstanceOf(UnsupportedOperationException.class);
    }
}
