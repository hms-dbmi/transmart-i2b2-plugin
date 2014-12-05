import org.codehaus.groovy.grails.commons.spring.BeanConfiguration
import org.springframework.stereotype.Component
import org.transmartproject.i2b2.ontology.I2b2ConceptsResource
import org.transmartproject.i2b2.querytool.I2b2QueriesResource

class TransmartI2b2GrailsPlugin {
    def version = "1.0-SNAPSHOT"
    def grailsVersion = "2.3 > *"
    def pluginExcludes = []

    def title = "tranSMART i2b2 Plugin"
    def author = "Gustavo Lopes"
    def authorEmail = "gustavo@thehyve.nl"
    def description = '''\
Implements some tranSMART API calls by going to a specific i2b2 project through
the i2b2 XML APIs.\
'''

    def documentation = "https://github.com/transmart/transmart-i2b2"

    def license = "GPL3"

    def organization = [ name: "The Hyve", url: "http://thehyve.nl" ]

    def developers = []

    def issueManagement = [ system: "JIRA", url: "http://jira.transmartfoundation.org" ]

    def scm = [ url: "https://github.com/transmart/transmart-i2b2" ]

    def doWithSpring = {
        xmlns context:"http://www.springframework.org/schema/context"

        context.'component-scan'('base-package': 'org.transmartproject.i2b2') {
            context.'include-filter'(
                    type:       'annotation',
                    expression: Component.canonicalName)
        }

        /* override bean */
        conceptsResourceService(I2b2ConceptsResource)

        /* override bean */
        queriesResourceService(I2b2QueriesResource) { BeanConfiguration bc ->
            bc.beanDefinition.primary = true
        }
        coreDbQueriesResourceService('org.transmartproject.db.querytool.QueriesResourceService')

        def instanceConfig = application.config.org.transmartproject.i2b2.instance

        def handleDefault = { setting, defValue ->
            if (instanceConfig."$setting") {
                return
            }
            instanceConfig."$setting" = defValue
            log.info "Setting i2b2 instance $setting " +
                    "to default '${instanceConfig."$setting"}'"
        }

        handleDefault 'host',         'localhost'
        handleDefault 'port',         9090
        handleDefault 'context',      'i2b2'

        handleDefault 'user',         'i2b2'
        handleDefault 'password',     'demouser'
        handleDefault 'domain',       'i2b2demo'
        handleDefault 'project_path', '/'
        handleDefault 'project',      'Demo'

        /* I'm not very happy with having a separate project
         * setting; this could be inferred from the getServices
         * responses together with project_path */
    }
}
