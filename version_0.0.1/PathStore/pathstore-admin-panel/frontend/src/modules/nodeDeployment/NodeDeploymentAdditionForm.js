import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import {Button} from "react-bootstrap";
import ReactDOM from "react-dom";
import {contains} from "../Utils";

/**
 * TODO: Error check inputs and display to user
 *
 * This component is a form that allows users to hypothetically add a node to the network
 *
 * Props:
 * topology: sliced topology from NodeDeployment state
 * updates: list of nodes added that aren't committed. Stored in NodeDeployment
 * servers: list of server objects from api
 */
export default class NodeDeploymentAdditionForm extends Component {

    /**
     * Read all data from form and push that data to the topology array and the updates array. Also clear the form when finished
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


    /**
     * First gather all servers that are free (Not currently hosting another existing pathstore instance
     * or are not in your hypothetical plan)
     *
     * Then if there are any free servers load the form for the user to use else inform them there are no
     * free servers and they must create one to continue
     *
     * @returns {*}
     */
    render() {
        const servers = [];

        for (let i = 0; i < this.props.servers.length; i++)
            if (!contains(this.props.topology.map(i => i.server_uuid), this.props.servers[i].server_uuid))
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