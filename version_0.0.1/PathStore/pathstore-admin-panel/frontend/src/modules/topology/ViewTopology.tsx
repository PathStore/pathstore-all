import React, {Component, RefObject} from 'react';
import {ApplicationStatus, AvailableLogDates, Deployment, Server} from "../../utilities/ApiDeclarations";
import {PathStoreTopology} from "../PathStoreTopology";
import NodeInfoModal from "../NodeInfoModal";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";

/**
 * Properties definition for {@link ViewTopology}
 */
interface ViewTopologyProps {

    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * List of node application status from api
     */
    readonly applicationStatus: ApplicationStatus[]

    /**
     * List of available dates for each log
     */
    readonly availableLogDates: AvailableLogDates[]

    /**
     * Force refresh other props components
     */
    readonly forceRefresh: () => void
}

/**
 * State definition for {@link ViewTopology}
 */
interface ViewTopologyState {

    /**
     * Whether to show the info modal or not
     */
    readonly infoModalShow: boolean

    /**
     * What node id was clicked
     */
    readonly infoModalData: number
}

/**
 * This component is used to give a visual of the network topology. This topology will display all nodes regardless
 * of their stage in deployment and will be coloured based on their stage.
 */
export default class ViewTopology extends Component<ViewTopologyProps, ViewTopologyState> {

    /**
     * Reference to the right div to determine the height and width
     */
    private rightRef: RefObject<HTMLDivElement> = React.createRef<HTMLDivElement>();

    /**
     * Initializes props and state
     *
     * @param props
     */
    constructor(props: ViewTopologyProps) {
        super(props);

        this.state = {
            infoModalShow: false,
            infoModalData: -1
        }
    }

    /**
     * Pathstore topology colour function. This function colours nodes based on their deployment status
     *
     * Waiting is orange (waiting_node)
     * Installing is blue (installing_node)
     * Failed is red (uninstalled_node)
     * Deployed is green (installed_node)
     *
     * @param object
     * @returns {string}
     */
    getClassName = (object: Deployment): string => {
        switch (object.process_status) {
            case "WAITING_DEPLOYMENT":
                return 'waiting_node';
            case "DEPLOYING":
            case "PROCESSING_DEPLOYING":
                return 'installing_node';
            case "FAILED":
                return 'uninstalled_node';
            case "WAITING_REMOVAL":
                return 'waiting_removal_node';
            case "REMOVING":
            case "PROCESSING_REMOVING":
                return 'removing_node';
            default:
                return 'installed_node';
        }
    };

    /**
     * Click function for pathstore topology to render the info modal on click
     *
     * @param event
     * @param node node id
     */
    handleClick = (event: any, node: number): void => this.setState({infoModalData: node, infoModalShow: true});

    /**
     * Callback function for info modal to close itself
     */
    callback = (): void => this.setState({infoModalData: -1, infoModalShow: false});

    /**
     * First determine if an info modal needs to be shown
     *
     * Then display the topology to the user
     *
     * @returns {*}
     */
    render() {

        const modal =
            this.state.infoModalShow ?
                <NodeInfoModal node={this.state.infoModalData}
                               show={this.state.infoModalShow}
                               deployment={this.props.deployment}
                               applicationStatus={this.props.applicationStatus}
                               servers={this.props.servers}
                               availableLogDates={this.props.availableLogDates}
                               callback={this.callback}
                               forceRefresh={this.props.forceRefresh}/>
                : null;

        let width = this.rightRef.current?.clientWidth;

        //let height = this.rightRef.current?.clientHeight;

        if (width === undefined) width = 500;

        let height = 500;

        return (
            <AlignedDivs>
                {modal}
                <Left width='25%'>
                    <h2>Topology Legend</h2>
                    <br/>
                    <p>Waiting Deployment: <span className={'d_yellow'}>Yellow</span></p>
                    <p>Deploying / Processing Deploying: <span className={'d_cyan'}>Cyan</span></p>
                    <p>Deployed: <span className={'d_green'}>Green</span></p>
                    <p>Failed: <span className={'d_red'}>Red</span></p>
                    <br/>
                    <p>Waiting Removal: <span className={'d_pink'}>Pink</span></p>
                    <p>Removing / Processing Removing: <span className={'d_purple'}>Purple</span></p>
                </Left>
                <Right divRef={this.rightRef}>
                    <h2>Topology</h2>
                    <p>Click on a node to view its current applications</p>
                    <PathStoreTopology width={width}
                                       height={height}
                                       deployment={this.props.deployment}
                                       get_colour={this.getClassName}
                                       get_click={this.handleClick}/>
                </Right>
            </AlignedDivs>
        )
    }
}