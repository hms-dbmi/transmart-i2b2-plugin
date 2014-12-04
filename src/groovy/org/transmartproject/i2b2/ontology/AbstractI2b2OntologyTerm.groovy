package org.transmartproject.i2b2.ontology

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study

import java.util.regex.Pattern

abstract class AbstractI2b2OntologyTerm implements OntologyTerm {
    I2b2ConceptsResource conceptsResource
    groovy.util.slurpersupport.NodeChild node

    @Override
    Integer getLevel() {
        node.level.text() as Integer
    }

    @Override
    String getKey() {
        node.key
    }

    static Pattern KEY_TO_FULLNAME_PATTERN = ~'^\\\\\\\\[^\\\\]+'

    @Override
    String getFullName() {
         node.key.text() - KEY_TO_FULLNAME_PATTERN
    }

    @Override
    Study getStudy() {
        /* study is a transmart-ism */
        /* better to implement this in StudiesResource, delegate to that method
         * and deprecate OntologyTerm::getStudy() */
        null
    }

    @Override
    String getName() {
        node.name
    }

    @Override
    String getCode() {
        node.basecode
    }

    @Override
    String getTooltip() {
        node.tooltip
    }

    @Override
    EnumSet<OntologyTerm.VisualAttributes> getVisualAttributes() {
        OntologyTerm.VisualAttributes.forSequence(node.visualattributes.text())
    }

    @Override
    List<Patient> getPatients() {
        /* only used by the restful api */
        /* probably also best to implement in PatientsResource and
         * deprecate OntologyTerm::getPatients() */
        throw new UnsupportedOperationException(
                'getPatients() not yet supported on i2b2 backed terms')
    }

    @Override
    Object getMetadata() {
        /* TODO: share with core-db implementation */
        def valueMetadata = node.metadataxml.ValueMetadata
        if (!valueMetadata) {
            return null
        }

        def ret = [:]

        ret.okToUseValues = valueMetadata.Oktousevalues == 'Y' ? true : false
        ret.dataType = valueMetadata.DataType.text() ?: null
        ret
    }

    String getDimensionCode() {
        node.dimcode
    }

    String getDimensionTableName() {
        node.tablename
    }
}
