import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useRef, useState} from "react";
import {NodeDeploymentModalDataContext} from "../../contexts/NodeDeploymentModalContext";
import {Button, Form} from "react-bootstrap";
import {SubmissionErrorModalContext} from "../../contexts/SubmissionErrorModalContext";
import {Deployment, DEPLOYMENT_STATE, DeploymentUpdate} from "../../utilities/ApiDeclarations";
import {createMap, identity} from "../../utilities/Utils";

/**
 * This component is used to add a node to the hypothetical network
 *
 * @constructor
 */
export const NodeDeploymentAdditionForm: FunctionComponent = () => {
    // deference needed values from node deployment modal data
    const {deployment, additions, deletions, servers, updateAdditions} = useContext(NodeDeploymentModalDataContext);

    // submission error modal context
    const submissionErrorModal = useContext(SubmissionErrorModalContext);

    // reference to clear form on submission
    const messageForm = useRef<HTMLFormElement>(null);

    // node id set to represent all node ids in the hypothetical deployment
    const [nodeIdSet, updateNodeIdSet] = useState<Set<number>>(new Set<number>());

    // server uuid set to represent all server uuids used in the hypothetical deployment
    const [serverUUIDSet, updateServerUUIDSet] = useState<Set<string>>(new Set<string>());

    // store the form in the state as there may be no reason to render the form
    const [form, updateForm] = useState<ReactElement | null>(null);

    /**
     * Everytime deployment or additions is updated, update the internal nodeIdSet and serverUUIDSet to match
     * those arrays
     */
    useEffect(() => {
        if (deployment && additions) {
            const serverUUIDLIst = deployment.map(i => i.server_uuid).concat(additions.map(i => i.serverUUID));

            updateServerUUIDSet(new Set<string>(serverUUIDLIst));

            const nodeIdList = deployment.map(i => i.new_node_id).concat(additions.map(i => i.newNodeId));

            updateNodeIdSet(new Set<number>(nodeIdList));
        }
    }, [deployment, additions, updateServerUUIDSet, updateNodeIdSet]);

    /**
     * This callback is used to parse the form data and to update the additions list with that data
     */
    const onFormSubmit = useCallback((event: any): void => {
        event.preventDefault();

        if (submissionErrorModal.show && additions && deletions && updateAdditions && deployment) {

            const parentId = parseInt(event.target.elements.parentId.value);

            const nodeId = parseInt(event.target.elements.nodeId.value);

            if ((!nodeIdSet.has(parentId) || nodeIdSet.has(nodeId))) {
                submissionErrorModal.show("You must entered a valid node id as the parent id and a unique node id as the new node id");
                return;
            }

            // If the parent node is in the process of removing cancel the submission
            const deploymentMap: Map<number, Deployment>
                = createMap<number, Deployment>(v => v.new_node_id, identity, deployment);
            switch (deploymentMap.get(parentId)?.process_status) {
                case DEPLOYMENT_STATE[DEPLOYMENT_STATE.PROCESSING_REMOVING]:
                case DEPLOYMENT_STATE[DEPLOYMENT_STATE.REMOVING]:
                case DEPLOYMENT_STATE[DEPLOYMENT_STATE.WAITING_REMOVAL]:
                    submissionErrorModal.show("You cannot deploy a new node as a child to a node that is currently in the process of being removed");
                    return;
            }

            // if the node is queued for removal cancel the submission
            const deletionsMap: Map<number, DeploymentUpdate> =
                createMap<number, DeploymentUpdate>(v => v.newNodeId, identity, deletions);
            if (deletionsMap.has(parentId)) {
                submissionErrorModal.show("You cannot deploy a new node as a child to a node that is planned for deletions");
                return;
            }

            const serverName = event.target.elements.serverName.value;

            let serverUUID = null;

            if (servers) {
                for (let server of servers)
                    if (server.name === serverName)
                        serverUUID = server.server_uuid;
            }

            if (serverUUID === null) {
                submissionErrorModal.show("Unable to find the serverUUID from servername");
                return;
            }

            updateAdditions(additions.concat({
                parentId: parentId,
                newNodeId: nodeId,
                serverUUID: serverUUID
            }));

            messageForm.current?.reset();
        }
    }, [deployment, additions, deletions, servers, updateAdditions, nodeIdSet, messageForm, submissionErrorModal]);

    /**
     * Everytime servers updates, generate a new form based on the information given and what the number
     * of free servers are. If there are no free servers inform the user, if there are render the form.
     * If the servers set is undefined render loading... to signify the api call is in progress
     */
    useEffect(() => {

        let value: ReactElement;

        let freeServers = [];

        if (servers) {
            for (let [index, server] of servers.entries())
                if (!serverUUIDSet.has(server.server_uuid))
                    freeServers.push(
                        <option key={index}>{server.name}</option>
                    );
            if (freeServers.length > 0) {
                value = (
                    <Form onSubmit={onFormSubmit} ref={messageForm}>
                        <Form.Group controlId="parentId">
                            <Form.Label>Parent Node Id</Form.Label>
                            <Form.Control type="text" placeholder="Parent Id"/>
                            <Form.Text className="text-muted">
                                Must be an integer
                            </Form.Text>
                        </Form.Group>
                        <Form.Group controlId="nodeId">
                            <Form.Label>New Node Id</Form.Label>
                            <Form.Control type="text" placeholder="New Node Id"/>
                            <Form.Text className="text-muted">
                                Must be an integer
                            </Form.Text>
                        </Form.Group>
                        <Form.Group controlId="serverName">
                            <Form.Control as="select">
                                {freeServers}
                            </Form.Control>
                        </Form.Group>
                        <Button variant="primary" type="submit">
                            Submit
                        </Button>
                    </Form>
                );
            } else {
                value = (
                    <p>There are no free servers available, you need to add a server to add a node to the network</p>
                );
            }
        } else {
            value = (
                <p>Loading...</p>
            );
        }
        updateForm(value);
    }, [servers, updateForm, serverUUIDSet, onFormSubmit]);

    return (
        <>
            {form}
        </>
    )
};