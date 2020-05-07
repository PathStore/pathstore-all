import React, {Component} from "react";
import Modal from "react-modal";
import Table from "react-bootstrap/Table";
import Button from "react-bootstrap/Button";
import {webHandler} from "../Utils";

export default class NodeInfoModal extends Component {

    constructor(props) {
        super(props);
        this.state = {
            retryData: null
        };
    }

    formatClickEvent = (messages) => {

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

    retryButton = (topology) => {
        const deployObject = topology.filter(i => i.new_node_id === this.props.node);

        if (deployObject[0].process_status === "FAILED") {
            this.setState({
                retryData: {
                    parentId: deployObject[0].parent_node_id,
                    newNodeId: deployObject[0].new_node_id,
                    serverUUID: deployObject[0].server_uuid
                }
            });
            return <Button onClick={this.retryOnClick}>Retry</Button>;
        } else return null;
    };

    retryOnClick = () => {
        alert(JSON.stringify(this.state.retryData));
        fetch('/api/v1/deployment', {
            method: 'PUT',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({record: this.state.retryData})
        })
            .then(webHandler)
            .then(response => {
                alert("Success " + JSON.stringify(response));
            }).catch(response => {
            alert("Error " + JSON.stringify(response));
        })

    };

    render() {
        return (
            <Modal isOpen={this.props.show}
                   style={{overlay: {zIndex: 1}}}>
                {this.formatServer(this.props.topology, this.props.servers)}
                {this.retryButton(this.props.topology)}
                <Table>{this.formatClickEvent(this.props.applicationStatus.filter(i => i.nodeid === this.props.node))}</Table>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}