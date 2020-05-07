import React, {Component} from "react";
import Modal from "react-modal";
import '../../Circles.css';
import PathStoreTopology from "../PathStoreTopology";
import {contains} from "../Utils";

/**
 * This component is loaded by DeployApplication on a successful POST request to the api.
 *
 * Props:
 * data: response data from api (Records that got written to table)
 * show: whether to show the modal or not
 * applicationName: name of the application that got deployed
 * topology: list of deployment objects from api
 * applicationStatus: list of node application statuses from api
 * callback: callback function to close modal
 *
 * Note: The reason for not updating the component when the props change is because even if the props change
 * technically their request remains the same.
 */
export default class DeployApplicationResponseModal extends Component {

    /**
     * State:
     * newlyInstalled: list of node id's that where installed via the users request
     * previouslyInstalled: list of node id's that already had this keyspace installed
     *
     * @param props
     */
    constructor(props) {
        super(props);

        this.state = {
            newlyInstalled:
                this.props.data
                    .map(i => parseInt(i.nodeid)),

            previouslyInstalled:
                this.props.applicationStatus
                    .filter(i => i.keyspace_name === this.props.applicationName)
                    .filter(i => i.process_status === "INSTALLED")
                    .map(i => parseInt(i.nodeid))
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
    getClassName = (object) => {
        if (contains(this.state.newlyInstalled, object.new_node_id)) return 'installation_node';
        else if (contains(this.state.previouslyInstalled, object.new_node_id)) return 'previous_node';
        else return 'not_set_node';
    };

    /**
     * Render modal with a description of the colours and their meanings also render the topology with appropriate colours
     *
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <div>
                    <p>Application {this.props.applicationName}</p>
                    <p>Blue nodes are nodes that have the application previous installed</p>
                    <p>Green nodes are nodes that you just installed the application on</p>
                    <p>Black nodes are nodes that have not be installed on</p>
                </div>
                <div>
                    <PathStoreTopology topology={this.props.topology.filter(i => i.process_status === "DEPLOYED")}
                                       get_colour={this.getClassName}/>
                </div>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}