import React, {Component} from "react";
import ReactDOM from "react-dom";
import {Deployment, Server, Update} from "../../utilities/ApiDeclarations";
import {contains} from "../../utilities/Utils";
import {Button, Form} from "react-bootstrap";

/**
 * Properties for {@link NodeDeploymentAdditionForm}
 */
interface NodeDeploymentAdditionFormProperties {
    /**
     * List of deployment nodes from {@link NodeDeploymentModal}
     */
    readonly deployment: Deployment[]

    /**
     * List of update nodes from {@link NodeDeploymentModal}
     */
    readonly updates: Update[]

    /**
     * List of server objects from the api
     */
    readonly servers: Server[]

    /**
     * Addition function to add a node to the hypothetical network
     */
    readonly addition: (deployment: Deployment, update: Update) => void
}

/**
 * TODO: Error check inputs and display to user
 *
 * This component is a form that allows users to hypothetically add a node to the network
 */
export default class NodeDeploymentAdditionForm extends Component<NodeDeploymentAdditionFormProperties> {

    /**
     * Used to clear message form
     */
    private messageForm: any;

    /**
     * Read all data from form and push that data to the topology array and the updates array. Also clear the form when finished
     *
     * @param event
     */
    onFormSubmit = (event: any): void => {
        event.preventDefault();

        const parentId = parseInt(event.target.elements.parentId.value);

        const nodeId = parseInt(event.target.elements.nodeId.value);

        if (!this.checkValidityOfInput(parentId, nodeId)) {
            alert("You must entered a valid node id as the parent id and a unique node id as the new node id");
            return;
        }

        const serverName = event.target.elements.serverName.value;

        let serverUUID = null;

        for (let i = 0; i < this.props.servers.length; i++)
            if (this.props.servers[i].name === serverName)
                serverUUID = this.props.servers[i].server_uuid;

        if (serverUUID === null) {
            alert("Unable to find the serverUUID from servername");
            return;
        }

        this.props.addition(
            {
                parent_node_id: parentId,
                new_node_id: nodeId,
                process_status: "WAITING_DEPLOYMENT",
                server_uuid: serverUUID
            },
            {
                parentId: parentId,
                newNodeId: nodeId,
                serverUUID: serverUUID
            }
        );

        // @ts-ignore
        ReactDOM.findDOMNode(this.messageForm).reset();
    };

    /**
     * This function ensures that the inputted parentId and nodeId are valid. As in
     * the parent id exists already within the topology and the nodeId is unique
     *
     * @param parentId inputted parentNodeId
     * @param nodeId inputted nodeId
     */
    checkValidityOfInput = (parentId: number, nodeId: number): boolean => {
        const mapDeployment = this.props.deployment.map(i => i.new_node_id);

        return contains<number>(mapDeployment, parentId) && !contains<number>(mapDeployment, nodeId);
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
            if (!contains(this.props.deployment.map(i => i.server_uuid), this.props.servers[i].server_uuid))
                servers.push(
                    <option key={i}>{this.props.servers[i].name}</option>
                );

        const form = servers.length > 0 ?
            <Form onSubmit={this.onFormSubmit} ref={(form: any) => this.messageForm = form}>
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