package org.zane.newpipe.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.HTTP3ClientQuicConfiguration;
import org.eclipse.jetty.http3.frames.DataFrame;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.quic.client.ClientQuicConfiguration;
import org.eclipse.jetty.quic.quiche.client.QuicheClientQuicConfiguration;
import org.eclipse.jetty.quic.quiche.client.QuicheTransport;
import org.eclipse.jetty.util.Blocker;
import org.eclipse.jetty.util.Promise;
import org.schabi.newpipe.extractor.downloader.Response;

public class Downloader
    extends org.schabi.newpipe.extractor.downloader.Downloader
{

    private HttpClient httpClient;
    private HTTP3Client http3Client;
    private HashMap<String, Session.Client> clients = new HashMap<>();
    private QuicheTransport quicheTransport;

    public Downloader() {
        try {
            ClientQuicConfiguration clientQuicConfiguration =
                HTTP3ClientQuicConfiguration.configure(
                    new ClientQuicConfiguration()
                );
            http3Client = new HTTP3Client(clientQuicConfiguration);
            http3Client.getHTTP3Configuration().setStreamIdleTimeout(15000);
            quicheTransport = new QuicheTransport(
                HTTP3ClientQuicConfiguration.configure(
                    new QuicheClientQuicConfiguration()
                )
            );
            httpClient = new HttpClient();
            httpClient.setFollowRedirects(true);
            http3Client.start();
            httpClient.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // httpClient = HttpClient.newBuilder()
        //     .version(HttpClient.Version.HTTP_2) // HTTP/2 is the default
        //     .followRedirects(HttpClient.Redirect.NORMAL)
        //     .connectTimeout(Duration.ofSeconds(10))
        //     .build();
    }

    private Session.Client connect(String hostname) throws IOException {
        Session.Client client = Blocker.blockWithPromise(p ->
            http3Client.connect(
                quicheTransport,
                new InetSocketAddress(hostname, 443),
                new Listener(hostname, p),
                p
            )
        );
        clients.put(hostname, client);
        return client;
    }

    public Response execute(
        org.schabi.newpipe.extractor.downloader.Request request
    ) throws IOException {
        byte[] dataToSend = request.dataToSend();
        URI uri = URI.create(request.url());
        String hostname = uri.getHost();
        HttpFields.Mutable header = HttpFields.build();
        request
            .headers()
            .forEach((key, values) -> {
                header.add(key, values);
            });
        MetaData.Request httpRequest = new MetaData.Request(
            request.httpMethod(),
            HttpURI.from(uri),
            HttpVersion.HTTP_3,
            header
        );
        HeadersFrame headersFrame = new HeadersFrame(httpRequest, false);
        try {
            Session.Client client;
            if (clients.containsKey(hostname)) {
                client = clients.get(hostname);
            } else {
                client = connect(hostname);
            }
            HttpResponse httpResponse = Blocker.blockWithPromise(p ->
                client.newRequest(
                    headersFrame,
                    new Stream.Client.Listener() {
                        private StringBuilder sb = new StringBuilder();
                        private MetaData.Response response;

                        @Override
                        public void onResponse(
                            Stream.Client stream,
                            HeadersFrame frame
                        ) {
                            response = (MetaData.Response) frame.getMetaData();
                            if (!frame.isLast()) {
                                // There will be content, so call demand() to have
                                // onDataAvailable() be called when the content is available.
                                stream.demand();
                            } else {
                                sb.ensureCapacity(
                                    (int) response.getContentLength()
                                );
                            }
                        }

                        @Override
                        public void onDataAvailable(Stream.Client stream) {
                            // Read a chunk of the content.
                            org.eclipse.jetty.io.Content.Chunk chunk =
                                stream.read();
                            if (chunk == null) {
                                // No data available now, demand to be called back.
                                stream.demand();
                            } else {
                                // Process the content.

                                CharBuffer c = StandardCharsets.UTF_8.decode(
                                    chunk.getByteBuffer()
                                );
                                char[] ca = c.array();
                                sb.append(ca, 0, c.length());
                                // Notify the implementation that the content has been consumed.
                                chunk.release();
                                if (chunk.isLast()) {
                                    p.succeeded(
                                        new HttpResponse(
                                            sb.toString().replaceAll("\0", " "),
                                            response
                                        )
                                    );
                                } else {
                                    stream.demand();
                                }
                            }
                        }

                        @Override
                        public void onFailure(
                            Stream.Client stream,
                            long error,
                            Throwable failure
                        ) {
                            p.failed(failure);
                        }
                    },
                    new Promise.Invocable<Stream>() {
                        @Override
                        public void succeeded(Stream stream) {
                            if (dataToSend != null) {
                                stream.data(
                                    new DataFrame(
                                        ByteBuffer.wrap(dataToSend),
                                        true
                                    ),
                                    Promise.Invocable.noop()
                                );
                            }
                        }

                        @Override
                        public void failed(Throwable x) {
                            p.failed(x);
                        }
                    }
                )
            );
            HashMap<String, List<String>> rHeader = new HashMap<>();
            httpResponse.FRAME.getHttpFields().forEach(h -> {
                try {
                    rHeader.put(h.getName(), h.getValueList());
                } catch (Exception e) {
                    ArrayList<String> headerValue = new ArrayList<>(1);
                    headerValue.add(h.getValue());
                    rHeader.put(h.getName(), headerValue);
                }
            });
            PrintWriter a = new PrintWriter(
                new FileOutputStream("abc.json"),
                true
            );
            a.write(httpResponse.BODY);
            a.close();
            return new Response(
                httpResponse.FRAME.getStatus(),
                httpResponse.FRAME.getReason(),
                rHeader,
                httpResponse.BODY,
                request.url()
            );
        } catch (IOException err) {
            err.printStackTrace();
            throw new IOException(err);
        }
        //
        // HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder(
        //     URI.create(request.url())
        // ).method(
        //     request.httpMethod(),
        //     dataToSend == null
        //         ? HttpRequest.BodyPublishers.noBody()
        //         : HttpRequest.BodyPublishers.ofByteArray(dataToSend)
        // );
        // request
        //     .headers()
        //     .forEach((key, values) -> {
        //         for (String value : values) {
        //             httpRequestBuilder.header(key, value);
        //         }
        //     });
        // HttpRequest httpRequest = httpRequestBuilder.build();

        // try {
        //     HttpResponse<String> httpResponse = httpClient.send(
        //         httpRequest,
        //         HttpResponse.BodyHandlers.ofString()
        //     );
        //     return new Response(
        //         httpResponse.statusCode(),
        //         null,
        //         httpResponse.headers().map(),
        //         httpResponse.body(),
        //         httpRequest.uri().toString()
        //     );
        // } catch (IOException | InterruptedException e) {
        //     e.printStackTrace();
        //     return null;
        // }
    }

    public static class HttpResponse {

        public final String BODY;
        public final MetaData.Response FRAME;

        public HttpResponse(String body, MetaData.Response frame) {
            BODY = body;
            FRAME = frame;
        }
    }

    public class Listener implements Session.Client.Listener {

        private final String hostname;
        private final Promise promise;

        public Listener(String hostname, Promise promise) {
            this.hostname = hostname;
            this.promise = promise;
        }

        @Override
        public void onDisconnect(Session session, long error, String reason) {
            clients.remove(hostname);
        }

        @Override
        public void onFailure(
            Session session,
            long error,
            String reason,
            Throwable failure
        ) {
            failure.printStackTrace();
            promise.failed(failure);
        }
    }
}
