package com.athaydes.rawhttp.core;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;

import static com.athaydes.rawhttp.core.RawHttpHeaders.Builder.emptyRawHttpHeaders;

/**
 * An eager specialization of {@link RawHttpResponse}.
 * <p>
 * Normally, an instance of this class is obtained by calling {@link RawHttpResponse#eagerly()}.
 * Doing that guarantees that the response is fully downloaded or read from its source, so that
 * it can be passed around even after the connection or stream it originates from has been closed.
 *
 * @param <Response> library response type
 */
public class EagerHttpResponse<Response> extends RawHttpResponse<Response> {

    private EagerHttpResponse(RawHttpResponse<Response> response,
                              RawHttpHeaders headers,
                              @Nullable EagerBodyReader bodyReader) throws IOException {
        super(response.getLibResponse().orElse(null),
                response.getRequest().orElse(null),
                response.getStartLine(),
                headers,
                bodyReader
        );
    }

    /**
     * @param response   raw HTTP response
     * @param <Response> library response type
     * @return an eager HTTP response from the given HTTP response.
     * @throws IOException if an error occurs while reading the response.
     */
    public static <Response> EagerHttpResponse<Response> from(RawHttpResponse<Response> response)
            throws IOException {
        if (response instanceof EagerHttpResponse) {
            return (EagerHttpResponse<Response>) response;
        }

        @Nullable EagerBodyReader bodyReader = response.getBody().isPresent() ?
                response.getBody().get().eager() :
                null;

        RawHttpHeaders headers;
        if (bodyReader != null) {
            RawHttpHeaders trailingHeaders = bodyReader.asChunkedBodyContents()
                    .map(ChunkedBodyContents::getTrailerHeaders)
                    .orElse(emptyRawHttpHeaders());
            headers = RawHttpHeaders.Builder.newBuilder(response.getHeaders())
                    .merge(trailingHeaders)
                    .build();
        } else {
            headers = response.getHeaders();
        }

        return new EagerHttpResponse<>(response, headers, bodyReader);
    }

    /**
     * @return this eager response.
     */
    @Override
    public EagerHttpResponse<Response> eagerly() {
        return this;
    }

    @Override
    public Optional<EagerBodyReader> getBody() {
        Optional<? extends BodyReader> body = super.getBody();
        return body.map(b -> (EagerBodyReader) b);
    }

}
