package org.transmartproject.i2b2.querytool

import grails.util.Holders
import groovy.util.slurpersupport.NodeChild
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryStatus
import sun.reflect.generics.reflectiveObjects.NotImplementedException

class I2b2QueryResultInstance implements QueryResult {
    NodeChild node
    String username

    @Override
    Long getId() {
        node.result_instance_id.text() as Long
    }

    @Override
    Long getSetSize() {
        node.set_size.text() as Long
    }

    @Override
    QueryStatus getStatus() {
        QueryStatus.forId(
                node.query_status_type.status_type_id.text() as int)
    }

    @Override
    String getErrorMessage() {
        node.message ?: null
    }

    @Override
    Set<Patient> getPatients() {
        /* TODO: we really need to break core-api into a api/spi...
         * This is a hack */
        QueriesResource qr =
                Holders.applicationContext.getBean('coreDbQueriesResourceService')
        qr.getQueryResultFromId(this.id).patients
    }
}
