import React, {Component} from "react";
import NodeInfoModal from "../NodeInfoModal";
import PathStoreTopology from "../PathStoreTopology";

/**
 * This component is used to give a visual of the network topology. This topology will display all nodes regardless
 * of their stage in deployment and will be coloured based on their stage.
 *
 * Props:
 * topology: list of deployment objects from api
 * servers: list of server objects from api
 * applicationStatus: list of node's application status from api
 */
export default class ViewTopology extends Component {

    /**
     * State:
     * infoModalData: node number to give to info modal
     * infoModalShow: whether to show the info modal or not
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            infoModalData: null,
            infoModalShow: false
        };
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
    getClassName = (object) => {
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
     * @param node
     */
    handleClick = (event, node) => this.setState({infoModalData: node, infoModalShow: true});

    /**
     * Callback function for info modal to close itself
     */
    callback = () => this.setState({infoModalShow: false});

    /**
     * First determine if an info modal needs to be shown
     *
     * Then display the topology to the user
     *
     * @returns {*}
     */
    render() {
        const modal = this.state.infoModalShow ?
            <NodeInfoModal node={this.state.infoModalData}
                           show={this.state.infoModalShow}
                           topology={this.props.topology}
                           applicationStatus={this.props.applicationStatus}
                           servers={this.props.servers}
                           callback={this.callback}/> : null;

        return (
            <div>
                <p>Click on a node to view its current applications</p>
                <PathStoreTopology topology={this.props.topology}
                                   get_colour={this.getClassName}
                                   get_click={this.handleClick}/>
                {modal}
            </div>
        );
    }
}