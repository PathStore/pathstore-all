import ReactDOM from 'react-dom'
import React, {Component} from "react";
import {Button} from "react-bootstrap";
import Modal from "react-modal";
import Tree from "react-tree-graph";
import Form from "react-bootstrap/Form";


/**
 * Props:
 *
 * topology: data from API
 */
export default class NodeDeployment extends Component {

    constructor(props) {
        super(props);

        this.state = {
            show: false
        };
    }

    callBack = () => this.setState({show: false});


    render() {
        return (
            <div>
                {this.state.show ? <NodeDeploymentModal topology={this.props.topology} show={this.state.show}
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
     * topology is array of objects denoting hierarchical structure
     * update: list of hypothetical nodeid's
     *
     * @param props
     */
    constructor(props) {
        super(props);

        this.state = {
            topology: props.topology.slice(),
            update: [],
            parentNodeId: null,
            newNodeId: null
        }
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
                if (array[i].parentid === parentId) return this.createTreeObject(array[i].id, array);
            } else {
                if (array[i].parentid === parentId) children.push(this.createTreeObject(array[i].id, array));
            }

        return children;
    };

    /**
     * Name is the node id, textProps is the location of the text associated with the node, children is a list of children
     *
     * @param name
     * @param array
     * @returns {{textProps: {x: number, y: number}, children: ({textProps: {x: number, y: number}, children: *[], name: *}|*[]), name: *}}
     */
    createTreeObject = (name, array) => {
        return {
            name: name,
            textProps: {x: -20, y: 25},
            pathProps: {className: 'installation_path'},
            gProps: {className: this.isHypothetical(parseInt(name))},
            children: this.createTree(array, name)
        }
    };

    /**
     * This function returns the css name for the node depending on whether the node is hypothetical or not
     * @param name
     * @returns {string}
     */
    isHypothetical = (name) => {
        for (let i = 0; i < this.state.update.length; i++)
            if (this.state.update[i] === name) return 'hypothetical';
        return 'not_set_node';
    };

    onFormSubmit = (event) => {
        event.preventDefault();

        this.state.topology.push({parentid: parseInt(this.state.parentNodeId), id: parseInt(this.state.newNodeId)});
        this.state.update.push(parseInt(this.state.newNodeId));
        this.setState({topology: this.state.topology, update: this.state.update});
        ReactDOM.findDOMNode(this.messageForm).reset();
    };

    onParentNodeIdChange = (event) => this.setState({parentNodeId: parseInt(event.target.value)});

    onNewNodeIdChange = (event) => this.setState({newNodeId: parseInt(event.target.value)});

    onClose = () => this.setState({topology: this.props.topology, update: []}, this.props.callback);

    render() {
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
                        <Button variant="primary" type="submit">
                            Submit
                        </Button>
                    </Form>
                </div>
                <div>
                    <p>Data:</p>
                    <p>{JSON.stringify(this.state.topology)}</p>
                    <p>{JSON.stringify(this.state.update)}</p>
                </div>
                <button onClick={this.onClose}>close</button>
            </Modal>
        );
    }
}