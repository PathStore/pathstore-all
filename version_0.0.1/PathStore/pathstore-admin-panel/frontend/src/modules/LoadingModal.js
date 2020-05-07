import React, {Component} from "react";
import Modal from "react-modal";

/**
 * TODO: Make this more interesting
 *
 * Simple modal to display to the user that their request has been submitted and it is currently going on
 */
export default class LoadingModal extends Component {

    /**
     * Inform user that their response is loading
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <p>Loading</p>
            </Modal>
        );
    }
}