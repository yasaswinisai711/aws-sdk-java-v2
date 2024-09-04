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

import static software.amazon.awssdk.Utils.KB;
import static software.amazon.awssdk.Utils.MB;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.VoiceId;

public class PollyInputStreamTest {

    @ParameterizedTest
    @MethodSource("software.amazon.awssdk.Utils#httpClients")
    void pollyCloseTest(SdkHttpClient httpClient, String clientName) throws Exception {
        System.out.println(clientName);
        PollyClient polly = PollyClient.builder()
                                       .region(Region.US_EAST_1)
                                       .httpClient(httpClient)
                                       .build();

        SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                                                                 .text(getText())
                                                                 .voiceId(VoiceId.RUTH)
                                                                 .outputFormat(OutputFormat.PCM)
                                                                 .engine(Engine.GENERATIVE)
                                                                 .build();

        ResponseInputStream<SynthesizeSpeechResponse> is =
            polly.synthesizeSpeech(request, ResponseTransformer.toInputStream());

        int size = KB;
        System.out.printf("READING TO BUFFER - %d kb%n", size/KB);
        byte[] block = new byte[size];
        ByteStreams.readFully(is, block);
        System.out.println("CLOSE");
        long startTime = System.nanoTime();
        try {
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("done close in %s ms%n", Duration.ofNanos(System.nanoTime() - startTime).toMillis());

    }

    @ParameterizedTest
    @MethodSource("software.amazon.awssdk.Utils#httpClients")
    void pollyAbortTest(SdkHttpClient httpClient, String clientName) throws Exception {
        System.out.println(clientName);
        PollyClient polly = PollyClient.builder()
                                       .region(Region.US_EAST_1)
                                       .httpClient(httpClient)
                                       .build();

        SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                                                                 .text(getText())
                                                                 .voiceId(VoiceId.RUTH)
                                                                 .outputFormat(OutputFormat.PCM)
                                                                 .engine(Engine.GENERATIVE)
                                                                 .build();
        ResponseInputStream<SynthesizeSpeechResponse> is =
            polly.synthesizeSpeech(request, ResponseTransformer.toInputStream());

        int size = KB;
        System.out.printf("READING TO BUFFER - %d kb%n", size/KB);
        byte[] block = new byte[size];
        ByteStreams.readFully(is, block);
        System.out.println("CLOSE");
        long startTime = System.nanoTime();
        is.abort();
        System.out.printf("done close in %s ms%n", Duration.ofNanos(System.nanoTime() - startTime).toMillis());
    }

    private String getText() {
        // Pennies from Heaven
        return "And every time it rains, it rains\n"
               + "Pennies from Heaven (shooby dooby)\n"
               + "Don't you know each cloud contains\n"
               + "Pennies from Heaven? (Shooby dooby)\n"
               + "You'll find your fortune falling all over town\n"
               + "Be sure that your umbrella is up, up, up, up upside down, and\n"
               + "Trade them for a package of sunshine and ravioli (macaroni)\n"
               + "If you want the things you love, you must have a pizzaioli, baby\n"
               + "And when you hear thunder, don't run under a tree\n"
               + "It'll be pennies from Heaven, for you and for me\n"
               + "Now come over here boy, Sam\n"
               + "And every time it rains, it rains\n"
               + "And don't you know each cloud contains?\n"
               + "Every time it rains, it rains\n"
               + "And don't you know each cloud contains?\n"
               + "You find your fortune falling\n"
               + "All over town, all over town, all over town\n"
               + "Be sure that your umbrella is upside down twiddly bop\n"
               + "I knew I'd get ya, I knew I'd get ya\n"
               + "Let's go, let's go, let's go\n"
               + "And you'll find your fortune falling all over town\n"
               + "It'll be pennies from Heaven\n"
               + "Pennies from Heaven\n"
               + "The pennies from Heaven\n"
               + "For you and me";
    }
}
