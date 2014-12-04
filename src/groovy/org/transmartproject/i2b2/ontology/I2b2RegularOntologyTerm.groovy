package org.transmartproject.i2b2.ontology

import groovy.util.logging.Log4j
import org.transmartproject.core.ontology.OntologyTerm

@Log4j
class I2b2RegularOntologyTerm extends AbstractI2b2OntologyTerm {

    @Override
    List<OntologyTerm> getChildren(boolean showHidden = false, boolean showSynonyms = false) {
        getAllDescendants(showHidden, showSynonyms, false)
    }

    @Override
    List<OntologyTerm> getAllDescendants(boolean showHidden = false,
                                         boolean showSynonyms = false,
                                         boolean goDown = true) {
        if (goDown) {
            throw new UnsupportedOperationException(
                    'Cannot get all descendants on i2b2')
        }

        def modifiersFut = conceptsResource.fetchConceptModifiers(
                this, showHidden, showSynonyms)
        def conceptsFut = conceptsResource.fetchConceptChildren(
                this, showHidden, showSynonyms)

        modifiersFut.get() + conceptsFut.get()
    }
}
