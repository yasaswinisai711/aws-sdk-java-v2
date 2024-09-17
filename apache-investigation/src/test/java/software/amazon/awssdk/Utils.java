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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.stream.Stream;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class Utils {

    public static final int KB = 1024;
    public static final int MB = KB * 1024;
    public static final int GB = MB * 1024;

    public static Stream<Arguments> s3Clients() {
        return Stream.of(
            Arguments.of(S3Client.builder().region(Region.US_WEST_2).httpClient(ApacheHttpClient.create()).build(),
                         "=== APACHE CLIENT ===")
            // , Arguments.of(S3Client.builder().region(Region.US_WEST_2).httpClient(UrlConnectionHttpClient.create()).build(),
            //              "=== URL CONNECTION CLIENT ===")
            // , Arguments.of(S3Client.builder().region(Region.US_WEST_2).httpClient(AwsCrtHttpClient.create()).build(),
            //              "=== CRT CLIENT ===")
        );
    }

    public static Stream<Arguments> httpClients() {
        return Stream.of(
            Arguments.of(ApacheHttpClient.create(), "=== APACHE CLIENT ==="),
            Arguments.of(UrlConnectionHttpClient.create(), "=== URL CONNECTION CLIENT ==="),
            Arguments.of(AwsCrtHttpClient.create(), "=== CRT CLIENT ===")
        );
    }

    public static Stream<Arguments> apache() {
        return Stream.of(
            Arguments.of(S3Client.builder().region(Region.US_WEST_2).httpClient(
                ApacheHttpClient.builder()
                                .build()).build(),
                         "=== APACHE CLIENT ===")
        );
    }

    public static Stream<Arguments> crt() {
        return Stream.of(
            Arguments.of(S3Client.builder().region(Region.US_WEST_2).httpClient(AwsCrtHttpClient.create()).build(),
                         "=== CRT CLIENT ===")
        );
    }

    public static Stream<Arguments> url() {
        return Stream.of(
            Arguments.of(S3Client.builder().region(Region.US_WEST_2).httpClient(UrlConnectionHttpClient.create()).build(),
                         "=== CRT CLIENT ===")
        );

    }

    private static class DebugSocketFactory implements ConnectionSocketFactory {
        @Override
        public Socket createSocket(HttpContext context) throws IOException {
            return null;
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
            return null;
        }
    }
}
