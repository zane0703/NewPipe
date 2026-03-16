package org.zane.newpipe.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;

public class Downloader
    extends org.schabi.newpipe.extractor.downloader.Downloader
{

    private HttpClient httpClient;

    public Downloader() {
        httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2) // HTTP/2 is the default
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public Response execute(Request request) {
        byte[] dataToSend = request.dataToSend();
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder(
            URI.create(request.url())
        ).method(
            request.httpMethod(),
            dataToSend == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(dataToSend)
        );
        request
            .headers()
            .forEach((key, values) -> {
                for (String value : values) {
                    httpRequestBuilder.header(key, value);
                }
            });
        HttpRequest httpRequest = httpRequestBuilder.build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
            );
            return new Response(
                httpResponse.statusCode(),
                null,
                httpResponse.headers().map(),
                httpResponse.body(),
                httpRequest.uri().toString()
            );
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
