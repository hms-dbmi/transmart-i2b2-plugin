package org.transmartproject.i2b2.messages

import com.google.common.util.concurrent.ListenableFuture
import org.gmock.WithGMock
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.transmartproject.i2b2.matchers.I2b2XPathMatcher
import org.transmartproject.i2b2.mock.MockI2b2

import javax.xml.xpath.XPathConstants

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@WithGMock
class I2b2CellLocatorTests {

    @Autowired
    I2b2CellLocator locator

    @Autowired
    MockI2b2 mockI2b2

    @Test
    void testFindCellLocations() {
        def mock = mock()
        mockI2b2.reset(mock)

        mock.withRequest(endsWith('/getServices'),
                I2b2XPathMatcher.hasXPath(
                        '/request/message_body/get_user_configuration',
                        XPathConstants.NODE,
                        any(Object))
        ).returns(
                new ClassPathResource(
                        'resources/PM_getServices_resp.xml')
                        .file.getText('UTF-8'))

        play {
            ListenableFuture<Map> locations = locator.findCellLocations()
            Map result
            try {
                result = locations.get()
            } finally {
                // will hide any earlier exception in .get(); that's ok
                mockI2b2.maybeThrowLastError()
            }

            def omUrl = 'http://testProject:9090/i2b2/services/OntologyService/'
            assertThat result, allOf(
                    hasEntry(equalTo(I2b2CellType.ONTOLOGY_MANAGEMENT),
                            allOf(
                                    hasProperty('projectPath', equalTo('/testProject/')),
                                    hasProperty('url', equalTo(omUrl)),
                                    hasProperty('cellType', equalTo(I2b2CellType.ONTOLOGY_MANAGEMENT)),
                            )),
                    hasEntry(equalTo(I2b2CellType.DATA_REPOSITORY), any(I2b2Cell)),
            )
        }
    }

}
