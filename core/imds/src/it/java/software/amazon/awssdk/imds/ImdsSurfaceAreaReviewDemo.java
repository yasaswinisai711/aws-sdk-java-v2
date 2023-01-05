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

package software.amazon.awssdk.imds;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

public class ImdsSurfaceAreaReviewDemo {

    WireMockServer server;

    public ImdsSurfaceAreaReviewDemo(WireMockServer server) {
        this.server = server;
        initStubs(server);
    }

    public static void main(String[] args) {
        WireMockServer server = new WireMockServer();
        ImdsSurfaceAreaReviewDemo demo = new ImdsSurfaceAreaReviewDemo(server);
        try {
            server.start();
            demo.defaultSyncClientDemo();
            demo.customSyncClientDemo();
            demo.defaultAsyncClientDemo();
            demo.customAsyncClientDemo();
        } finally {
            server.stop();
        }
    }

    void initStubs(WireMockServer server) {
        server.stubFor(put(urlPathEqualTo("/latest/api/token")).willReturn(
            aResponse().withBody("ABCD-1234-EFGH")
                       .withHeader("x-aws-ec2-metadata-token-ttl-seconds", "3600")));
        server.stubFor(get(urlPathEqualTo("/latest/meta-data/value")).willReturn(
            aResponse().withBody("key1=value1\nkey2=value2")));
        server.stubFor(get(urlPathEqualTo("/latest/meta-data/json")).willReturn(
            aResponse().withBody("{\"jsonKey1\":\"jsonValue1\", \"jsonKey2\":\"jsonValue2\"}")));
        server.stubFor(get(urlPathEqualTo("/errors/unknown-path")).willReturn(
            aResponse().withStatus(404).withBody("404 Not Found")));
        server.stubFor(get(urlPathEqualTo("/errors/server-error")).willReturn(
            aResponse().withStatus(500).withBody("500 Internal Server Error")));


    }

    // ===== SYNC DEMO =====

    void defaultSyncClientDemo() {
        Ec2MetadataClient client = Ec2MetadataClient.create();
        // we have to override the endpoint because wiremock is used for the surface area review.
        // Users usually won't need this for a default client, `Ec2MetadataClient.create()` is enough.
        client = Ec2MetadataClient.builder()
                                  .endpoint(URI.create("http://localhost:" + this.server.port()))
                                  .build();

        System.out.println();
        System.out.println("=== Running demo with default sync client === ");
        runSyncDemo(client);
    }

    void customSyncClientDemo() {
        // You can play with the client builder here
        Ec2MetadataClient client = Ec2MetadataClient
            .builder()
            .endpoint(URI.create("http://localhost:" + this.server.port()))
            .retryPolicy(Ec2MetadataRetryPolicy.none()) // no retries
            .httpClient(UrlConnectionHttpClient.builder() // default HttpClient
                                               .connectionTimeout(Duration.ofSeconds(5))
                                               .socketTimeout(Duration.ofSeconds(10))
                                               .build())
            .tokenTtl(Duration.ofMinutes(5))
            .build();

        System.out.println();
        System.out.println("=== Running demo with custom sync client === ");
        runSyncDemo(client);
    }



    // you can play with the response here in this method
    void runSyncDemo(Ec2MetadataClient client) {

        // ==========================
        // Happy path
        MetadataResponse response = client.get("/latest/meta-data/value");
        System.out.printf("response as String:%n%s%n", response.asString());
        System.out.printf("response as List: %s%n", response.asList());
        MetadataResponse jsonResponse = client.get("/latest/meta-data/json");
        System.out.printf("response as Json: %s%n", jsonResponse.asDocument());

        // ===========================
        // Errors

        // 404
        try {
            MetadataResponse errorResponse = client.get("/errors/unknown-path");
        } catch (Exception e) {
            System.err.printf("[ERROR] %s%n", e.getMessage());
            // e.printStackTrace();
        }

        // Malformed URI
        try {
            MetadataResponse errorResponse = client.get("invalid path");
        } catch (Exception e) {
            System.err.printf("[ERROR] %s%n", e.getMessage());
            // e.printStackTrace();
        }

        // 500 Internal Server Error
        try {
            MetadataResponse errorResponse = client.get("/errors/server-error");
        } catch (Exception e) {
            System.err.printf("[ERROR] %s%n", e.getMessage());
            // e.printStackTrace();
        }

    }


