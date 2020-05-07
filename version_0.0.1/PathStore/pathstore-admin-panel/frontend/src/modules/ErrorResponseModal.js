import React, {Component} from "react";
import Modal from "react-modal";

export default class ErrorResponseModal extends Component {

    parseMessage = (response) => {
        let message = "The following errors occured: ";

        for (let i = 0; i < response.length; i++)
            message += " " + (i + 1) + ". " + response[i].error;

        return message;
    };

    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <p>{this.parseMessage(this.props.data)}</p>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}