package org.transmartproject.i2b2.messages

import groovy.transform.Canonical

@Canonical
class I2b2MessageEnvelope {
    I2b2CellType cell
    String service
    I2b2Message message
}
