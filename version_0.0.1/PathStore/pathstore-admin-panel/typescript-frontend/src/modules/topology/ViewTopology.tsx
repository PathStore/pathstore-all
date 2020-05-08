import React, {Component} from 'react';
import {ApplicationStatus, Deployment, Server} from "../../utilities/ApiDeclarations";
import {PathStoreTopology} from "../PathStoreTopology";
import NodeInfoModal from "../NodeInfoModal";

interface ViewTopologyProps {
    readonly deployment: Deployment[]
    readonly servers: Server[]
    readonly applicationStatus: ApplicationStatus[]
}

interface ViewTopologyState {
    readonly infoModalShow: boolean
    readonly infoModalData: number
}

export default class ViewTopology extends Component<ViewTopologyProps, ViewTopologyState> {

    constructor(props: ViewTopologyProps) {
        super(props);

        this.state = {
            infoModalShow: false,
            infoModalData: -1
        }
    }

    getClassName = (object: Deployment) => {
        switch (object.process_status) {
            case "WAITING_DEPLOYMENT":
                return 'waiting_node';
            case "DEPLOYING":
                return 'installing_node';
            case "FAILED":
                return 'uninstalled_node';
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
    handleClick = (event: any, node: number) => this.setState({infoModalData: node, infoModalShow: true});

    /**
     * Callback function for info modal to close itself
     */
    callback = () => this.setState({infoModalData: -1, infoModalShow: false});

    render() {

        const modal =
            this.state.infoModalShow ?
                <NodeInfoModal node={this.state.infoModalData}
                               show={this.state.infoModalShow}
                               deployment={this.props.deployment}
                               applicationStatus={this.props.applicationStatus}
                               servers={this.props.servers}
                               callback={this.callback}/>
                : null;

        return (
            <div>
                <p>Click on a node to view its current applications</p>
                <PathStoreTopology deployment={this.props.deployment}
                                   get_colour={this.getClassName}
                                   get_click={this.handleClick}/>
                {modal}
            </div>
        )
    }
}