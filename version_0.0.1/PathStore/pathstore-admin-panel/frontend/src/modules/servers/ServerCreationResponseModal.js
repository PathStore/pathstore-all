import React, {Component} from "react";
import Modal from "react-modal";

export default class ServerCreationResponseModal extends Component {

    parseMessage = (response) => "Successfully create server with uuid: " + response.server_uuid + " and ip: " + response.ip;

    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <p>{this.parseMessage(this.props.data)}</p>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}