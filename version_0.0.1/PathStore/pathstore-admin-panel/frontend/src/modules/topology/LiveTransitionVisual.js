import React, {Component} from "react";
import Modal from "react-modal";
import Button from "react-bootstrap/Button";
import PathStoreTopology from "../PathStoreTopology";
import {contains} from "../Utils";

export default class LiveTransitionVisual extends Component {

    constructor(props) {
        super(props);
        this.state = {
            showModal: false,
            dataModal: null
        }
    }

    onButtonClick = (event) => this.setState({showModal: true, dataModal: event.target.value});

    callBack = () => this.setState({showModal: false});

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
                                           topology={this.props.topology}
                                           applicationStatus={this.props.applicationStatus}
                                           keyspace={this.state.dataModal}
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

class LiveTransitionVisualModal extends Component {

    constructor(props) {
        super(props);
        this.state = {
            waiting: [],
            installing: [],
            installed: []
        };
    }

    static getDerivedStateFromProps(nextProps, prevState) {
        return {
            waiting:
                nextProps.applicationStatus
                    .filter(i => i.keyspace_name === nextProps.keyspace)
                    .filter(i => i.process_status === "WAITING_INSTALL")
                    .map(i => parseInt(i.nodeid)),
            installing:
                nextProps.applicationStatus
                    .filter(i => i.keyspace_name === nextProps.keyspace)
                    .filter(i => i.process_status === "INSTALLING")
                    .map(i => parseInt(i.nodeid)),
            installed:
                nextProps.applicationStatus
                    .filter(i => i.keyspace_name === nextProps.keyspace)
                    .filter(i => i.process_status === "INSTALLED")
                    .map(i => parseInt(i.nodeid))
        }
    }


    getClassName = (object) => {
        const name = object.new_node_id;

        if (contains(this.state.installed, name)) return 'installed_node';
        else if (contains(this.state.installing, name)) return 'installing_node';
        else if (contains(this.state.waiting, name)) return 'waiting_node';
        else return 'not_set_node';
    };

    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}}>
                <div>
                    <p>Live updates for: {this.props.keyspace}</p>
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