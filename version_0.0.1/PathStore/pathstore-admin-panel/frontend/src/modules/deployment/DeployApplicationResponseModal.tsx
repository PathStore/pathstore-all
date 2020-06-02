import React, {Component, RefObject} from "react";
import {ApplicationStatus, Deployment, Server} from "../../utilities/ApiDeclarations";
import NodeInfoModal from "../NodeInfoModal";
import {PathStoreTopology} from "../PathStoreTopology";
import {Button} from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";

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
    readonly newlyInstalled: Set<number>

    /**
     * List of node id's that alread had the application installed on
     */
    readonly previouslyInstalled: Set<number>

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
     * Reference to the right div to determine the height and width
     */
    private rightRef: RefObject<HTMLDivElement> = React.createRef<HTMLDivElement>();

    /**
     * Initializes props and state
     *
     * @param props
     */
    constructor(props: DeployApplicationResponseModalProperties) {
        super(props);

        this.state = {
            newlyInstalled:
                new Set<number>(
                    this.props.data
                        .map(i => i.node_id)
                ),

            previouslyInstalled:
                new Set<number>(
                    this.props.applicationStatus
                        .filter(i => i.keyspace_name === this.props.applicationName)
                        .filter(i => i.process_status === "INSTALLED")
                        .map(i => i.node_id)
                ),

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
        if (this.state.newlyInstalled.has(object.new_node_id)) return 'installation_node';
        else if (this.state.previouslyInstalled.has(object.new_node_id)) return 'previous_node';
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

        //let width = this.rightRef.current?.clientWidth;

        //let height = this.rightRef.current?.clientHeight;

        let width = 700;

        let height = 500;

        return (
            <Modal show={this.props.show}
                   size='xl'
                   centered
            >
                {modal}
                <Modal.Header>
                    <Modal.Title>Application {this.props.applicationName}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <AlignedDivs>
                        <Left width='35%'>
                            <h2>Topology Legend</h2>
                            <p>Previously installed nodes are <span className={'d_cyan'}>cyan</span></p>
                            <p>Nodes just installed are <span className={'d_green'}>green</span></p>
                            <p>Nodes that aren't installed are <span className={'d_currentLine'}>dark grey</span></p>
                        </Left>
                        <Right divRef={this.rightRef}>
                            <h2>Application Installation Topology</h2>
                            <PathStoreTopology width={width}
                                               height={height}
                                               deployment={this.props.deployment.filter(i => i.process_status === "DEPLOYED")}
                                               get_colour={this.getClassName}
                                               get_click={this.handleClick}/>
                        </Right>
                    </AlignedDivs>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.props.callback}>close</Button>
                </Modal.Footer>
            </Modal>
        );
    }
}