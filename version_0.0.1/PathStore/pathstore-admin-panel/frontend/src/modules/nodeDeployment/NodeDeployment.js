import React, {Component} from "react";
import {Button} from "react-bootstrap";
import Modal from "react-modal";
import AddServers from "../servers/AddServers";
import PathStoreTopology from "../PathStoreTopology";
import NodeDeploymentAdditionForm from "./NodeDeploymentAdditionForm";
import {contains} from "../Utils";
import DisplayServers from "../servers/DisplayServers";


/**
 * This is the parent class for the node deployment section of the website. This component is used
 * to add servers and deploy additional nodes to the topology
 *
 * Props:
 * topology: list of deployment objects form api
 * servers: list of server objects from api
 * forceRefresh: callback to force refresh all other components props
 */
export default class NodeDeployment extends Component {

    /**
     * State:
     * show: whether or not to show the node deployment modal
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
     * Used to call when the user clicks the show modal button
     */
    showModal = () => this.setState({show: true});

    /**
     * Used for the modal to close itself
     */
    callBack = () => this.setState({show: false});

    /**
     * Check if you need to render the modal and then render the deploy additional nodes button
     *
     * @returns {*}
     */
    render() {

        const modal =
            this.state.show ?
                <NodeDeploymentModal show={this.state.show}
                                     topology={this.props.topology}
                                     servers={this.props.servers}
                                     forceRefresh={this.props.forceRefresh}
                                     callback={this.callBack}/>
                : null;

        return (
            <div>
                {modal}
                <Button onClick={this.showModal}>Deploy Additional Nodes to Network</Button>
            </div>
        );
    }
}

/**
 * TODO: Make nodes in hypo topology clickable
 * TODO: Make nodes in hypo topology deletable
 *
 * This component is used to allow users to deploy additional nodes to their pathstore network
 *
 * Props:
 * show: whether to show the modal or not
 * topology: list of deployment objects from api
 * servers: list of server objects from api
 * forceRefresh: callback function to resfresh all components props
 * callback: callback function to close modal
 */
class NodeDeploymentModal extends Component {

    /**
     * State:
     * topology: internal topology array as this will be used to render a hypothetical tree
     *           that shouldn't be propagated to other components
     * updates: list of update nodes. Used when the user wants to submit their changes to the network
     *
     * @param props
     */
    constructor(props) {
        super(props);

        this.state = {
            topology: props.topology.slice(),
            updates: []
        }
    }

    /**
     * TODO: Response modals
     *
     * Formats all updates into the required format for the api
     *
     * Alert the user if updates are empty as you cannot submit no changes.
     *
     * Then make the request with a json body of all the updates
     *
     */
    submit = () => {

        const formattedUpdates = [];

        for (let i = 0; i < this.state.updates.length; i++) {
            formattedUpdates.push({
                parentId: this.state.updates[i].parent_node_id,
                newNodeId: this.state.updates[i].new_node_id,
                serverUUID: this.state.updates[i].server_uuid
            })
        }

        if (formattedUpdates.length <= 0) {
            alert("You have not made any changes to the network");
            return;
        }

        fetch('/api/v1/deployment', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                records: formattedUpdates
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
     * Colour function for pathstore topology.
     *
     * Colours nodes based on whether the node is inside the updates array or not
     *
     * Hypothetical nodes are blue (hypothetical)
     * Original nodes are black (not_set_node)
     *
     * @param object
     * @returns {string}
     */
    isHypothetical = (object) =>
        contains(this.state.updates.map(i => i.new_node_id), object.new_node_id) ? 'hypothetical' : 'not_set_node';


    /**
     * Render Modal and show the hypothetical topology
     *
     * render the node deployment addition form to allow users to generate a hypothetical topology
     *
     * render servers component to allow users to add servers
     *
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <div>
                    <PathStoreTopology topology={this.state.topology}
                                       get_colour={this.isHypothetical}/>
                </div>

                <NodeDeploymentAdditionForm topology={this.state.topology}
                                            updates={this.state.updates}
                                            servers={this.props.servers}/>

                <DisplayServers servers={this.props.servers}/>

                <AddServers servers={this.props.servers}
                            callback={this.props.forceRefresh}/>


                <div>
                    <Button onClick={this.submit}>Submit changes</Button>
                </div>
                <div>
                    <Button onClick={this.props.callback}>Close</Button>
                </div>
            </Modal>
        );
    }
}