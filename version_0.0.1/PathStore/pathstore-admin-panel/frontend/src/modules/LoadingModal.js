import React, {Component} from "react";
import Modal from "react-modal";

export default class LoadingModal extends Component {

    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <p>Loading</p>
            </Modal>
        );
    }
}