import React, {Component} from "react";
import Modal from "react-modal";

/**
 * TODO: Show some spinning image or something
 *
 * This is a simple loading model to be shown when the user has made a request that may take some time.
 *
 * An example of this is when the user creates an application as the general time this takes is 9 secs.
 */
export default class LoadingModel extends Component {
    /**
     * Show a modal with a loading image TODO
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show}>
                <p>Loading</p>
            </Modal>
        );
    }
}