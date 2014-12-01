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

