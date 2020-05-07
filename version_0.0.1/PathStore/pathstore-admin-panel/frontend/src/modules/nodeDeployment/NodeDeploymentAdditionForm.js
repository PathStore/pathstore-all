import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import {Button} from "react-bootstrap";
import ReactDOM from "react-dom";

export default class NodeDeploymentAdditionForm extends Component {

    /**
     * TODO: Check newNodeId and parentId is valid
     *
     * @param event
     */
    onFormSubmit = (event) => {
        event.preventDefault();

        const parentId = parseInt(event.target.elements.parentId.value);

        const nodeId = parseInt(event.target.elements.nodeId.value);

        const serverName = event.target.elements.serverName.value;

        let serverUUID;

        for (let i = 0; i < this.props.servers.length; i++)
            if (this.props.servers[i].name === serverName)
                serverUUID = this.props.servers[i].server_uuid;

        this.props.topology.push({
            parent_node_id: parentId,
            new_node_id: nodeId,
            process_status: "WAITING_DEPLOYMENT",
            server_uuid: serverUUID
        });

        this.props.updates.push({
            parent_node_id: parentId,
            new_node_id: nodeId,
            server_uuid: serverUUID
        });

        ReactDOM.findDOMNode(this.messageForm).reset();
    };


    render() {
        const servers = [];

        for (let i = 0; i < this.props.servers.length; i++)
            if (!topologyContainsServer(this.props.topology, this.props.servers[i].server_uuid))
                servers.push(
                    <option key={i}>{this.props.servers[i].name}</option>
                );

        const form = servers.length > 0 ?
            <Form onSubmit={this.onFormSubmit} ref={form => this.messageForm = form}>
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
                        {servers}
                    </Form.Control>
                </Form.Group>
                <Button variant="primary" type="submit">
                    Submit
                </Button>
            </Form> : <p>There are no free servers available, you need to add a server to add a node to the network</p>;

        return (
            <div>
                {form}
            </div>
        );
    }
}

function topologyContainsServer(topology, server) {
    for (let i = 0; i < topology.length; i++)
        if (topology[i].server_uuid === server) return true;

    return false;
}