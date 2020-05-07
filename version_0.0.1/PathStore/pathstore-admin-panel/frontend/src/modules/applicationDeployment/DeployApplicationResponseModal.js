import React, {Component} from "react";
import Modal from "react-modal";
import '../../Circles.css';
import PathStoreTopology from "../PathStoreTopology";
import {contains} from "../Utils";

export default class DeployApplicationResponseModal extends Component {

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

    getClassName = (object) => {
        if (contains(this.state.newlyInstalled, object.new_node_id)) return 'installation_node';
        else if (contains(this.state.previouslyInstalled, object.new_node_id)) return 'previous_node';
        else return 'not_set_node';
    };

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