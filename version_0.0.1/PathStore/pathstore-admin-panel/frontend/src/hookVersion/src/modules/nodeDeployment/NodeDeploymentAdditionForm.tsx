import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useRef, useState} from "react";
import {NodeDeploymentModalData} from "../../contexts/NodeDeploymentModalContext";
import {Button, Form} from "react-bootstrap";
import {SubmissionErrorModalContext} from "../../contexts/SubmissionErrorModalContext";

/**
 * This component is used to add a node to the hypothetical network
 *
 * @constructor
 */
export const NodeDeploymentAdditionForm: FunctionComponent = () => {
    // deference needed values from node deployment modal data
    const {deployment, additions, servers, updateAdditions} = useContext(NodeDeploymentModalData);

    // submission error modal context
    const submissionErrorModal = useContext(SubmissionErrorModalContext);

    // reference to clear form on submission
    const messageForm = useRef<HTMLFormElement>(null);

    // node id set to represent all node ids in the hypothetical deployment
    const [nodeIdSet, updateNodeIdSet] = useState<Set<number>>(new Set<number>());

    // server uuid set to represent all server uuids used in the hypothetical deployment
    const [serverUUIDSet, updateServerUUIDSet] = useState<Set<string>>(new Set<string>());

    // state to store the servers to show in the table
    const [freeServers, updateFreeServers] = useState<ReactElement[]>([]);

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
    }, [deployment, additions]);

    /**
     * Everytime servers or update free servers changes update the servers to display on the page
     */
    useEffect(() => {
        let temp = [];

        if (servers) {
            for (let i = 0; i < servers.length; i++)
                if (!serverUUIDSet.has(servers[i].server_uuid))
                    temp.push(
                        <option key={i}>{servers[i].name}</option>
                    );
            updateFreeServers(temp);
        }
    }, [servers, updateFreeServers, serverUUIDSet]);

    /**
     * This callback is used to parse the form data and to update the additions list with that data
     */
    const onFormSubmit = useCallback((event: any): void => {
        event.preventDefault();

        if (submissionErrorModal.show && additions && updateAdditions) {

            const parentId = parseInt(event.target.elements.parentId.value);

            const nodeId = parseInt(event.target.elements.nodeId.value);

            if ((!nodeIdSet.has(parentId) || nodeIdSet.has(nodeId))) {
                submissionErrorModal.show("You must entered a valid node id as the parent id and a unique node id as the new node id");
                return;
            }

            const serverName = event.target.elements.serverName.value;

            let serverUUID = null;

            if (servers) {
                for (let i = 0; i < servers.length; i++)
                    if (servers[i].name === serverName)
                        serverUUID = servers[i].server_uuid;
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
    }, [additions, servers, updateAdditions, nodeIdSet, messageForm, submissionErrorModal]);

    return freeServers.length > 0 ?
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
        : <p>There are no free servers available, you need to add a server to add a node to the network</p>;
};