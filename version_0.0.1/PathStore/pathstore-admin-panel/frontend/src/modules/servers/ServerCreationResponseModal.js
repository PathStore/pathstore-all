import React, {Component} from "react";
import Modal from "react-modal";

/**
 * This component is used when an add server request as gone through successfully
 *
 * Props:
 * show: whether to show the modal or not
 * data: data about the server that was created
 * callback: callback to close modal on completion
 */
export default class ServerCreationResponseModal extends Component {

    /**
     * Inform the user what the new server's uuid is and what the ip is
     *
     * @param response
     * @returns {string}
     */
    parseMessage = (response) => "Successfully create server with uuid: " + response.server_uuid + " and ip: " + response.ip;

    /**
     * Render modal and response message
     * l
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <p>{this.parseMessage(this.props.data)}</p>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}