    // ===== ASYNC DEMO =====

    void defaultAsyncClientDemo() {
        Ec2MetadataAsyncClient client = Ec2MetadataAsyncClient.create();
        // we have to override the endpoint because wiremock is used for the surface area review,
        // users usually don't need this for the default client, `Ec2MetadataClient.create()` is enough.
        client = Ec2MetadataAsyncClient.builder()
                                       .endpoint(URI.create("http://localhost:" + this.server.port()))
                                       .build();

        System.out.println();
        System.out.println("=== Running demo with default async client === ");
        runAsyncDemo(client);
    }

    void customAsyncClientDemo() {
        Ec2MetadataAsyncClient client = Ec2MetadataAsyncClient
            .builder()
            .endpoint(URI.create("http://localhost:" + this.server.port()))
            .retryPolicy(Ec2MetadataRetryPolicy.none()) // no retries
            .httpClient(NettyNioAsyncHttpClient.builder() // default HttpClient
                                               .connectionTimeout(Duration.ofSeconds(5))
                                               .readTimeout(Duration.ofSeconds(10))
                                               .build())
            .tokenTtl(Duration.ofMinutes(5))
            .build();

        System.out.println();
        System.out.println("=== Running demo with custom async client === ");
        runAsyncDemo(client);
    }

    void runAsyncDemo(Ec2MetadataAsyncClient client) {
        // ==========================
        // Happy path
        CompletableFuture<MetadataResponse> future = client.get("/latest/meta-data/value");
        future.thenAccept(response -> {
            System.out.printf("response as String:%n%s%n", response.asString());
            System.out.printf("response as List: %s%n", response.asList());
        });

        CompletableFuture<MetadataResponse> jsonFuture = client.get("/latest/meta-data/json");
        jsonFuture.thenAccept(jsonResponse -> {
            System.out.printf("response as Json: %s%n", jsonResponse.asDocument());
        });

        future.join();
        jsonFuture.join();

        // ===========================
        // Errors

        // 404
        CompletableFuture<MetadataResponse> errorFuture = client.get("/errors/unknown-path");
        errorFuture.whenComplete((res, err) -> {
            System.err.printf("[ERROR] %s%n", err.getMessage());
            // err.printStackTrace();
        });

        // 500
        CompletableFuture<MetadataResponse> serverErrorFuture = client.get("/errors/server-error");
        serverErrorFuture.whenComplete((res, err) -> {
            System.err.printf("[ERROR] %s%n", err.getMessage());
            // err.printStackTrace();
        });

        CompletableFuture<MetadataResponse> invalidPathFuture = client.get("invalid path");
        invalidPathFuture.whenComplete((res, err) -> {
            System.err.printf("[ERROR] %s%n", err.getMessage());
            // err.printStackTrace();
        });

        // note: we could .join() the future to wait for them to finish, but it would throw an exception, as those future will be
        // completed exceptionally. Here instead, just for fun and for demonstration purpose, we can manually wait for the
        // future to finish. This shows that the call to the .get(...) method actually starts the request process and
        // eventually completes in a background thread without having to call .join() for them to complete.
        waitToComplete(errorFuture, serverErrorFuture, invalidPathFuture);
    }

    void waitToComplete(Future<?> ...futures) {
        List<Future<?>> futureList = Arrays.asList(futures);
        if (!allCompleted(futureList)) {
            while (!allCompleted(futureList)) {
                try {
                    System.out.println("waiting...");
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

    private boolean allCompleted(List<Future<?>> futures) {
        return futures.stream().allMatch(Future::isDone);
    }
}
