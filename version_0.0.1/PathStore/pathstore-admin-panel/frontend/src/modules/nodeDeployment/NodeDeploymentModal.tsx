import React, {Component} from "react";
import {Deployment, Error, Server, Update} from "../../utilities/ApiDeclarations";
import {webHandler} from "../../utilities/Utils";
import {PathStoreTopology} from "../PathStoreTopology";
import Button from "react-bootstrap/Button";
import Modal from "react-bootstrap/Modal";
import {HypotheticalInfoModal} from "./HypotheticalInfoModal";
import NodeDeploymentAdditionForm from "./NodeDeploymentAdditionForm";
import AddServers from "./servers/AddServers"
import DisplayServers from "./servers/DisplayServers";
import {ErrorResponseModal} from "../ErrorResponseModal";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";

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
     * Set of updates node id's
     */
    readonly updateNodeIdSet: Set<number>

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

    /**
     * Whether to show the error modal or not
     */
    readonly errorModalShow: boolean

    /**
     * What to give the error modal
     */
    readonly errorModalData: Error[]
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
            updateNodeIdSet: new Set<number>(),
            infoModalShow: false,
            infoModalIsHypothetical: false,
            infoModalNodeNumber: -1,
            errorModalShow: false,
            errorModalData: []
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
            .catch((response: Error[]) => this.setState({errorModalShow: true, errorModalData: response}));
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
        this.state.updateNodeIdSet.has(object.new_node_id) ?
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
            infoModalIsHypothetical: this.state.updateNodeIdSet.has(node),
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
        this.state.updateNodeIdSet.add(updateRecord.newNodeId);

        this.setState({
            deployment: this.state.deployment,
            updates: this.state.updates,
            updateNodeIdSet: this.state.updateNodeIdSet
        });
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
        }, () =>
            this.setState(this.deleteNodeAndChildren(this.state.deployment, this.state.updates, this.state.infoModalNodeNumber)));
    };

    /**
     * This function is used to handle the deletion of a hypothetical node from the hypothetical network builder.
     *
     * What this algorithm does is that it takes the list of deployment and parses it into a map, This map is a representation
     * from P -> List<Deployment> where P is the parent node id. This is used to find a list of children based on some node number.
     * If the response is undefined you can determine that, that node id has no children.
     *
     * If the node that has been requested to be deleted has no children we can simply filter out that node id from
     * the deployment list and the updates list.
     *
     * If the node has children then you need to recursively check to see if children exist and delete them from
     * both lists as well. In this case it is easier to do the work inside the map and then after produce a list
     * based on the map, this doesn't produce any additional problems because the ordering of deployment does not
     * matter.
     *
     * @param deployment list of deployment objects
     * @param updates list of update objects
     * @param node_number node id requested to be deleted
     * @return updated state
     */
    deleteNodeAndChildren = (deployment: Deployment[], updates: Update[], node_number: number):
        { deployment: Deployment[], updates: Update[], updateNodeIdSet: Set<number>, infoModalNodeNumber: number } => {

        const deploymentMap: Map<number, Deployment[]> = new Map<number, Deployment[]>();

        for (let i = 0; i < deployment.length; i++)
            if (deployment[i].new_node_id !== node_number) {
                if (!deploymentMap.has(deployment[i].parent_node_id)) deploymentMap.set(deployment[i].parent_node_id, [deployment[i]]);
                else deploymentMap.get(deployment[i].parent_node_id)?.push(deployment[i]);
            }

        if (!deploymentMap.has(node_number)) {

            const newUpdates = updates.filter(i => i.newNodeId !== node_number);

            return {
                deployment: deployment.filter(i => i.new_node_id !== node_number),
                updates: newUpdates,
                updateNodeIdSet: new Set<number>(newUpdates.map(i => i.newNodeId)),
                infoModalNodeNumber: -1
            };
        } else {

            const updatesMap: Map<number, Update> = new Map<number, Update>();

            for (let i = 0; i < updates.length; i++)
                updatesMap.set(updates[i].newNodeId, updates[i]);

            this.deleteNodeAndChildrenHelper(deploymentMap, updatesMap, node_number);

            let newDeployment: Deployment[] = [];

            deploymentMap.forEach((value: Deployment[]) => newDeployment = newDeployment.concat(value));

            const newUpdates: Update[] = [];

            updatesMap.forEach((value: Update) => newUpdates.push(value));

            return {
                deployment: newDeployment,
                updates: newUpdates,
                updateNodeIdSet: new Set<number>(newUpdates.map(i => i.newNodeId)),
                infoModalNodeNumber: -1
            };
        }
    };

    /**
     * Helper function to recursively delete all children based on a given node id.
     *
     * @param deploymentMap deployment map to remove from
     * @param updatesMap updates map to remove from
     * @param node which node to delete
     */
    deleteNodeAndChildrenHelper = (deploymentMap: Map<number, Deployment[]>, updatesMap: Map<number, Update>, node: number): void => {
        if (deploymentMap.has(node)) {
            const children: Deployment[] | undefined = deploymentMap.get(node);

            if (children !== undefined) {
                for (let i = 0; i < children.length; i++) {
                    this.deleteNodeAndChildrenHelper(deploymentMap, updatesMap, children[i].new_node_id);
                    updatesMap.delete(children[i].new_node_id);
                }

                updatesMap.delete(node);
                deploymentMap.delete(node);
            }
        }
    };

    /**
     * Used by info modal to close itself and reset all state data associated with the modal
     */
    handleClose = (): void => this.setState(
        {
            infoModalShow: false,
            infoModalIsHypothetical: false,
            infoModalNodeNumber: -1,
            errorModalShow: false,
            errorModalData: []
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

        const errorModal =
            this.state.errorModalShow ?
                <ErrorResponseModal show={this.state.errorModalShow}
                                    data={this.state.errorModalData}
                                    callback={this.handleClose}/>
                : null;

        return (
            <Modal show={this.props.show}
                   size={"xl"}
                   centered
            >
                {modal}
                {errorModal}
                <Modal.Header>
                    <Modal.Title>Node Deployment</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <AlignedDivs>
                        <Left width='35%'>
                            <h2>Topology Legend</h2>
                            <br/>
                            <p>Deployed Node: <span className={'d_currentLine'}>Light Grey</span></p>
                            <p>Hypothetical Node: <span className={'d_cyan'}>Cyan</span></p>
                        </Left>
                        <Right>
                            <h2>Hypothetical Topology</h2>
                            <PathStoreTopology width={700}
                                               deployment={this.state.deployment}
                                               get_colour={this.isHypothetical}
                                               get_click={this.handleClick}/>
                        </Right>
                    </AlignedDivs>
                    <NodeDeploymentAdditionForm deployment={this.state.deployment}
                                                servers={this.props.servers}
                                                addition={this.handleAddition}/>
                    <hr/>
                    <DisplayServers deployment={this.props.deployment}
                                    servers={this.props.servers}
                                    forceRefresh={this.props.forceRefresh}/>
                    <hr/>
                    <AddServers servers={this.props.servers}
                                callback={this.props.forceRefresh}/>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.submit}>Submit changes</Button>

                    <Button onClick={this.props.callback}>Close</Button>
                </Modal.Footer>
            </Modal>
        );
    }
}