package org.transmartproject.i2b2.ontology

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.ontology.OntologyTerm

@Component
class I2b2OntologyTermFactory {

    @Autowired
    private I2b2ConceptsResource i2b2ConceptsResource

    OntologyTerm fromResponseNode(
            OntologyTerm qualified /* if applicable */,
            groovy.util.slurpersupport.NodeChild node) {
        if (node.applied_path.size() == 0) {
            new I2b2RegularOntologyTerm(
                    conceptsResource: i2b2ConceptsResource,
                    node: node)
        } else {
            new I2b2Modifier(
                    conceptsResource: i2b2ConceptsResource,
                    node: node,
                    qualifiedTerm: qualified)
        }
    }
}
