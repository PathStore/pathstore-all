import React, {Component} from "react";
import {Button} from "react-bootstrap";
import Modal from "react-modal";
import Servers from "../servers/Servers";
import PathStoreTopology from "../PathStoreTopology";
import NodeDeploymentAdditionForm from "./NodeDeploymentAdditionForm";


export default class NodeDeployment extends Component {

    constructor(props) {
        super(props);

        this.state = {
            show: false
        };
    }

    showModal = () => this.setState({show: true});

    callBack = () => this.setState({show: false});

    render() {
        return (
            <div>
                {this.state.show ? <NodeDeploymentModal topology={this.props.topology}
                                                        show={this.state.show}
                                                        servers={this.props.servers}
                                                        forceRefresh={this.props.forceRefresh}
                                                        callback={this.callBack}/> : null}

                <Button onClick={this.showModal}>Deploy Additional Nodes to Network</Button>
            </div>
        );
    }
}

class NodeDeploymentModal extends Component {

    constructor(props) {
        super(props);

        this.state = {
            topology: props.topology.slice(),
            updates: []
        }
    }

    submit = () => {

        const formattedUpdates = [];

        for (let i = 0; i < this.state.updates.length; i++) {
            formattedUpdates.push({
                parentId: this.state.updates[i].parent_node_id,
                newNodeId: this.state.updates[i].new_node_id,
                serverUUID: this.state.updates[i].server_uuid
            })
        }

        if (formattedUpdates.length <= 0) {
            alert("You have not made any changes to the network");
            return;
        }

        fetch('/api/v1/deployment', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                records: formattedUpdates
            })
        })
            .then(response => response.json())
            .then(response => {
                alert("Success " + JSON.stringify(response));
            }).catch(response => {
            alert("ERROR: " + JSON.stringify(response));
        });
    };

    isHypothetical = (object) => {
        switch (object.process_status) {
            case "WAITING_DEPLOYMENT":
            case "DEPLOYING":
                return 'hypothetical';
            default:
                return 'not_set_node';
        }
    };

    serverUpdateCallBack = () => this.props.forceRefresh();

    onClose = () => this.setState({topology: this.props.topology}, this.props.callback);

    render() {

        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <div>
                    <PathStoreTopology topology={this.state.topology}
                                       get_colour={this.isHypothetical}/>
                </div>

                <NodeDeploymentAdditionForm topology={this.state.topology}
                                            updates={this.state.updates}
                                            servers={this.props.servers}/>

                <div>
                    <Servers servers={this.props.servers}
                             callback={this.serverUpdateCallBack}/>
                </div>

                <Button onClick={this.submit}>Submit changes</Button>
                <Button onClick={this.onClose}>Close</Button>
            </Modal>
        );
    }
}