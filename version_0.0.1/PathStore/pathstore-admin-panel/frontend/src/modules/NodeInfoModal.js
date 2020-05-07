import React, {Component} from "react";
import Modal from "react-modal";
import Table from "react-bootstrap/Table";
import Button from "react-bootstrap/Button";
import {webHandler} from "./Utils";

/**
 * This component is used when a user clicks on a node in a pathstore topology to give the user some information
 * about the node.
 *
 * Props:
 * node: node id of the node to display information about
 * show: whether to display the modal or not
 * topology: list of deployment objects from api
 * applicationStatus: list of node application status from api
 * servers: list of server objects from api
 * callback: callback function to close modal
 *
 */
export default class NodeInfoModal extends Component {
    /**
     * Formats the data for the application status table which informs the user about the current status of
     * application installation on this particular node
     *
     * @param messages
     * @returns {[]}
     */
    formatApplicationStatusTable = (messages) => {

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

        for (let i = 0; i < messages.length; i++) {

            let currentObject = messages[i];

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
     * Format the server information about the currently selected node
     *
     * @param topology
     * @param servers
     * @returns {*}
     */
    formatServer = (topology, servers) => {

        const deployObject = topology.filter(i => i.new_node_id === this.props.node);

        const object = servers.filter(i => i.server_uuid === deployObject[0].server_uuid);

        return <div>
            <p>Server Information</p>
            <p>UUID: {object[0].server_uuid}</p>
            <p>IP: {object[0].ip}</p>
            <p>Username: {object[0].username}</p>
            <p>Name: {object[0].name}</p>
        </div>;
    };

    /**
     * Returns a button or null iff the node is eligible for re-trying deployment (the node has failed deployment)
     *
     * @param topology
     * @returns {null|*}
     */
    retryButton = (topology) => {
        const deployObject = topology.filter(i => i.new_node_id === this.props.node);

        if (deployObject[0].process_status === "FAILED")
            return <Button onClick={this.retryOnClick}>Retry</Button>;
        else return null;
    };

    /**
     * Get data for retry
     *
     * @param topology
     * @returns {{newNodeId: number, serverUUID, parentId: number}}
     */
    retryData = (topology) => {
        const deployObject = topology.filter(i => i.new_node_id === this.props.node);

        return {
            parentId: deployObject[0].parent_node_id,
            newNodeId: deployObject[0].new_node_id,
            serverUUID: deployObject[0].server_uuid
        }
    };

    /**
     * TODO: Response Modals
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
            body: JSON.stringify({record: this.retryData(this.props.topology)})
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
                   style={{overlay: {zIndex: 1}}}>
                {this.formatServer(this.props.topology, this.props.servers)}
                {this.retryButton(this.props.topology)}
                <Table>
                    {this.formatApplicationStatusTable(
                        this.props.applicationStatus
                            .filter(i => i.nodeid === this.props.node))}
                </Table>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}