package org.transmartproject.i2b2.messages

enum I2b2CellType {
    PRJOJECT_MANAGEMENT('PM', 'PMService'),
    ONTOLOGY_MANAGEMENT('ONT', 'OntologyService'),
    DATA_REPOSITORY('CRC', 'QueryToolService'),

    final String id
    final String serviceName

    I2b2CellType(String id, String serviceName) {
        this.id = id
        this.serviceName = serviceName
    }
}
