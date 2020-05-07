import React, {Component} from "react";
import Modal from "react-modal";

/**
 * This component is used to display
 *
 * Props:
 * show: whether to show the modal or not
 * data: response data from api giving the keyspace name successfully created
 * callback: callback function to close modal
 */
export default class ApplicationCreationResponseModal extends Component {

    /**
     * Let the user know the keyspace that passed was successfully created
     *
     * @param response
     * @returns {string}
     */
    parseMessage = (response) => "Successfully create application named: " + response.keyspace_created;

    /**
     * Load Modal and show the success message to the user
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