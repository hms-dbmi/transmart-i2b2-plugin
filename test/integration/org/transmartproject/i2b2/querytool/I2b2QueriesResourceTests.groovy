package org.transmartproject.i2b2.querytool

import org.gmock.WithGMock
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.transmartproject.core.querytool.*
import org.transmartproject.i2b2.matchers.I2b2XPathMatcher
import org.transmartproject.i2b2.mock.MockI2b2

import javax.xml.xpath.XPathConstants

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@WithGMock
class I2b2QueriesResourceTests {

    @Autowired
    QueriesResource i2b2QueriesResource

    @Autowired
    MockI2b2 mockI2b2

    def requestMock

    @Before
    void setUp() {
        requestMock = mock()
        mockI2b2.reset(requestMock)
    }

    @Test
    void testRunQuery() {
        def conceptKey = '\\\\PCORI_DIAG\\PCORI\\DIAGNOSIS\\09\\(001-999.99) ' +
                'D~qlur\\(140-239.99) N~6vc5\\'
        def user = 'admin'

        Item item = new Item(conceptKey: conceptKey)
        Panel p = new Panel(items: [item])

        QueryDefinition queryDefinition = new QueryDefinition([p])

        requestMock.withRequest(
                endsWith('/QueryToolService/request'),
                allOf(
                        I2b2XPathMatcher.hasXPath(
                                '//item_key', XPathConstants.STRING,
                                is(conceptKey))))
                .returns(
                new ClassPathResource(
                        'resources/CRC_loadQueryDefinition_resp.xml')
                        .file.getText('UTF-8'))

        QueryResult result
        play {
            try {
                result = i2b2QueriesResource.runQuery(
                        queryDefinition, user)
            } finally {
                mockI2b2.maybeThrowLastError()
            }
        }

        assertThat result, allOf(
                hasProperty('id', is(675L)),
                hasProperty('setSize', is(65L)),
                hasProperty('status', is(QueryStatus.FINISHED)),
                hasProperty('username', is(user)))
    }
}
