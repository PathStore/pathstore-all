import React, {Component} from "react";
import Modal from "react-modal";

/**
 * This component is used to parse and display an error message generated from the api (error code 400)
 *
 * Props:
 * show: whether or not to show the modal
 * data: error response from api in a json array
 * callback: callback function to close modal
 */
export default class ErrorResponseModal extends Component {

    /**
     * Parse json array into a string to inform the user why their request failed
     *
     * @param response
     * @returns {string}
     */
    parseMessage = (response) => {
        let message = "The following errors occured: ";

        for (let i = 0; i < response.length; i++)
            message += " " + (i + 1) + ". " + response[i].error;

        return message;
    };

    /**
     * Render modal with error message(s)
     *
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