package org.transmartproject.i2b2.ontology

import groovy.util.slurpersupport.NodeChild
import org.gmock.WithGMock
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.transmartproject.core.ontology.BoundModifier
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.i2b2.matchers.I2b2XPathMatcher
import org.transmartproject.i2b2.mock.MockI2b2

import javax.xml.xpath.XPathConstants

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@WithGMock
class I2b2ModifierTests {

    @Autowired
    MockI2b2 mockI2b2

    @Autowired
    I2b2ConceptsResource conceptsResource

    def requestMock

    @Before
    void setUp() {
        requestMock = mock()
        mockI2b2.reset(requestMock)
    }

    @Test
    void testLoadingModifier() {
        def modifierKey = '\\\\PCORI_DIAG\\PCORI_MOD\\PDX\\S\\'
        def appliedPath = '\\PCORI\\DIAGNOSIS\\%'
        def qualifiedTermKey = '\\\\PCORI_DIAG\\PCORI\\DIAGNOSIS\\09\\(001-999.99) ' +
                'D~qlur\\(760-779.99) C~nh4g\\(760-763.99) M~atny\\(762) Fetus ' +
                'or~9lo0\\(762.7) Chorio~tjj0\\'

        requestMock.withRequest(
                endsWith('/getModifierInfo'),
                allOf(
                        I2b2XPathMatcher.hasXPath(
                                '//self', XPathConstants.STRING,
                                is(modifierKey)),
                        I2b2XPathMatcher.hasXPath(
                                '//applied_path', XPathConstants.STRING,
                                is(appliedPath)),
                ))
        .returns(
                new ClassPathResource(
                        'resources/ONT_getModifierInfo_resp.xml')
                        .file.getText('UTF-8'))

        requestMock.withRequest(
                endsWith('/getTermInfo'),
                I2b2XPathMatcher.hasXPath(
                                '//self', XPathConstants.STRING,
                                is(qualifiedTermKey)))
                .returns(
                new ClassPathResource(
                        'resources/ONT_getTermInfo_resp.xml')
                        .file.getText('UTF-8'))


        BoundModifier modifier
        play {
            try {
                modifier = conceptsResource.getModifier(
                        modifierKey,
                        appliedPath,
                        qualifiedTermKey)
            } finally {
                mockI2b2.maybeThrowLastError()
            }
        }

        assertThat modifier, allOf(
                hasProperty('key', equalTo(modifierKey)),
                hasProperty('appliedPath', equalTo(appliedPath)),
                hasProperty('qualifiedTerm', allOf(
                        hasProperty('name', equalTo('Chorioamnionitis affecting fetus or newborn')),
                        hasProperty('key', equalTo(qualifiedTermKey)))))
    }

    @Test
    void testGetModifierChildren() {
        def qualifiedTerm = mock(OntologyTerm)
        def qualifiedTermKey = '\\\\PCORI_DIAG\\PCORI\\DIAGNOSIS\\09\\'
        qualifiedTerm.key.returns(qualifiedTermKey)

        NodeChild nodeChild = mock(NodeChild)

        def parentModifierKey = '\\\\PCORI_DIAG\\PCORI_MOD\\PDX\\'
        nodeChild.key.returns(parentModifierKey)
        def modifierAppliedPath = '\\PCORI\\DIAGNOSIS\\%'
        nodeChild.applied_path.returns(modifierAppliedPath)

        I2b2Modifier boundModifier = new I2b2Modifier(
                node: nodeChild,
                conceptsResource: conceptsResource,
                qualifiedTerm: qualifiedTerm,)

        requestMock.withRequest(
                endsWith('/getModifierChildren'),
                allOf(
                        I2b2XPathMatcher.hasXPath(
                                '//parent', XPathConstants.STRING,
                                is(parentModifierKey)),
                        I2b2XPathMatcher.hasXPath(
                                '//applied_path', XPathConstants.STRING,
                                is(modifierAppliedPath)),
                        I2b2XPathMatcher.hasXPath(
                                '//applied_concept', XPathConstants.STRING,
                                is(qualifiedTermKey))))
                .returns(
                new ClassPathResource(
                        'resources/ONT_getChildModifiers_resp.xml')
                        .file.getText('UTF-8'))

        def result
        play {
            try {
                result = boundModifier.getChildren()
            } finally {
                mockI2b2.maybeThrowLastError()
            }
        }

        assertThat result, allOf(
                hasSize(6),
                everyItem(isA(BoundModifier)),
                hasItem(allOf(
                        hasProperty('level', is(2)),
                        hasProperty('appliedPath', is('\\PCORI\\DIAGNOSIS\\%')),
                        hasProperty('key', is('\\\\PCORI_DIAG\\PCORI_MOD\\PDX\\UN\\')),
                ))
        )
    }
}
