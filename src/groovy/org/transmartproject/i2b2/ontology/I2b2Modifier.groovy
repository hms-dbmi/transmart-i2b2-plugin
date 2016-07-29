package org.transmartproject.i2b2.ontology

import groovy.transform.ToString
import org.transmartproject.core.ontology.BoundModifier
import org.transmartproject.core.ontology.OntologyTerm

@ToString(includes = ['key', 'appliedPath', 'qualifiedTerm'])
class I2b2Modifier extends AbstractI2b2OntologyTerm implements BoundModifier {

    OntologyTerm qualifiedTerm

    List<OntologyTerm> getChildren(boolean showHidden = false, boolean showSynonyms = false) {
        getAllDescendants(showHidden, showSynonyms, false)
    }

    List<OntologyTerm> getAllDescendants(boolean showHidden = false,
                                         boolean showSynonyms = false,
                                         boolean goDown = true) {
        if (goDown) {
            throw new UnsupportedOperationException(
                    'Cannot get all descendants on i2b2')
        }

        conceptsResource.fetchModifierChildren(
                this, showHidden, showSynonyms).get()
    }

    @Override
    String getAppliedPath() {
        node.applied_path
    }
}
