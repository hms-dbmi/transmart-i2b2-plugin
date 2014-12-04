package org.transmartproject.i2b2.mock

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * This is a test class, but it's here for technical reasons.
 */
@Log4j
class MockI2b2 {

    HttpServer server

    ExecutorService executorService

    @Value('#{grailsApplication.config.org.transmartproject.i2b2.instance.port}')
    int port

    private callsMock

    private AssertionError lastError

    @PostConstruct
    void init() {
        executorService = Executors.newCachedThreadPool()

        InetSocketAddress addr = new InetSocketAddress(port)
        server = HttpServer.create(addr, 0)

        server.with {
            createContext('/', new MockI2b2Handler())
            executor = executorService
            start()
        }

        log.info("Mock i2b2 is listening on port $port")
    }

    void reset(Object callsMock) {
        this.callsMock = callsMock
        this.lastError = null
    }

    void maybeThrowLastError() {
        if (lastError) {
            throw lastError
        }
    }

    @PreDestroy
    void destroy() {
        server.stop(3 /* seconds */)
        executorService.shutdownNow()
    }

    class MockI2b2Handler implements HttpHandler {

        @Override
        void handle(HttpExchange exchange) throws IOException {
            String body = exchange.requestBody.getText('UTF-8')

            try {
                String response = callsMock.withRequest(body)
                exchange.responseHeaders.add(
                        'Content-type', 'application/xml; charset=UTF-8')
                def responseBytes = response.getBytes('UTF-8')
                exchange.sendResponseHeaders(200, responseBytes.length)
                exchange.responseBody.withStream {
                    it << responseBytes
                }
            } catch (AssertionError e) {
                /* we cannot let the error propagate on this thread.
                 * save it so it can be rethrown in the main thread */
                lastError = e
                exchange.sendResponseHeaders(500, 0)
                exchange.responseBody.close()
            }
        }
    }
}
