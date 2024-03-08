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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.utils.Logger;

public class ManualTmTests {
    private static final Logger log = Logger.loggerFor(ManualTmTests.class);
    static final int fileTestSize = 128;
    static final String bucket = "olapplin-test-bucket";
    static final String key = String.format("debug-test-%smb", fileTestSize);

    private S3TransferManager tm;

    @BeforeEach
    void init() {
        tm = S3TransferManager.builder()
                              .s3Client(S3AsyncClient.builder()
                                                     .multipartEnabled(true)
                                                     .region(Region.US_WEST_2)
                                                     .build())
                              .build();
    }

    @Test
    void testTM() {
        Path path = Paths.get("/Users/olapplin/Develop/tmp",
                              ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT) + "_" + key);
        FileDownload dl = tm.downloadFile(b -> b.getObjectRequest(r -> r.bucket(bucket).key(key))
                                                .destination(path));
        dl.completionFuture().join();
        assertTrue(path.toFile().exists());
        assertThat(path.toFile()).hasSize(fileTestSize * 1024 * 1024);
    }
}
