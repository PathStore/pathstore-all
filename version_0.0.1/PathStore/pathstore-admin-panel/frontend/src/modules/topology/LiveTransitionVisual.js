import React, {Component} from "react";
import Modal from "react-modal";
import Button from "react-bootstrap/Button";
import PathStoreTopology from "../PathStoreTopology";
import {contains} from "../Utils";

/**
 * This component is used to display buttons for each keyspace that you can watch transition live and visually
 *
 * Props:
 * applications: list of application objects from api
 * applicationStatus: list of node's application status objects from api
 * topology: list of deployment objects from api
 */
export default class LiveTransitionVisual extends Component {

    /**
     * State:
     * showModal: whether to show the transition modal or not
     * dataModal: what data to give to the modal (the keyspace to display)
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            showModal: false,
            dataModal: null
        }
    }

    /**
     * On button click display the modal and give the modal which keyspace to display
     *
     * @param event
     */
    onButtonClick = (event) => this.setState({showModal: true, dataModal: event.target.value});

    /**
     * Callback for modal to close itself
     */
    callBack = () => this.setState({showModal: false});

    /**
     * First gather all buttons based on the number of applications installed on the network
     *
     * If there are no applications inform the user of this
     *
     * Secondly figure out if you need to load a modal
     *
     * Finally load the potential modal and all buttons or information message to user
     *
     * @returns {*}
     */
    render() {

        const buttons = [];

        for (let i = 0; i < this.props.applications.length; i++)
            buttons.push(
                <Button key={i}
                        variant="primary"
                        value={this.props.applications[i].keyspace_name}
                        onClick={this.onButtonClick}>{this.props.applications[i].keyspace_name}
                </Button>
            );

        if (buttons.length === 0) buttons.push(<p key={0}>No Applications are installed on the system</p>);

        const modal =
            this.state.showModal ?
                <LiveTransitionVisualModal show={this.state.showModal}
                                           application={this.state.dataModal}
                                           topology={this.props.topology}
                                           applicationStatus={this.props.applicationStatus}
                                           callback={this.callBack}/>
                : null;

        return (
            <div>
                {modal}
                {buttons}
            </div>
        );
    }
}

/**
 * This component is used to show a visual of the network nodes transitioning through the various states for an
 * application deployment
 *
 * Props:
 * show: whether to show the modal or not
 * application: what application to filter by
 * topology: list of deployment objects from api
 * applicationStatus: list node application status objects from api
 * callback: callback function to close modal
 */
class LiveTransitionVisualModal extends Component {

    /**
     * State: (All based on which keyspace was passed via application prop)
     * waiting: list of node id's that are waiting
     * installing: list of node id's that are installing
     * installed: list of node id's that are installed
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            waiting: [],
            installing: [],
            installed: []
        };
    }

    /**
     * Since we have internal state data based on a prop we need to derive new states everytime the applicationStatus
     * prop updates
     *
     * This function filters all applicationStatus data in 3 different ways. This is needed to colour the nodes based
     * on their current process status
     *
     * @param nextProps
     * @param prevState
     * @returns {{installed: number[], waiting: number[], installing: number[]}}
     */
    static getDerivedStateFromProps(nextProps, prevState) {
        return {
            waiting:
                nextProps.applicationStatus
                    .filter(i => i.keyspace_name === nextProps.application)
                    .filter(i => i.process_status === "WAITING_INSTALL")
                    .map(i => parseInt(i.nodeid)),
            installing:
                nextProps.applicationStatus
                    .filter(i => i.keyspace_name === nextProps.application)
                    .filter(i => i.process_status === "INSTALLING")
                    .map(i => parseInt(i.nodeid)),
            installed:
                nextProps.applicationStatus
                    .filter(i => i.keyspace_name === nextProps.application)
                    .filter(i => i.process_status === "INSTALLED")
                    .map(i => parseInt(i.nodeid))
        }
    }


    /**
     * Function for pathstore topology to colour nodes based on their process status
     *
     * Installed nodes are green (installed_node)
     * Installing nodes are blue (installing_node)
     * Waiting nodes are orange (waiting_node)
     *
     * @param object
     * @returns {string}
     */
    getClassName = (object) => {
        const name = object.new_node_id;

        if (contains(this.state.installed, name)) return 'installed_node';
        else if (contains(this.state.installing, name)) return 'installing_node';
        else if (contains(this.state.waiting, name)) return 'waiting_node';
        else return 'not_set_node';
    };

    /**
     * Load colour description to user
     *
     * Load topology filtered that each node is deployed
     *
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <div>
                    <p>Live updates for: {this.props.application}</p>
                    <p>Nodes installed are in green</p>
                    <p>Nodes installing are in blue</p>
                    <p>Nodes waiting are in orange</p>
                    <p>Nodes not set are black</p>
                    <PathStoreTopology topology={this.props.topology.filter(i => i.process_status === "DEPLOYED")}
                                       get_colour={this.getClassName}/>
                </div>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        )
    }
}