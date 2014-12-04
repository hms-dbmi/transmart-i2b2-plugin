package org.transmartproject.i2b2.messages

import com.google.common.base.Function
import com.google.common.collect.Maps
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
@Log4j
class I2b2CellLocator {

    @Autowired
    private I2b2Messager messager

    @Autowired
    private I2b2MessageFactory messageFactory

    @Autowired
    private GrailsApplication grailsApplication

    boolean findCellsOnStartup = true // useful to set to false for testing

    @PostConstruct
    void startupFindCells() {
        if (!findCellsOnStartup) {
            log.info('Skipping finding cells on startup')
            return
        }

        Futures.addCallback(findCellLocations(), new FutureCallback<Map>() {
            void onSuccess(Map map) {
                messager.registeredCells = map
            }

            void onFailure(Throwable throwable) {
                log.error "Error retrieving i2b2 cells", throwable
            }
        })
    }

    ListenableFuture<Map> findCellLocations() {

        def i2b2message = messageFactory.create {
            'pm:get_user_configuration'() {}
        }

        def envelope = new I2b2MessageEnvelope(
                cell: I2b2CellType.PRJOJECT_MANAGEMENT,
                message: i2b2message,
                service: 'getServices',)

        def messageFuture = messager.sendMessage(envelope)
        Futures.transform(messageFuture, this.&getCellLocations as Function)
    }

    private Map<I2b2CellType, String> getCellLocations(I2b2Response response) {
        Map<I2b2CellType, I2b2Cell> result = Maps.newHashMap()
        response.body.configure.cell_datas.children().each { cell_data ->
            def cellId = cell_data.'@id'.text()
            I2b2CellType cellType = I2b2CellType.values().find { it.id == cellId }
            if (!cellType && log.debugEnabled) {
                log.debug("Cell with id ${cellId} not recognized")
                return
            }

            def cellProjectPath = cell_data.project_path.text()
            /* cell project path must be a prefix of configured projectPath */
            if (!projectPath.startsWith(cellProjectPath)) {
                log.info("Cell $cellType with project path " +
                        "${cellProjectPath} ignored")
                return
            }

            I2b2Cell currentCell = new I2b2Cell(
                    cellType: cellType,
                    projectPath: cellProjectPath,
                    url: cell_data.url)
            if (result[cellType]) {
                I2b2Cell previousCell = result[cellType]

                if (cellProjectPath.startsWith(previousCell.projectPath)) {
                    /* this one we found is more specific */
                    log.info("Forgetting about cell $cellType with less specific " +
                            "project path $previousCell, adding one with " +
                            "path $cellProjectPath")
                    result[cellType] = currentCell
                } else { /* the one we found is less specific */
                    log.info("Ignoring cell with project path $cellProjectPath " +
                            "in favor of the one with ${result[cellType]}")
                }
            } else {
                result[cellType] = currentCell
            }
        }

        result
    }

    private getInstanceConfig() {
        grailsApplication.config.org.transmartproject.i2b2.instance
    }

    private String getProjectPath() {
        instanceConfig.project_path
    }
}
