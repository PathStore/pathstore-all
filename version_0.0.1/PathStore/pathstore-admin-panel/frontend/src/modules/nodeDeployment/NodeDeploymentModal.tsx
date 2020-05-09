import React, {Component} from "react";
import {Deployment, Server, Update} from "../../utilities/ApiDeclarations";
import {contains, webHandler} from "../../utilities/Utils";
import {PathStoreTopology} from "../PathStoreTopology";
import Button from "react-bootstrap/Button";
import Modal from "react-modal";
import {HypotheticalInfoModal} from "./HypotheticalInfoModal";
import NodeDeploymentAdditionForm from "./NodeDeploymentAdditionForm";
import AddServers from "./servers/AddServers"
import {DisplayServers} from "./servers/DisplayServers";

/**
 * Properties definition for {@link NodeDeploymentModal}
 */
interface NodeDeploymentModalProperties {
    /**
     * Whether to show the modal or not
     */
    readonly show: boolean

    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * Callback function to refresh all component props
     */
    readonly forceRefresh: () => void

    /**
     * Callback function to close modal
     */
    readonly callback: () => void
}

/**
 * State definition for {@link NodeDeploymentModal}
 */
interface NodeDeploymentModalState {
    /**
     * Copied list of deployment objects for local customization
     */
    readonly deployment: Deployment[]

    /**
     * List of updates nodes to determine a node is hypothetical or not
     */
    readonly updates: Update[]

    /**
     * Whether or not to show the info modal
     */
    readonly infoModalShow: boolean

    /**
     * Whether the node clicked is a hypothetical node
     */
    readonly infoModalIsHypothetical: boolean

    /**
     * What node was clicked
     */
    readonly infoModalNodeNumber: number
}

/**
 * This component is used to allow users to deploy additional nodes to their pathstore network
 */
export default class NodeDeploymentModal extends Component<NodeDeploymentModalProperties, NodeDeploymentModalState> {

    /**
     * Initialize props and state
     *
     * @param props
     */
    constructor(props: NodeDeploymentModalProperties) {
        super(props);

        this.state = {
            deployment: props.deployment.slice(),
            updates: [],
            infoModalShow: false,
            infoModalIsHypothetical: false,
            infoModalNodeNumber: -1
        };
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
    submit = (): void => {
        if (this.state.updates.length <= 0) {
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
                records: this.state.updates
            })
        })
            .then(webHandler)
            .then(() => {
                this.props.forceRefresh();
                this.props.callback();
            })
            .catch(response => {
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
    isHypothetical = (object: Deployment): string =>
        contains<number>(this.state.updates.map(i => i.newNodeId), object.new_node_id) ?
            'hypothetical'
            : 'not_set_node';

    /**
     * This function is used to update the state to display an info modal on click
     *
     * @param event
     * @param node
     */
    handleClick = (event: any, node: number): void => this.setState(
        {
            infoModalShow: true,
            infoModalIsHypothetical: contains<number>(this.state.updates.map(i => i.newNodeId), node),
            infoModalNodeNumber: node
        }
    );

    /**
     * Callback function used for NodeDeploymentAdditionForm to handle new node creation
     *
     * @param topologyRecord record to write to topology
     * @param updateRecord record to write to updates
     */
    handleAddition = (topologyRecord: Deployment, updateRecord: Update): void => {
        this.state.deployment.push(topologyRecord);
        this.state.updates.push(updateRecord);

        this.setState({deployment: this.state.deployment, updates: this.state.updates});
    };

    /**
     * Used by info modal to delete a node from the topology iff the node is hypothetical.
     *
     * We first must update the state so that the info modal is closed as otherwise an error will occur within the
     * info modal.
     *
     * Then the topology and updates arrays are filtered to remove the delete node and infoModalNodeNumber is reset
     *
     * @param event not used
     */
    handleDelete = (event: any): void => {
        this.setState({
            infoModalShow: false,
            infoModalIsHypothetical: false
        }, () => this.setState({
            deployment: this.state.deployment.filter(i => i.new_node_id !== this.state.infoModalNodeNumber),
            updates: this.state.updates.filter(i => i.newNodeId !== this.state.infoModalNodeNumber),
            infoModalNodeNumber: -1
        }));
    };

    /**
     * Used by info modal to close itself and reset all state data associated with the modal
     */
    handleClose = (): void => this.setState(
        {
            infoModalShow: false,
            infoModalIsHypothetical: false,
            infoModalNodeNumber: -1
        }
    );

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
        const modal =
            this.state.infoModalShow ?
                <HypotheticalInfoModal show={this.state.infoModalShow}
                                       hypothetical={this.state.infoModalIsHypothetical}
                                       node={this.state.infoModalNodeNumber}
                                       servers={this.props.servers}
                                       deployment={this.state.deployment}
                                       deleteNode={this.handleDelete}
                                       callback={this.handleClose}/>
                : null;

        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                {modal}
                <div>
                    <PathStoreTopology deployment={this.state.deployment}
                                       get_colour={this.isHypothetical}
                                       get_click={this.handleClick}/>
                </div>

                <NodeDeploymentAdditionForm deployment={this.state.deployment}
                                            updates={this.state.updates}
                                            servers={this.props.servers}
                                            addition={this.handleAddition}/>

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