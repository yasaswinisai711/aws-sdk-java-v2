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

package software.amazon.awssdk;

import com.google.common.io.ByteStreams;
import java.time.Duration;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class ApacheStreamLatencyTest {

    static final int MB = 1024 * 1024;

    static String bucket = "olapplin-test-bucket";
    static String key = "test-key-1724783487614.dat";

    @ParameterizedTest
    @MethodSource("software.amazon.awssdk.Utils#apache")
    void v2request(S3Client s3Client, String client) throws Exception {
        // doClose(s3Client, client);
        doAbort(s3Client, client);
    }

    static void doClose(S3Client s3Client, String client) throws Exception {
        System.out.println(client);
        System.out.println("GETTING FILE");
        GetObjectRequest getRequest = GetObjectRequest.builder()
                                                      .bucket(bucket)
                                                      .key(key)
                                                      .range("bytes=0-209715199") // 200 MB
                                                      .build();
        ResponseInputStream<GetObjectResponse> ris = s3Client.getObject(getRequest, ResponseTransformer.toInputStream());
        readBlock(ris, 20 * MB, is -> {
            System.out.println("ABORT");
            long startTime = System.nanoTime();
            try {
                is.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println("done abort in " + Duration.ofNanos(System.nanoTime() - startTime).toMillis() + "ms");
            // BytesReadTrackingInputStream bris = (BytesReadTrackingInputStream) is.getAbortable();
            // System.out.printf("Tracked bytes: %,d%n", bris.bytesRead());
        });
    }

    static void doAbort(S3Client s3Client, String client) throws Exception {
        System.out.println(client);
        System.out.println("GETTING FILE");
        GetObjectRequest getRequest = GetObjectRequest.builder()
                                                      .bucket(bucket)
                                                      .key(key)
                                                      .range("bytes=0-209715199") // 200 MB
                                                      .build();
        ResponseInputStream<GetObjectResponse> ris = s3Client.getObject(getRequest, ResponseTransformer.toInputStream());
        readBlock(ris, 20 * MB, is -> {
            System.out.println("CLOSE");
            long startTime = System.nanoTime();
            try {
                is.abort();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println("done close in " + Duration.ofNanos(System.nanoTime() - startTime).toMillis() + "ms");
            // BytesReadTrackingInputStream bris = (BytesReadTrackingInputStream) is.getAbortable();
            // System.out.printf("Tracked bytes: %,d%n", bris.bytesRead());

        }); // 20 mb;
    }

    static void readBlock(ResponseInputStream<GetObjectResponse> is, int size, Consumer<ResponseInputStream<?>> doWith) throws Exception {
        System.out.println("READING TO BUFFER");
        byte[] megablock = new byte[size];
        ByteStreams.readFully(is, megablock);
        doWith.accept(is);
    }

}
