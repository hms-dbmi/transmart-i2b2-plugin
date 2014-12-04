package org.transmartproject.i2b2.messages

import groovy.transform.Canonical

@Canonical
class I2b2Cell {
    I2b2CellType cellType
    String projectPath
    String url
}
