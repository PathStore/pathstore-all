import {ApplicationStatus, Deployment, Server} from "../utilities/ApiDeclarations";
import Modal from "react-modal";
import Table from "react-bootstrap/Table";
import Button from "react-bootstrap/Button";
import React, {Component} from "react";
import {formatServer, webHandler} from "../utilities/Utils";

/**
 * Properties definition for {@link NodeInfoModal}
 */
interface NodeInfoModalProperties {
    /**
     * Node id of node to show info of
     */
    readonly node: number

    /**
     * Whether to display the modal or not
     */
    readonly show: boolean

    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * List of node application status from api
     */
    readonly applicationStatus: ApplicationStatus[]

    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * Callback function to close modal on completion
     */
    readonly callback: () => void
}

/**
 * This component is used when a user clicks on a node in a pathstore topology to give the user some information
 * about the node.
 */
export default class NodeInfoModal extends Component<NodeInfoModalProperties> {

    /**
     * Formats the data for the application status table which informs the user about the current status of
     * application installation on this particular node
     *
     * @param applicationStatus
     * @returns {[]}
     */
    formatApplicationStatusTable = (applicationStatus: ApplicationStatus[]) => {

        let response = [];

        response.push(
            <thead key={0}>
            <tr>
                <th>Nodeid</th>
                <th>Application</th>
                <th>Status</th>
                <th>Waiting</th>
                <th>Job UUID</th>
            </tr>
            </thead>
        );

        let body = [];

        for (let i = 0; i < applicationStatus.length; i++) {

            let currentObject = applicationStatus[i];

            body.push(
                <tr>
                    <td>{currentObject.nodeid}</td>
                    <td>{currentObject.keyspace_name}</td>
                    <td>{currentObject.process_status}</td>
                    <td>{currentObject.wait_for}</td>
                    <td>{currentObject.process_uuid}</td>
                </tr>)
        }

        response.push(
            <tbody key={1}>
            {body}
            </tbody>
        );

        return response;
    };

    /**
     * Returns a button or null iff the node is eligible for re-trying deployment (the node has failed deployment)
     *
     * @param deployment
     * @returns {null|*}
     */
    retryButton = (deployment: Deployment[]) => {
        const deployObject = deployment.filter(i => i.new_node_id === this.props.node);

        if (deployObject[0].process_status === "FAILED")
            return <Button onClick={this.retryOnClick}>Retry</Button>;
        else return null;
    };

    /**
     * Get data for retry
     *
     * @param deployment
     * @returns {{newNodeId: number, serverUUID, parentId: number}}
     */
    retryData = (deployment: Deployment[]) => {
        const deployObject = deployment.filter(i => i.new_node_id === this.props.node);

        return {
            parentId: deployObject[0].parent_node_id,
            newNodeId: deployObject[0].new_node_id,
            serverUUID: deployObject[0].server_uuid
        }
    };

    /**
     * TODO: Response Modals.
     *
     * PUT request to api with retryData as the body to inform the root node
     * that this node should be re-tried for deployment
     */
    retryOnClick = () => {
        fetch('/api/v1/deployment', {
            method: 'PUT',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({record: this.retryData(this.props.deployment)})
        })
            .then(webHandler)
            .then(response => {
                alert("Success " + JSON.stringify(response));
            }).catch(response => {
            alert("Error " + JSON.stringify(response));
        })

    };

    /**
     * Render server information and application status table and optionally the retry button
     *
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show}
                   style={{overlay: {zIndex: 1}}}
                   ariaHideApp={false}>
                {formatServer(this.props.deployment, this.props.servers, this.props.node)}
                {this.retryButton(this.props.deployment)}
                <Table>
                    {this.formatApplicationStatusTable(
                        this.props.applicationStatus
                            .filter(i => i.nodeid === this.props.node))}
                </Table>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
};