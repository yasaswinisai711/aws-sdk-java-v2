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

package software.amazon.awssdk.core.internal.checksum;

import static org.apache.logging.log4j.Level.DEBUG;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.zip.Checksum;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.internal.checksums.factory.ChecksumProvider;
import software.amazon.awssdk.testutils.LogCaptor;

class ChecksumProviderTest {

    @Test
    void testCrc32_NotLoadedWhenCrtClassNotAvailable() {
        try (LogCaptor logCaptor = LogCaptor.create()) {
            Checksum checksum = ChecksumProvider.createCrc32();
            assertThat(checksum).isNull();
            List<LogEvent> events = logCaptor.loggedEvents();
            assertLogged(events, DEBUG, "Cannot find the CRT-based checksum class: "
                                        + "software.amazon.awssdk.crt.checksums.CRC32");
            assertThat(events).isEmpty();
        }
    }

    @Test
    void testCrc32C_NotLoadedWhenCrtClassNotAvailable() {
        try (LogCaptor logCaptor = LogCaptor.create()) {
            Checksum checksum = ChecksumProvider.createCrtCrc32C();
            assertThat(checksum).isNull();
            List<LogEvent> events = logCaptor.loggedEvents();
            assertLogged(events, DEBUG, "Cannot find the CRT-based checksum class: "
                                        + "software.amazon.awssdk.crt.checksums.CRC32C");
            assertThat(events).isEmpty();
        }
    }

    @Test
    void testJavaCrc32C_LoadsWhenJavaClassIsAvailable() {
        Assumptions.assumeTrue(isJava9OrLater());
        Checksum checksum = ChecksumProvider.createJavaCrc32C();
        assertThat(checksum).isNotNull();
    }

    @Test
    void testJavaCrc32C_NotLoadedWhenJavaClassNotAvailable() {
        if(!isJava9OrLater()) {
            try (LogCaptor logCaptor = LogCaptor.create()) {
                Checksum checksum = ChecksumProvider.createJavaCrc32C();
                assertThat(checksum).isNull();
                List<LogEvent> events = logCaptor.loggedEvents();
                assertLogged(events, DEBUG, "Cannot find the Java-based checksum class: java.util.zip.CRC32C. "
                                            + "This feature requires Java 9 or later.");
                assertThat(events).isEmpty();
            }
        }
    }

    private boolean isJava9OrLater() {
        String javaVersion = System.getProperty("java.version");
        return javaVersion.startsWith("9") || javaVersion.startsWith("1.9") ||
               Integer.parseInt(javaVersion.split("\\.")[0]) >= 9;
    }

    private static void assertLogged(List<LogEvent> events, org.apache.logging.log4j.Level level, String message, Object... args) {
        assertThat(events).withFailMessage("Expecting events to not be empty").isNotEmpty();
        LogEvent event = events.remove(0);
        String msg = event.getMessage().getFormattedMessage();
        assertThat(msg).isEqualTo(String.format(message, args));
        assertThat(event.getLevel()).isEqualTo(level);
    }
}
