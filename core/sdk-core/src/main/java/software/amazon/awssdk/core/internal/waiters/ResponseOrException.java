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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a value that can be either a response or a Throwable
 * note: for Java 17, seal this interface and only permits Response and ExceptionResponse
 * <pre>{@code public sealed interface ResponseOrException<R> permits Response, ExceptionResponse}</pre>
 * Example usage:
 * {@snippet :
 *     ResponseOrException<String>responseOrException = ResponseOrException.response("Hello world");
 *
 *     // either
 *     String message = responseOrException.either(
 *         res   -> "Got a response: " + res,
 *         error -> "Got an error: " + error.getMessage()
 *     );
 *     System.out.println(message); // >>> Got a response:Hello world
 *
 *     // match
 *     responseOrException.match(
 *         res   -> System.out.println("Response in ResponseOrException: " + res),
 *         error -> System.err.println("Error found in ResponseOrException: " + error.getMessage())
 *     ); // >>> Response in ResponseOrException: Hello world
 *
 *     // map and orElseThrow
 *     ResponseOrException<List<String>> responseSplit = Arrays.asList(responseOrException.map(msg->msg.split(" ")));
 *     responseSplit.orElseThrow().forEach(str -> System.out.println(str))
 *}
 * @param <R> response type
 */
@SdkPublicApi
public interface ResponseOrException<R> {

    /**
     * Create a new ResponseOrException with the response
     *
     * @param value response
     * @param <R> Response type
     */
    static <R> ResponseOrException<R> response(R value) {
        return new Response<>(value);
    }

    /**
     * Create a new ResponseOrException with the exception
     *
     * @param value exception
     * @param <R> Response type
     */
    static <R> ResponseOrException<R> exception(Throwable value) {
        return new ExceptionResponse<>(value);
    }

    /**
     * @return the optional response that has matched with the waiter success condition
     */
    Optional<R> response();

    /**
     * @return the optional exception that has matched with the waiter success condition
     */
    Optional<Throwable> exception();

    /**
     * Applies the mapper function to the response, if present. Do nothing if it is an error.
     * @param mapper the function to be applied
     * @return an instance of ResponseOrException with a value mapped to the new type, as prescribed by the mapping function
     * @param <T> the new type of the response, as prescribed by the mapping function
     */
    <T> ResponseOrException<T> map(Function<R, T> mapper);

    /**
     * Applies either of the specified fucntion, depending wether the instance is Result or Exception, returning a mapped
     * value.
     * @param onResponse function to be run if the instance in a Response.
     * @param onException function to be run if the instance in Exception.
     * @return Some value, from applying whichever function.
     */
    <T> T either(Function<R, T> onResponse, Function<Throwable, T> onException);

    /**
     * @param onResponse will be run if the Response is present, passing in the response to the consumer.
     * @param onException will be run if there is no response, passing in the Throwable
     */
    void match(Consumer<R> onResponse, Consumer<Throwable> onException);

    /**
     * @return the Response value if it's present, or throw the Throwable instance if there is no response.
     */
    R orElseThrow();

    final class Response<R> implements ResponseOrException<R> {
        private final R value;

        public R getValue() {
            return this.value;
        }

        public Response(R value) {
            Validate.notNull(value, "Value for Response must not be null");
            this.value = value;
        }

        @Override
        public Optional<R> response() {
            return Optional.of(value);
        }

        @Override
        public Optional<Throwable> exception() {
            return Optional.empty();
        }

        @Override
        public <T> ResponseOrException<T> map(Function<R, T> mapper) {
            Validate.notNull(mapper, "mapper Function must not be null");
            return new Response<>(mapper.apply(this.value));
        }

        @Override
        public <T> T either(Function<R, T> onResponse, Function<Throwable, T> onException) {
            Validate.notNull(onResponse, "onResponse Function must not be null");
            return onResponse.apply(this.value);
        }

        @Override
        public void match(Consumer<R> onResponse, Consumer<Throwable> onException) {
            Validate.notNull(onResponse, "onResponse Consumer must not be null");
            onResponse.accept(this.value);
        }

        @Override
        public R orElseThrow() {
            return this.value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Response<?> response = (Response<?>) o;

            return Objects.equals(value, response.value);
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    final class ExceptionResponse<R> implements ResponseOrException<R> {
        private final Throwable throwable;

        public Throwable getThrowable() {
            return this.throwable;
        }

        public ExceptionResponse(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public Optional<R> response() {
            return Optional.empty();
        }

        @Override
        public Optional<Throwable> exception() {
            return Optional.of(throwable);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseOrException<T> map(Function<R, T> mapper) {
            return (ResponseOrException<T>) this;
        }

        @Override
        public <T> T either(Function<R, T> onResponse, Function<Throwable, T> onException) {
            Validate.notNull(onException, "onException Function must not be null");
            return onException.apply(this.throwable);
        }

        @Override
        public void match(Consumer<R> onResponse, Consumer<Throwable> onException) {
            Validate.notNull(onException, "onException Consumer must not be null");
            onException.accept(this.throwable);
        }

        @Override
        public R orElseThrow() {
            throw new RuntimeException(this.throwable);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ExceptionResponse<?> that = (ExceptionResponse<?>) o;

            return Objects.equals(throwable, that.throwable);
        }

        @Override
        public int hashCode() {
            return throwable != null ? throwable.hashCode() : 0;
        }
    }
}
