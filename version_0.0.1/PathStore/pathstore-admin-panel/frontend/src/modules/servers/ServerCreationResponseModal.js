import React, {Component} from "react";
import Modal from "react-modal";

/**
 * Props:
 * show: whether to show or not to show
 * data: response from api request
 * callback: to close modal
 */
export default class ServerCreationResponseModal extends Component {

    /**
     * If the response contains a certain element we know its successful otherwise theres an error
     * @param response
     * @returns {string}
     */
    parseMessage = (response) => {
        if (Array.isArray(response)) {
            let message = "Could not create server for the following reasons:";

            for (let i = 0; i < response.length; i++)
                message += " " + (i + 1) + ". " + response[i].error;

            return message;

        } else return "Successfully create server with uuid: " + response.server_uuid + " and ip: " + response.ip;
    };

    /**
     * Render modal with parsed data
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}}>
                <p>{this.parseMessage(this.props.data)}</p>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}