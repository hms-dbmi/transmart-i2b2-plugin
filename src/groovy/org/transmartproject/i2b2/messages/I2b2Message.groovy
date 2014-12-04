package org.transmartproject.i2b2.messages

import groovy.xml.MarkupBuilder

class I2b2Message {
    private StringWriter sw = new StringWriter()

    MarkupBuilder xml = new MarkupBuilder(sw)

    String toString() {
        sw.toString()
    }
}
