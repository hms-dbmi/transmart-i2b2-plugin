package org.transmartproject.i2b2.matchers

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.transmartproject.i2b2.messages.I2b2MessageFactory
import org.w3c.dom.Document
import org.xml.sax.InputSource

import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

class I2b2XPathMatcher extends BaseMatcher<String> {
    String xpathExpression
    QName type = XPathConstants.STRING
    Matcher<?> valueMatcher

    public static I2b2XPathMatcher hasXPath(String xpath,
                                            QName type = XPathConstants.STRING,
                                            Matcher<?> valueMatcher = null) {
        new I2b2XPathMatcher(
                xpathExpression: xpath, type: type, valueMatcher: valueMatcher)
    }

    @Override
    boolean matches(Object item) {
        if (!(item instanceof String)) {
            return false
        }

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance()
        DocumentBuilder builder = factory.newDocumentBuilder()
        Document doc = builder.parse(new InputSource(
                new StringReader(item)))
        XPathFactory xPathFactory = XPathFactory.newInstance()
        XPath xpath = xPathFactory.newXPath()
        xpath.setNamespaceContext(I2b2MessageFactory.NAMESPACE_CONTEXT)
        XPathExpression expr = xpath.compile(xpathExpression)

        def result = expr.evaluate(doc, type)
        if (valueMatcher) {
            valueMatcher.matches(result)
        } else {
            result.respondsTo('size') ?
                    result.size() > 0 :
                    !!result
        }
    }

    @Override
    void describeTo(Description description) {
        description.appendText("an string for an XML document with XPath " +
                xpathExpression + "returning type $type")
        if (valueMatcher != null) {
            description.appendText(" matching ")
            valueMatcher.describeTo(description)
        }
    }
}
