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

package software.amazon.awssdk.core.internal.checksums.factory;

import java.util.Optional;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ClassLoaderHelper;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class ChecksumProvider {

    public static final Logger LOG = Logger.loggerFor(ChecksumProvider.class);

    private static final String CRT_CLASSPATH_FOR_CRC32C = "software.amazon.awssdk.crt.checksums.CRC32C";
    private static final String CRT_CLASSPATH_FOR_CRC32 = "software.amazon.awssdk.crt.checksums.CRC32";
    private static final String JAVA_VERSION_CLASSPATH_FOR_CRC32C = "java.util.zip.CRC32C";

    private static final String CRT_LOG_MESSAGE = "Cannot find the CRT-based checksum class: ";
    private static final String JAVA_LOG_MESSAGE = "Cannot find the Java-based checksum class: %s."
                                                   + " This feature requires Java 9 or later.";

    private static final Lazy<Optional<Class<?>>> CRT_CRC32_CLASS_LOADER =
        new Lazy<>(() -> initializeChecksumClass(CRT_CLASSPATH_FOR_CRC32, CRT_LOG_MESSAGE + CRT_CLASSPATH_FOR_CRC32));
    private static final Lazy<Optional<Class<?>>> CRT_CRC32_C_CLASS_LOADER =
        new Lazy<>(() -> initializeChecksumClass(CRT_CLASSPATH_FOR_CRC32C, CRT_LOG_MESSAGE + CRT_CLASSPATH_FOR_CRC32C));
    private static final Lazy<Optional<Class<?>>> JAVA_CRC32_C_CLASS_LOADER =
        new Lazy<>(() -> initializeChecksumClass(JAVA_VERSION_CLASSPATH_FOR_CRC32C,
                                                 String.format(JAVA_LOG_MESSAGE, JAVA_VERSION_CLASSPATH_FOR_CRC32C)));

    private ChecksumProvider() {
    }

    public static Checksum createCrc32() {
        return createChecksum(CRT_CRC32_CLASS_LOADER);
    }

    public static Checksum createCrtCrc32C() {
        return createChecksum(CRT_CRC32_C_CLASS_LOADER);
    }

    public static Checksum createJavaCrc32C() {
        return createChecksum(JAVA_CRC32_C_CLASS_LOADER);
    }

    private static Checksum createChecksum(Lazy<Optional<Class<?>>> lazyClassLoader) {
        return lazyClassLoader.getValue().map(checksumClass -> {
            try {
                return (Checksum) checksumClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }).orElse(null);
    }

    private static Optional<Class<?>> initializeChecksumClass(String classPath, String logMessage) {
        try {
            return Optional.of(ClassLoaderHelper.loadClass(classPath, false));
        } catch (ClassNotFoundException e) {
            LOG.debug(() -> logMessage, e);
            return Optional.empty();
        }
    }
}
