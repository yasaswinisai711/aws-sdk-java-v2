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

package software.amazon.awssdk.core.internal.waiters;

import static org.junit.jupiter.api.Assertions.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResponseOrExceptionTest {

    @Test
    void result_shouldBehave() {
        ResponseOrException<String> underTest = ResponseOrException.response("test value");
        assertTrue(underTest.response().isPresent());
        assertFalse(underTest.exception().isPresent());
        underTest.match(str -> assertEquals("test value", str), Assertions::fail);
        int length = underTest.either(String::length, Assertions::fail);
        assertEquals("test value".length(), length);
        String value = underTest.orElseThrow();
        assertEquals("test value", value);
        ResponseOrException<String> mapped = underTest.map(str -> str + " is mapped");

        assertTrue(underTest.response().isPresent());
        assertFalse(underTest.exception().isPresent());
        assertTrue(mapped.response().isPresent());
        assertFalse(mapped.exception().isPresent());
        mapped.match(str -> assertEquals("test value is mapped", str), Assertions::fail);
    }

    @Test
    void exception_shouldBehave() {
        ResponseOrException<String> underTest = ResponseOrException.exception(new RuntimeException("Exception msg"));
        assertFalse(underTest.response().isPresent());
        assertTrue(underTest.exception().isPresent());
        underTest.match(Assertions::fail, t -> assertEquals("Exception msg", t.getMessage()));

        int length = underTest.either(str -> -2, t -> t.getMessage().length());
        assertEquals("Exception msg".length(), length);
        assertThatThrownBy(underTest::orElseThrow).hasMessageContaining("Exception msg");
        ResponseOrException<String> mapped = underTest.map(str -> str + " is mapped");
        assertThatThrownBy(mapped::orElseThrow).hasMessageContaining("Exception msg");
    }
}