package org.transmartproject.i2b2.querytool

import com.google.common.base.Function
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import groovy.util.slurpersupport.NodeChild
import org.apache.http.HttpEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryDefinitionXmlConverter
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.i2b2.messages.*
import org.transmartproject.i2b2.ontology.I2b2RegularOntologyTerm

import java.util.regex.Matcher

@Component
class I2b2QueriesResource implements QueriesResource {

    @Autowired
    private I2b2MessageFactory messageFactory

    @Autowired
    private I2b2Messager messager

    @Autowired
    private QueryDefinitionXmlConverter queryDefinitionXmlConverter

    private static String QUERY_PLACEHOLDER = '===== PLACEHOLDER ====='

    @Override
    QueryResult runQuery(QueryDefinition definition, String username = null)
            throws InvalidRequestException {
        if (!username) {
            throw new UnsupportedOperationException("Running a query without " +
                    "specifying the tranSMART user is not supported anymore")
        }

        QueryDefinition modifiedDefinition =
                introduceTransmartUser(definition, username)

        String definitionText =
                queryDefinitionXmlConverter.toXml(modifiedDefinition)

        def protoMessage = messageFactory.create {
            'psm:psmheader' {
                request_type 'CRC_QRY_runQueryInstance_fromQueryDefinition'
            }
            'psm:request' 'xsi:type': 'psm:query_definition_requestType', {
                mkp.yieldUnescaped definitionText
                result_output_list {
                    result_output name: 'patientset'
                }
            }
        }

        def message = new I2b2Message() {
            String toString() {
                protoMessage.toString().replace(
                        QUERY_PLACEHOLDER, definitionText)
            }
        }

        ListenableFuture<I2b2Response> response =
                messager.sendMessage(envelope(message))

        I2b2Response resp = response.get()
        checkCRCResponse(resp)

        def r = resp.body.response.query_result_instance
        if (r.size() == 0) {
            throw new NoSuchResourceException(
                    "Could not find query result instance inside response")
        }

        new I2b2QueryResultInstance(
                username: username,
                node: r[0])
    }

    private QueryDefinition introduceTransmartUser(
            QueryDefinition queryDefinition,
            String username) {
        String name = queryDefinition.name
        name += " tmuser $username"

        new QueryDefinition(name, queryDefinition.panels)
    }

    @Override
    QueryResult getQueryResultFromId(Long id) throws NoSuchResourceException {
        //getQueryResultInstance_fromResultInstanceId

        ListenableFuture<I2b2QueryResultInstance> futResultInstance =
                getIncompleteResultInstance(id)
        ListenableFuture<NodeChild> futQueryMasterNode =
                queryMasterNodeFromResultInstanceId(id)

        def result = futResultInstance.get()
        def queryMaster = futQueryMasterNode.get()

        result.username = extractTransmartUser(queryMaster.name)

        result
    }

    @Override
    QueryDefinition getQueryDefinitionForResult(QueryResult result)
            throws NoSuchResourceException {
        ListenableFuture<NodeChild> queryMasterNode =
                queryMasterNodeFromResultInstanceId(result.id)

        def requestXmlText = queryMasterNode.get().request_xml.text()
        if (!requestXmlText) {
            throw new UnexpectedResultException(
                    "Could not find request_xml content")
        }

        queryDefinitionXmlConverter.fromXml(new StringReader(requestXmlText))
    }

    private ListenableFuture<NodeChild> queryMasterNodeFromResultInstanceId(Long id) {
        def message = messageFactory.create {
            'psm:psmheader' {
                request_type 'CRC_QRY_getQueryMaster_fromResultInstanceId'
            }
            'psm:request' 'xsi:type': 'psm:instance_requestType', {
                query_instance_id id.toString()
            }
        }
        ListenableFuture<I2b2Response> resp =
                messager.sendMessage(envelope(message))

        Futures.transform(resp, { I2b2Response i2b2response ->
            checkCRCResponse i2b2response

            def r = i2b2response.body.response.query_master
            if (r.size() == 0) {
                throw new NoSuchResourceException("Could not find query master " +
                        "for query result instane with id $id")
            }
            r[0]
        } as Function<I2b2Response, NodeChild>)
    }

    private ListenableFuture<I2b2QueryResultInstance> getIncompleteResultInstance(Long id) {
        def message = messageFactory.create {
            'psm:psmheader' {
                request_type 'CRC_QRY_getQueryResultInstance_fromResultInstanceId'
            }
            'psm:request' 'xsi:type': 'psm:instance_requestType', {
                query_instance_id
            }
        }
        ListenableFuture<I2b2Response> resp =
                messager.sendMessage(envelope(message))

        // doesn't include user
        Futures.transform(resp, { I2b2Response i2b2resp ->
            checkCRCResponse i2b2resp

            def r = i2b2resp.body.response.query_result_instance
            if (r.size() == 0) {
                throw new NoSuchResourceException(
                        "Could not find query result instance with id $id")
            }

            new I2b2QueryResultInstance(node: r[0])
        } as Function)
    }

    private static I2b2MessageEnvelope envelope(I2b2Message message) {
        new I2b2MessageEnvelope(
                message: message,
                service: 'request',
                cell: I2b2CellType.DATA_REPOSITORY)
    }

    private static QUERY_MASTER_NAME_PATTERN = ~'(.+?) tmuser (.+?)'

    private static String extractTransmartUser(String queryMasterName) {
        Matcher m = QUERY_MASTER_NAME_PATTERN.matcher(queryMasterName)
        if (!m.matches()) {
            return null
        }

        m.group(2)
    }

    private static checkCRCResponse(I2b2Response response) {
        response.throwIfNotDone()

        String status = response.body.response.status.condition.'@type'.text()
        if (status != 'DONE') {
            throw new UnexpectedResultException(
                    "Expected PM result to be DONE, but it was $status")
        }
    }
}
