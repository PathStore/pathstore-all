import React, {Component} from "react";
import {Deployment, Error, Server, Update} from "../../utilities/ApiDeclarations";
import {createMap, identity, webHandler} from "../../utilities/Utils";
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
     * List of network additions
     */
    readonly additions: Update[]

    /**
     * List of network deletions
     */
    readonly deletions: Update[]

    /**
     * Set of addition node id's
     */
    readonly additionNodeIdSet: Set<number>

    /**
     * Set of deleted node id's
     */
    readonly deletionsNodeIdSet: Set<number>

    /**
     * Whether or not to show the info modal
     */
    readonly infoModalShow: boolean

    /**
     * What node was clicked
     */
    readonly infoModalNodeNumber: number

    /**
     * Whether the node clicked is a hypothetical node
     */
    readonly infoModalIsHypothetical: boolean

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
            additions: [],
            deletions: [],
            additionNodeIdSet: new Set<number>(),
            deletionsNodeIdSet: new Set<number>(),
            infoModalShow: false,
            infoModalNodeNumber: -1,
            infoModalIsHypothetical: false,
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
        if (this.state.additions.length <= 0 && this.state.deletions.length <= 0) {
            alert("You have not made any changes to the network");
            return;
        }

        if (this.state.deletions.length > 0)
            fetch('/api/v1/deployment', {
                method: 'DELETE',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    records: this.state.deletions
                })
            })
                .then(webHandler)
                .then(() => {
                    this.props.forceRefresh();
                    this.props.callback();
                })
                .catch((response: Error[]) => this.setState({errorModalShow: true, errorModalData: response}));

        if (this.state.additions.length > 0)
            fetch('/api/v1/deployment', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    records: this.state.additions
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
    getColour = (object: Deployment): string =>
        this.state.additionNodeIdSet.has(object.new_node_id) ?
            'hypothetical'
            :
            this.state.deletionsNodeIdSet.has(object.new_node_id) ?
                'uninstalled_node'
                :
                object.process_status === "DEPLOYED" ?
                    'not_set_node'
                    : 'waiting_node';

    /**
     * This function is used to update the state to display an info modal on click
     *
     * @param event
     * @param node
     */
    handleClick = (event: any, node: number): void => this.setState(
        {
            infoModalShow: true,
            infoModalIsHypothetical: this.state.additionNodeIdSet.has(node),
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
        this.state.additions.push(updateRecord);
        this.state.additionNodeIdSet.add(updateRecord.newNodeId);

        this.setState({
            deployment: this.state.deployment,
            additions: this.state.additions,
            additionNodeIdSet: this.state.additionNodeIdSet
        });
    };

    /**
     * This function is used as a callback function for the hypothetical info modal. It will delete a subtree of the
     * topology based on which node was clicked to delete. It will also close the modal and
     *
     * @param event
     */
    handleHypotheticalDelete = (event: any): void => {
        try {
            const response = this.deleteSubTree(this.state.deployment, this.state.additions, this.state.deletions, this.state.infoModalNodeNumber);
            this.setState(response, () => {
                this.setState({
                    infoModalShow: false,
                    infoModalNodeNumber: -1,
                    infoModalIsHypothetical: false
                });
            });
        }catch (e) {
            alert("You cannot perform a delete on a sub tree that has a deploying node");
        }
    };

    /**
     * This function is used to handle the deletion of a sub tree from the network based on what node the info modal
     * was loaded for. This function handles all cases whether the sub tree is purley comprised of hypothetical additions,
     * all deployed nodes, or a mixture of both. To see the logic of how each value is modified see {@link handleDeletion}
     *
     * @param deployment deployment objects from the internal state
     * @param additions addition objects from the internal state
     * @param deletions deletion objects from the internal state
     * @param node_number what node was click on for the info modal
     */
    deleteSubTree = (deployment: Deployment[], additions: Update[], deletions: Update[], node_number: number):
        { deployment: Deployment[], additions: Update[], deletions: Update[], additionNodeIdSet: Set<number>, deletionsNodeIdSet: Set<number> } => {

        const deploymentMap: Map<number, Deployment> = createMap<number, Deployment>(v => v.new_node_id, identity, deployment);
        const additionMap: Map<number, Update> = createMap<number, Update>(v => v.newNodeId, identity, additions);
        const deletionsMap: Map<number, Update> = createMap<number, Update>(v => v.newNodeId, identity, deletions);

        const nodeToListOfChildren: Map<number, Deployment[]> = new Map<number, Deployment[]>();

        deployment.forEach(v => {
            if (!nodeToListOfChildren.has(v.parent_node_id)) nodeToListOfChildren.set(v.parent_node_id, [v]);
            else nodeToListOfChildren.get(v.parent_node_id)?.push(v);
        });

        if (nodeToListOfChildren.has(node_number))
            this.deleteSubTreeHelper(deploymentMap, nodeToListOfChildren, additionMap, deletionsMap, node_number);

        this.handleDeletion(deploymentMap, additionMap, deletionsMap, node_number);

        const newDeployment = Array.from(deploymentMap.values());
        const newUpdates = Array.from(additionMap.values());
        const newDeletions = Array.from(deletionsMap.values());

        return (
            {
                deployment: newDeployment,
                additions: newUpdates,
                deletions: newDeletions,
                additionNodeIdSet: new Set<number>(newUpdates.map(i => i.newNodeId)),
                deletionsNodeIdSet: new Set<number>(newDeletions.map(i => i.newNodeId))
            }
        );

    };

    /**
     * This function will perform operations from a back to front order (furthest away from the root to the root node).
     * The operations performed are based on what type the node is see {@link handleDeletion}
     *
     * @param deploymentMap newNodeId to deployment object
     * @param nodeToListOfChildren newNodeId to list of children
     * @param additionMap newNodeId to update object
     * @param deletionsMap newNodeId to deletions object
     * @param node node currently inspecting
     */
    deleteSubTreeHelper = (deploymentMap: Map<number, Deployment>, nodeToListOfChildren: Map<number, Deployment[]>, additionMap: Map<number, Update>, deletionsMap: Map<number, Update>, node: number): void => {
        if (nodeToListOfChildren.has(node)) {

            const children: Deployment[] | undefined = nodeToListOfChildren.get(node);

            // Only here because Map.get may return undefined even though
            if (children !== undefined)
                children.forEach(c => {
                    this.deleteSubTreeHelper(deploymentMap, nodeToListOfChildren, additionMap, deletionsMap, c.new_node_id);

                    this.handleDeletion(deploymentMap, additionMap, deletionsMap, c.new_node_id);
                });
        }
    };

    /**
     * If hypo then remove the updates record and remove the deployment record
     *
     * Else add deletion record
     *
     * @param deploymentMap map of newNodeId to deployment object
     * @param updatesMap map of newNodeId to update object
     * @param deletionsMap map of newNodeId to deletion object
     * @param node node to handle
     */
    handleDeletion = (deploymentMap: Map<number, Deployment>, updatesMap: Map<number, Update>, deletionsMap: Map<number, Update>, node: number): void => {
        if (updatesMap.has(node)) {
            updatesMap.delete(node);
            deploymentMap.delete(node);
        } else {
            if (deploymentMap.get(node)?.process_status !== "DEPLOYED")
                throw new Error("Cannot perform a delete operation on a sub-tree with a currently deploying node");

            const value = this.conversion(deploymentMap.get(node));

            if (value !== undefined)
                deletionsMap.set(node, value);
        }
    };

    /**
     * Strips a deployment object of certain information to produce an update object
     *
     * @param deployment deployment object to produce an update object
     */
    conversion = (deployment: Deployment | undefined): Update | undefined => {
        return deployment !== undefined ? {
            newNodeId: deployment.new_node_id,
            parentId: deployment.parent_node_id,
            serverUUID: deployment.server_uuid
        } : undefined;
    };

    /**
     * This function is used to reset all changes without closing
     *
     * @param event
     */
    resetChanges = (event: any) => this.setState({
        deployment: this.props.deployment.filter(i => i.process_status === "DEPLOYED").slice(),
        additions: [],
        deletions: [],
        additionNodeIdSet: new Set<number>(),
        deletionsNodeIdSet: new Set<number>()
    });

    /**
     * Used by info modal to close itself and reset all state data associated with the modal
     */
    handleClose = (): void => this.setState(
        {
            infoModalShow: false,
            infoModalNodeNumber: -1,
            infoModalIsHypothetical: false,
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
                                       node={this.state.infoModalNodeNumber}
                                       isHypothetical={this.state.infoModalIsHypothetical}
                                       servers={this.props.servers}
                                       deployment={this.state.deployment}
                                       deleteNode={this.handleHypotheticalDelete}
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
                                               get_colour={this.getColour}
                                               get_click={this.handleClick}/>
                            <Button onClick={this.resetChanges}>Reset to default</Button>
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