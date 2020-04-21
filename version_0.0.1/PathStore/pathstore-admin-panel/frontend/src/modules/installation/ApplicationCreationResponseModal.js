import React, {Component} from "react";
import Modal from "react-modal";

/**
 * This class is used to display the response of an application creation request
 *
 * Props:
 * show: whether or not to show the modal
 * data: response from api request
 * callback: call when user hits the close button. This allows for a single point of access control rather then both
 * the child and the parent element both controlling it
 *
 */
export default class ApplicationCreationResponseModal extends Component {

    /**
     * If the response contains a certain element we know its successful otherwise theres an error
     * @param response
     * @returns {string}
     */
    parseMessage = (response) => {

        if (Array.isArray(response)) {
            let message = "Could not create application for the following reasons:";

            for (let i = 0; i < response.length; i++)
                message += " " + (i + 1) + ". " + response[i].error;

            return message;

        } else return response.keyspace_created !== undefined ?
            "Successfully create application named: " + response.keyspace_created
            : "Could not create application for reason: " + response.error;
    };

    render() {

        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}}>
                <p>{this.parseMessage(this.props.data)}</p>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}