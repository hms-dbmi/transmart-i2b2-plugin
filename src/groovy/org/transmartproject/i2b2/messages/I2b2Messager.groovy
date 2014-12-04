package org.transmartproject.i2b2.messages

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import groovy.util.logging.Log4j
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.concurrent.FutureCallback
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.transmartproject.core.exceptions.UnexpectedResultException

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.util.concurrent.CancellationException

@Service
@Log4j
class I2b2Messager {
    public static final ContentType XML_UTF8 = ContentType.create('application/xml', 'UTF-8')

    @Autowired
    private GrailsApplication grailsApplication

    @Autowired
    private I2b2MessageFactory messageFactory

    private volatile Map<I2b2CellType, I2b2Cell> registeredCells

    private CloseableHttpAsyncClient httpClient

    @PostConstruct
    private void startHttpClient() {
        PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(
                new DefaultConnectingIOReactor())
        cm.with {
            maxTotal = poolConfig.maxTotal ?: 30
            defaultMaxPerRoute = poolConfig.maxPerRoute ?: 30
        }

        httpClient = HttpAsyncClients.custom()
                .setConnectionManager(cm)
                .build()
        httpClient.start()
    }

    @PreDestroy
    private void closeHttpClient() {
        log.info "Destroying http client"
        httpClient.close()
    }

    void setRegisteredCells(Map<I2b2CellType, I2b2Cell> cells) {
        log.info "Registered ${cells.size()} i2b2 cells: ${cells.values()}"
        registeredCells = cells
    }

    /**
     * Send a message to an I2b2 cell. Sending messages to cells other than
     * the PM cell requires {@link #registeredCells} to be set.
     * @param envelope the message envelope
     * @return the future response
     */
    ListenableFuture<I2b2Response> sendMessage(I2b2MessageEnvelope envelope) {
        SettableFuture<I2b2Response> resp = new SettableFuture<>()

        def httpPost = httpPostForEnvelope(envelope)
        def entity = new StringEntity(envelope.message.toString(), XML_UTF8)
        httpPost.entity = entity
        log.debug("About to make request with: $httpPost, message: ${envelope.message.toString()}")

        httpClient.execute(httpPost,
                new FutureCallback<HttpResponse>() {
                    @Override
                    void completed(HttpResponse httpResponse) {
                        log.debug("Request for $httpPost completed " +
                                "with status ${httpResponse.statusLine}")
                        if (httpResponse.statusLine.statusCode != 200) {
                            //EntityUtils.consume(httpResponse.entity)
                            failed(new UnexpectedResultException(
                                    'Response status was not 200, it was instead ' +
                                            httpResponse.statusLine.statusCode +
                                    ". Content: ${httpResponse.entity.content.text}"))
                            return
                        }

                        resp.set(new I2b2Response(httpResponse.entity))
                    }

                    @Override
                    void failed(Exception e) {
                        log.error("Request for $httpPost failed")
                        resp.exception = new UnexpectedResultException(
                                "Failed getting result from i2b2: ${e.message}", e)
                    }

                    @Override
                    void cancelled() {
                        log.warn("Request for $httpPost cancelled")
                        resp.exception = new CancellationException(
                                "Request for $resp was cancelled")
                    }
                })

        resp
    }

    private HttpPost httpPostForEnvelope(I2b2MessageEnvelope envelope) {
        def url
        if (envelope.cell == I2b2CellType.PRJOJECT_MANAGEMENT) {
            url = PMServiceUrl
        } else {
            if (!registeredCells) {
                throw new IllegalStateException("The cell locations have " +
                        "not been fetched from the PM cell")
            }

            if (!registeredCells[envelope.cell]) {
                throw new UnsupportedOperationException(
                        "Cell not available: ${envelope.cell}")
            }

            url = registeredCells[envelope.cell].url
        }

        new HttpPost(url + envelope.service)
    }


    private getInstanceConfig() {
        grailsApplication.config.org.transmartproject.i2b2.instance
    }

    private getPoolConfig() {
        grailsApplication.config.org.transmartproject.i2b2.pool
    }

    private String getPMServiceUrl() {
        "http://${instanceConfig.host}:${instanceConfig.port}/" +
                "${instanceConfig.context}/services/PMService/"
    }
}
