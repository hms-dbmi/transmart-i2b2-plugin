import com.google.common.collect.ImmutableMap
import org.transmartproject.i2b2.messages.I2b2Cell
import org.transmartproject.i2b2.messages.I2b2CellLocator
import org.transmartproject.i2b2.messages.I2b2CellType
import org.transmartproject.i2b2.messages.I2b2Messager
import org.transmartproject.i2b2.mock.MockI2b2

// Note this file is only used for testing.
// Spring context configuration is otherwise done in the plugin descriptor.

beans = {
    mockI2b2(MockI2b2)
    i2b2CellLocator(I2b2CellLocator) {
        findCellsOnStartup = false
    }
    i2b2Messager(I2b2Messager) {
        registeredCells = ImmutableMap.of(
                I2b2CellType.ONTOLOGY_MANAGEMENT,
                new I2b2Cell(
                        cellType: I2b2CellType.ONTOLOGY_MANAGEMENT,
                        url: 'http://localhost:9090/i2b2/services/OntologyService/',
                        projectPath: '/',
                ),
                I2b2CellType.DATA_REPOSITORY,
                new I2b2Cell(
                        cellType: I2b2CellType.DATA_REPOSITORY,
                        url: 'http://localhost:9090/i2b2/services/QueryToolService/',
                        projectPath: '/',

                ),
        )
    }
    queryDefinitionXmlService('org.transmartproject.db.querytool.QueryDefinitionXmlService')
}
