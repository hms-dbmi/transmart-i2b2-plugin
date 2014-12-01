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
}
