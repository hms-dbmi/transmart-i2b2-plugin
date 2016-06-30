def defaultVMSettings = [
        maxMemory: 768,
        minMemory: 64,
        debug:     false,
        maxPerm:   256
]

grails.project.fork = [
        test:    [*: defaultVMSettings, daemon:      true],
        run:     [*: defaultVMSettings, forkReserve: false],
        war:     [*: defaultVMSettings, forkReserve: false],
        console: defaultVMSettings
]

def dm, dmClass
try {
    dmClass = new GroovyClassLoader().parseClass(
            new File('../transmart-dev/DependencyManagement.groovy'))
} catch (Exception e) { }
if (dmClass) {
    dm = dmClass.newInstance()
}

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
    inherits('global')
    log 'warn'
    repositories {
        grailsCentral()
        mavenCentral()
        mavenLocal()
        mavenRepo 'https://repo.transmartfoundation.org/content/repositories/public/'
    }

    dependencies {
        compile 'org.transmartproject:transmart-core-api:1.2.2-hackathon-SNAPSHOT'
        compile 'org.apache.httpcomponents:httpclient:4.3.3'
        compile 'org.apache.httpcomponents:httpasyncclient:4.0.2'
        compile 'com.google.guava:guava:18.0'

        test 'org.hamcrest:hamcrest-library:1.3',
                'org.hamcrest:hamcrest-core:1.3'
        test 'junit:junit:4.11', {
            transitive = false /* don't bring hamcrest */
            export     = false
        }
        test 'org.gmock:gmock:0.9.0-r435-hyve2', {
            transitive = false /* don't bring groovy-all */
            export     = false
        }
    }

    plugins {
        build ':release:3.0.1', ':rest-client-builder:1.0.3', {
            export = false
        }

        if (!dm) {
            runtime ':transmart-core:1.2.2-hackathon-SNAPSHOT'
        } else {
            dm.internalDependencies delegate
        }
    }
}

dm?.with {
    configureInternalPlugin 'runtime', 'transmart-core'
}
