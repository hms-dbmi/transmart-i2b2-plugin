package org.transmartproject.i2b2.ontology

import org.transmartproject.core.ontology.BoundModifier
import org.transmartproject.core.ontology.OntologyTerm

class I2b2Modifier extends AbstractI2b2OntologyTerm implements BoundModifier {

    I2b2RegularOntologyTerm qualifiedTerm

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

        conceptsResource.fetchModifierChildren(
                this, showHidden, showSynonyms).get()
    }

    @Override
    String getAppliedPath() {
        node.applied_path
    }
}
