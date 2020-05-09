import React, {Component} from "react";
import {Deployment, Server} from "../../utilities/ApiDeclarations";
import {Button} from "react-bootstrap";
import NodeDeploymentModal from "./NodeDeploymentModal";

/**
 * Properties definition for {@link NodeDeployment}
 */
interface NodeDeploymentProperties {
    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * Callback function to force refresh all other components props
     */
    readonly forceRefresh: () => void
}

/**
 * State definition for {@link NodeDeployment}
 */
interface NodeDeploymentState {
    /**
     * Whether to show the node deployment modal or not
     */
    readonly show: boolean
}

/**
 * This component is the parent component of all node deployment related components
 */
export default class NodeDeployment extends Component<NodeDeploymentProperties, NodeDeploymentState> {
    /**
     * Initialize props and state
     *
     * @param props
     */
    constructor(props: NodeDeploymentProperties) {
        super(props);

        this.state = {
            show: false
        };
    }

    /**
     * Used to call when the user clicks the show modal button
     */
    showModal = (): void => this.setState({show: true});

    /**
     * Used for the modal to close itself
     */
    callBack = (): void => this.setState({show: false});

    /**
     * Check if you need to render the modal and then render the deploy additional nodes button
     *
     * @returns {*}
     */
    render() {

        const modal =
            this.state.show ?
                <NodeDeploymentModal show={this.state.show}
                                     deployment={this.props.deployment}
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