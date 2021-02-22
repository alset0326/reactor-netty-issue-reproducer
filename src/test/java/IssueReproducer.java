import org.junit.jupiter.api.*;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.ConnectionProvider;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IssueReproducer {
    private DisposableServer disposableServer;

    @BeforeAll
    public void setup() {
        HttpServer server = HttpServer.create()
                .host("0.0.0.0")
                .handle((httpServerRequest, httpServerResponse) -> httpServerResponse.sendString(Mono.just("ok")));
        server.warmup().block();
        disposableServer = server.bindNow();
    }

    @AfterAll
    public void shutdown() {
        disposableServer.dispose();
        disposableServer.onDispose().block();
    }

    @Test
    public void test() {
        // should create HttpClient instance every time
        // should use ConnectionProvider.newConnection() to trigger the bug
        Mono.defer(() -> HttpClient.create(ConnectionProvider.newConnection())
                .host(disposableServer.host())
                .port(disposableServer.port())
                .baseUrl("/")
                .get()
                .response())
                .repeat(Integer.MAX_VALUE)
                .blockLast();
    }

}
