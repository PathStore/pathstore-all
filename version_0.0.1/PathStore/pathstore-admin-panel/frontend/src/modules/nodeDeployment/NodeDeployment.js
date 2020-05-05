import ReactDOM from 'react-dom'
import React, {Component} from "react";
import {Button} from "react-bootstrap";
import Modal from "react-modal";
import Tree from "react-tree-graph";
import Form from "react-bootstrap/Form";
import Servers from "../servers/Servers";


/**
 * Props:
 *
 * topology: data from API
 * servers: server info from api
 * forceRefresh: refresh serverInfo
 */
export default class NodeDeployment extends Component {

    /**
     * State:
     * show: whether to show the modal or not
     *
     * @param props
     */
    constructor(props) {
        super(props);

        this.state = {
            show: false
        };
    }

    /**
     * Close modal
     */
    callBack = () => this.setState({show: false});


    /**
     * Render button
     * @returns {*}
     */
    render() {
        return (
            <div>
                {this.state.show ? <NodeDeploymentModal topology={this.props.topology} show={this.state.show}
                                                        servers={this.props.servers}
                                                        forceRefresh={this.props.forceRefresh}
                                                        callback={this.callBack}/> : null}
                <Button onClick={() => this.setState({show: true})}>Deploy Additional Nodes to Network</Button>
            </div>
        );
    }
}

class NodeDeploymentModal extends Component {

    /**
     * reference to form
     * @type {null}
     */
    messageForm = null;

    /**
     * State:
     * topology: is array of objects denoting hierarchical structure
     * updates: array of cached updates to be pushed after creation of your network update
     * parentNodeId: current parentNodeId inputted on the form
     * newNodeId: current newNodeId inputted on the form
     * serverUUID: currently selected serverUUID
     * serverName: name of currently select server
     *
     * @param props
     */
    constructor(props) {
        super(props);

        this.state = {
            topology: props.topology.slice(),
            updates: [],
            parentNodeId: null,
            newNodeId: null,
            serverUUID: null,
            serverName: null
        }
    }

    /**
     * Set initial serverUUID
     */
    componentDidMount() {
        this.setState({serverUUID: this.props.servers[0] !== undefined ? this.props.servers[0].server_uuid : null});
    }

    /**
     * Creates interpretable json object for the Tree package
     *
     * @param array from parent
     * @param parentId -1 for initial call to look for the root node and work way down
     * @returns {{textProps: {x: number, y: number}, children: [], name: *}|[]}
     */
    createTree = (array, parentId) => {
        let children = [];

        for (let i = 0; i < array.length; i++)
            if (parentId === -1) {
                if (array[i].parentid === parentId) return this.createTreeObject(array[i], array);
            } else {
                if (array[i].parentid === parentId) children.push(this.createTreeObject(array[i], array));
            }

        return children;
    };

    /**
     * Name is the node id, textProps is the location of the text associated with the node, children is a list of children
     *
     * @param object
     * @param array
     * @returns {{textProps: {x: number, y: number}, children: ({textProps: {x: number, y: number}, children: *[], name: *}|*[]), name: *}}
     */
    createTreeObject = (object, array) => {
        return {
            name: object.id,
            textProps: {x: -20, y: 25},
            pathProps: {className: 'installation_path'},
            gProps: {className: this.isHypothetical(object.processStatus)},
            children: this.createTree(array, object.id)
        }
    };

    /**
     * This function returns the css name for the node depending on whether the node is hypothetical or not
     * @param status
     * @returns {string}
     */
    isHypothetical = (status) => {
        switch (status) {
            case "WAITING_DEPLOYMENT":
            case "DEPLOYING":
                return 'hypothetical';
            default:
                return 'not_set_node';
        }
    };


    /**
     * TODO: Check newNodeId and parentId is valid
     *
     * @param event
     */
    onFormSubmit = (event) => {
        event.preventDefault();

        this.state.topology.push({
            parentid: parseInt(this.state.parentNodeId),
            id: parseInt(this.state.newNodeId),
            processStatus: "WAITING_DEPLOYMENT"
        });

        this.state.updates.push({
            parentId: this.state.parentNodeId,
            newNodeId: this.state.newNodeId,
            serverUUID: this.state.serverUUID
        });

        this.setState({topology: this.state.topology, updates: this.state.updates});
        ReactDOM.findDOMNode(this.messageForm).reset();
    };

    /**
     * Submit all updates to the api.
     */
    submit = () => {
        fetch('/api/v1/deployment', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                records: this.state.updates
            })
        })
            .then(response => response.json())
            .then(response => {
                alert("Success " + JSON.stringify(response));
            }).catch(response => {
            alert("ERROR: " + JSON.stringify(response));
        });
    };

    /**
     * Update the parentNodeId based on what the user has given in the form
     *
     * @param event
     */
    onParentNodeIdChange = (event) => this.setState({parentNodeId: parseInt(event.target.value)});

    /**
     * Updates the newNodeId based on what the user has given in the form
     *
     * @param event
     */
    onNewNodeIdChange = (event) => this.setState({newNodeId: parseInt(event.target.value)});

    /**
     * Updates the serverUUID based on what the user has given in the form
     *
     * @param event
     */
    onServerUUIDChange = (event) => {

        const serverName = event.target.value;

        for (let i = 0; i < this.props.servers.length; i++)
            if (this.props.servers[i].name === serverName)
                this.setState({serverUUID: this.props.servers[i].server_uuid, serverName: serverName});
    };

    /**
     * Closes modal without submitting updates
     */
    onClose = () => this.setState({topology: this.props.topology}, this.props.callback);

    /**
     * Callback for server creater to reload all servers from api to display on the form
     */
    serverUpdateCallBack = () => {
        this.props.forceRefresh();
        this.componentDidMount();
    };

    render() {

        const servers = [];

        for (let i = 0; i < this.props.servers.length; i++)
            servers.push(
                <option key={i}>{this.props.servers[i].name}</option>
            );

        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}}>
                <div>
                    <Tree data={this.createTree(this.state.topology, -1)}
                          nodeRadius={15}
                          margins={{top: 20, bottom: 10, left: 20, right: 200}}
                          height={1000}
                          width={1080}
                          gProps={{
                              className: 'node'
                          }}
                    />
                </div>
                <div>
                    <Form onSubmit={this.onFormSubmit} ref={form => this.messageForm = form}>
                        <Form.Group controlId="parentId">
                            <Form.Label>Parent Node Id</Form.Label>
                            <Form.Control type="text" placeholder="Parent Id" onChange={this.onParentNodeIdChange}/>
                            <Form.Text className="text-muted">
                                Must be an integer
                            </Form.Text>
                        </Form.Group>
                        <Form.Group controlId="nodeId">
                            <Form.Label>New Node Id</Form.Label>
                            <Form.Control type="text" placeholder="New Node Id" onChange={this.onNewNodeIdChange}/>
                            <Form.Text className="text-muted">
                                Must be an integer
                            </Form.Text>
                        </Form.Group>
                        <Form.Control as="select" single onChange={this.onServerUUIDChange}
                                      value={this.state.serverName}>
                            {servers}
                        </Form.Control>
                        <Button variant="primary" type="submit">
                            Submit
                        </Button>
                    </Form>
                </div>
                <div>
                    <Servers servers={this.props.servers} callback={this.serverUpdateCallBack}/>
                </div>
                <Button onClick={this.submit}>Submit changes</Button>
                <Button onClick={this.onClose}>Close</Button>
            </Modal>
        );
    }
}