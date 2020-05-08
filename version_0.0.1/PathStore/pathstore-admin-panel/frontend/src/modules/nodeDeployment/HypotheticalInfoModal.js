import React, {Component} from "react";
import Modal from "react-modal";
import {Button} from "react-bootstrap";
import {formatServer} from "../Utils";

/**
 * TODO: Allow removal of non-hypothetical nodes
 *
 * This component is used to give node information in the hypothetical topology viewer in the node deployment
 * component. It allows you to remove hypothetical nodes that you could have misplaced or named improperly
 *
 * Props:
 * show: whether to show the node or not
 * hypothetical: whether the node is hypothetical or not
 * node: node id of the node to give info on
 * servers: list of server objects from api
 * topology: copy of topology array from NodeDeployment
 * deleteNode: delete Node callback function
 * callback: callback function to close node
 */
export default class HypotheticalInfoModal extends Component {

    /**
     * If the node is hypothetical give them an option to remove the node from the hypothetical topology
     *
     * @returns {*}
     */
    canDelete = () => this.props.hypothetical ? <Button onClick={this.props.deleteNode}>Delete</Button> : null;

    /**
     * Render modal with the server info and optional a delete button
     *
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <p>Info Modal for
                    node {this.props.node} and {this.props.hypothetical ? "Is hypothetical" : "Is not hypothetical"}</p>
                {formatServer(this.props.topology, this.props.servers, this.props.node)}
                {this.canDelete()}
                <div>
                    <Button onClick={this.props.callback}>Close</Button>
                </div>
            </Modal>
        );
    }
}