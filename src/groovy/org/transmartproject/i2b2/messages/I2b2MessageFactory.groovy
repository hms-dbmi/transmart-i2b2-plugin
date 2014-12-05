package org.transmartproject.i2b2.messages

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.xml.SimpleNamespaceContext

import javax.xml.namespace.NamespaceContext

@Component
class I2b2MessageFactory {
    final static NamespaceContext NAMESPACE_CONTEXT =
            new SimpleNamespaceContext(
                    bindings: ImmutableMap.of(
                            'i2b2', 'http://www.i2b2.org/xsd/hive/msg/1.1/',
                            'pm', 'http://www.i2b2.org/xsd/cell/pm/1.1/',
                            'ont', 'http://www.i2b2.org/xsd/cell/ont/1.1/',
                            'psm', 'http://www.i2b2.org/xsd/cell/crc/psm/1.1/',
                            'xsi', 'http://www.w3.org/2001/XMLSchema-instance'
                    ))
    private static final long DEFAULT_WAIT_TIME_MILLISECONDS = 600 * 1000

    @Autowired
    GrailsApplication grailsApplication

    Long waitTimeMilliseconds

    private getInstanceConfig() {
        grailsApplication.config.org.transmartproject.i2b2.instance
    }

    private String getUser() {
        instanceConfig.user
    }

    private String getPassword() {
        instanceConfig.password
    }

    private String getDomain() {
        instanceConfig.domain
    }

    private String getProject() {
        instanceConfig.project
    }

    private Long getWaitTimeMilliseconds() {
        if (this.@waitTimeMilliseconds) {
            return this.@waitTimeMilliseconds
        }

        grailsApplication.config.org.transmartproject.i2b2.waitTimeMilliseconds ?:
                DEFAULT_WAIT_TIME_MILLISECONDS
    }

    I2b2Message create(Closure<Void> configureBody) {
        def message = new I2b2Message()
        message.xml.'i2b2:request'(
                NAMESPACE_CONTEXT.prefixToNamespaceUri.collectEntries { k, v ->
                    ["xmlns:$k".toString(), v]
                }) {
            message_header() {
                sending_application {
                    application_name 'tranSMART'
                    application_version '1.7' /* i2b2 checks this! */
                }
                sending_facility {
                    facility_name 'tranSMART'
                }
                security() {
                    domain domain
                    username user
                    password password
                }
                project_id project
            }
            request_header {
                result_waittime_ms waitTimeMilliseconds.toString()
            }
            message_body() {
                configureBody.setDelegate delegate
                configureBody.run()
            }
        }

        message
    }
}
