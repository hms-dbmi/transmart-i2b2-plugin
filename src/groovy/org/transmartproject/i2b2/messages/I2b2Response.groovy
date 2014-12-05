package org.transmartproject.i2b2.messages

import groovy.util.logging.Log4j
import groovy.util.slurpersupport.NodeChildren
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.transmartproject.core.exceptions.UnexpectedResultException

@Log4j
class I2b2Response {
    private slurperResult

    I2b2Response(HttpEntity entity) {
        def contentType = ContentType.getOrDefault(entity)

        entity.content.withStream {
            def reader = new InputStreamReader(it, contentType.charset)

            if (log.isDebugEnabled()) {
                def content = reader.text
                log.debug("Building i2b2 response with content:\n$content")
                reader = new StringReader(content)
            }

            slurperResult = new XmlSlurper(false, true, false)
                    .parse(reader)
        }
    }

    NodeChildren getBody() {
        slurperResult.message_body
    }

    Status getStatus() {
        Status.values().find {
            it.name() ==
                    slurperResult?.response_header?.result_status?.status?.'@type'.text()
        } ?: Status.UNKNOWN
    }

    void throwIfNotDone() {
        if (status != Status.DONE) {
            // TODO: include error message
            throw new UnexpectedResultException(
                    "Expected status DONE in i2b2 response; got $status")
        }
    }

    enum Status {
        DONE,
        PENDING,
        ERROR,

        UNKNOWN
    }
}
