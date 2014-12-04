package org.transmartproject.i2b2.ontology

import com.google.common.base.Function
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.BoundModifier
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.i2b2.messages.*

import java.util.concurrent.Future

@Log4j
class I2b2ConceptsResource implements ConceptsResource {

    @Autowired
    private I2b2Messager messager

    @Autowired
    private I2b2MessageFactory messageFactory

    @Autowired
    private I2b2OntologyTermFactory ontologyTermFactory

    @Override
    List<OntologyTerm> getAllCategories() {

        def message = messageFactory.create {
            'ont:get_categories'([
                    hiddens: 'true',
                    synonyms: 'true',
                    type: 'core',
                    blob: 'true'])
        }

        def res = doTermListQuery null, 'concepts',
                new I2b2MessageEnvelope(
                        cell: I2b2CellType.ONTOLOGY_MANAGEMENT,
                        service: 'getCategories',
                        message: message)
        res.get()
    }

    private Future<List<OntologyTerm>> doTermListQuery(
            OntologyTerm qualified,
            String entityName,
            I2b2MessageEnvelope message) {
        ListenableFuture<I2b2Response> response =
                messager.sendMessage(message)

        Futures.transform(response, { I2b2Response resp ->
            resp.throwIfNotDone()

            resp.body."$entityName".children().collect {
                ontologyTermFactory.fromResponseNode(qualified, it)
            }
        } as Function)
    }

    private I2b2MessageEnvelope getTermInfoEnvelope(String key) {
        def message = messageFactory.create {
            'ont:get_term_info' blob: 'true',
                    type: 'core',
                    synonyms: 'true',
                    hiddens: 'true', {
                self key
            }
        }

        new I2b2MessageEnvelope(
                message: message,
                service: 'getTermInfo',
                cell: I2b2CellType.ONTOLOGY_MANAGEMENT)
    }

    @Override
    OntologyTerm getByKey(String key) throws NoSuchResourceException {
        List<OntologyTerm> results = doTermListQuery(null,
                'concepts', getTermInfoEnvelope(key)).get()

        if (results.size() == 0) {
            throw new NoSuchResourceException("No concept with key $key found")
        }

        results[0]
    }

    /* TODO: needs to be added to the interface */
    BoundModifier getModifier(String modifierKey,
                              String appliedPath,
                              String qualifiedTermKey) throws NoSuchResourceException {
        def message1 = messageFactory.create {
            'ont:get_modifier_info' blob: 'true',
                    type: 'core',
                    synonyms: 'true',
                    hiddens: 'true', {
                self modifierKey
                applied_path appliedPath
            }
        }
        def envelope1 = new I2b2MessageEnvelope(
                message: message1,
                service: 'getModifierInfo',
                cell: I2b2CellType.ONTOLOGY_MANAGEMENT)
        Future<List<OntologyTerm>> futModifier =
                doTermListQuery(null, 'modifiers', envelope1)

        def envelope2 = getTermInfoEnvelope(qualifiedTermKey)
        Future<List<OntologyTerm>> futTermInfo =
                doTermListQuery(null, 'concepts', envelope2)

        def modifiers = futModifier.get()
        def termInfos = futTermInfo.get()

        if (!modifiers) {
            throw new NoSuchResourceException(
                    "No modifier with key $modifierKey " +
                            "and applied path $appliedPath")
        }
        if (!termInfos) {
            throw new NoSuchResourceException(
                    "No term with key $qualifiedTermKey")
        }

        ((I2b2Modifier) modifiers[0]).with {
            qualifiedTerm = termInfos[0]
            it
        }
    }

    Future<List<OntologyTerm>> fetchConceptChildren(I2b2RegularOntologyTerm ot,
                                                    boolean showHidden,
                                                    boolean showSynonyms) {

        def message = messageFactory.create {
            'ont:get_children' blob: 'true',
                    hiddens: (String) showHidden,
                    synonyms: (String) showSynonyms,
                    type: 'core', {
                parent ot.key
            }
        }

        doTermListQuery null, 'concepts', new I2b2MessageEnvelope(
                message: message,
                service: 'getChildren',
                cell: I2b2CellType.ONTOLOGY_MANAGEMENT)
    }

    Future<List<BoundModifier>> fetchConceptModifiers(I2b2RegularOntologyTerm ot,
                                                      boolean showHidden,
                                                      boolean showSynonyms) {
        def message = messageFactory.create {
            'ont:get_modifiers' blob: 'true',
                    hiddens: (String) showHidden,
                    synonyms: (String) showSynonyms,
                    type: 'core', {
                self ot.key
            }
        }

        doTermListQuery ot, 'modifiers', new I2b2MessageEnvelope(
                message: message,
                service: 'getModifiers',
                cell: I2b2CellType.ONTOLOGY_MANAGEMENT)
    }

    Future<List<BoundModifier>> fetchModifierChildren(I2b2Modifier modifier,
                                                      boolean showHidden,
                                                      boolean showSynonyms) {
        def message = messageFactory.create {
            'ont:get_modifier_children' blob: 'true',
                    hiddens: (String) showHidden,
                    synonyms: (String) showSynonyms, {
                parent          modifier.key
                applied_path    modifier.appliedPath
                applied_concept modifier.qualifiedTerm.key
            }
        }

        doTermListQuery modifier.qualifiedTerm,
                'modifiers',
                new I2b2MessageEnvelope(
                        message: message,
                        service: 'getModifierChildren',
                        cell: I2b2CellType.ONTOLOGY_MANAGEMENT)
    }
}
