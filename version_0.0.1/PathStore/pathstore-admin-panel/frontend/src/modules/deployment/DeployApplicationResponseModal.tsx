import React, {Component} from "react";
import Modal from "react-modal";
import {ApplicationStatus, Deployment, Server} from "../../utilities/ApiDeclarations";
import NodeInfoModal from "../NodeInfoModal";
import {PathStoreTopology} from "../PathStoreTopology";
import {contains} from "../../utilities/Utils";

/**
 * Properties definition for {@link DeployApplicationResponseModal}
 */
interface DeployApplicationResponseModalProperties {
    /**
     * List of newly installed nodes
     */
    readonly data: ApplicationStatus[]

    /**
     * Whether to show the modal or not
     */
    readonly show: boolean

    /**
     * What application was just installed
     */
    readonly applicationName: string

    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * List of node application status from api
     */
    readonly applicationStatus: ApplicationStatus[]

    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * Callback function to close modal on completion
     */
    readonly callback: () => void
}

/**
 * State definition for {@link DeployApplicationResponseModal}
 */
interface DeployApplicationResponseModalState {
    /**
     * List of node id's that just got installed
     */
    readonly newlyInstalled: number[]

    /**
     * List of node id's that alread had the application installed on
     */
    readonly previouslyInstalled: number[]

    /**
     * Whether to show the info modal or not
     */
    readonly infoModalShow: boolean

    /**
     * What node to show in the info modal
     */
    readonly infoModalNode: number
}

/**
 * This component is loaded by DeployApplication on a successful POST request to the api.
 *
 * Note: The reason for not updating the component when the props change is because even if the props change
 * technically their request remains the same.
 */
export default class DeployApplicationResponseModal
    extends Component<DeployApplicationResponseModalProperties, DeployApplicationResponseModalState> {

    /**
     * Initializes props and state
     *
     * @param props
     */
    constructor(props: DeployApplicationResponseModalProperties) {
        super(props);

        this.state = {
            newlyInstalled:
                this.props.data
                    .map(i => i.nodeid),

            previouslyInstalled:
                this.props.applicationStatus
                    .filter(i => i.keyspace_name === this.props.applicationName)
                    .filter(i => i.process_status === "INSTALLED")
                    .map(i => i.nodeid),

            infoModalShow: false,
            infoModalNode: -1
        };
    }

    /**
     * Function for PathStoreTopology to determine what css class to give each node.
     *
     * If a node is newly installed it will be green (installation_node)
     * If a node is previously installed it will be blue (previous_node)
     * If a node is not installed it will be black (not_set_node)
     *
     * @param object
     * @returns {string}
     */
    getClassName = (object: Deployment): string => {
        if (contains<number>(this.state.newlyInstalled, object.new_node_id)) return 'installation_node';
        else if (contains<number>(this.state.previouslyInstalled, object.new_node_id)) return 'previous_node';
        else return 'not_set_node';
    };

    /**
     * Function for pathstore topology to render info modal on click of node
     *
     * @param event
     * @param node
     */
    handleClick = (event: any, node: number): void => this.setState(
        {
            infoModalShow: true,
            infoModalNode: node
        }
    );

    /**
     * Callback function for modal to close itself
     */
    closeModal = (): void => this.setState(
        {
            infoModalShow: false,
            infoModalNode: -1
        }
    );

    /**
     * Render modal with a description of the colours and their meanings also render the topology with appropriate colours
     *
     * @returns {*}
     */
    render() {

        const modal =
            this.state.infoModalShow ?
                <NodeInfoModal show={this.state.infoModalShow}
                               node={this.state.infoModalNode}
                               deployment={this.props.deployment}
                               applicationStatus={this.props.applicationStatus}
                               servers={this.props.servers}
                               callback={this.closeModal}/>
                :
                null;

        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                {modal}
                <div>
                    <p>Application {this.props.applicationName}</p>
                    <p>Blue nodes are nodes that have the application previous installed</p>
                    <p>Green nodes are nodes that you just installed the application on</p>
                    <p>Black nodes are nodes that have not be installed on</p>
                </div>
                <div>
                    <PathStoreTopology deployment={this.props.deployment.filter(i => i.process_status === "DEPLOYED")}
                                       get_colour={this.getClassName}
                                       get_click={this.handleClick}/>
                </div>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}