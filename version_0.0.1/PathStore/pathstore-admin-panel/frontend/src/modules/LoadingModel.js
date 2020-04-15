import React, {Component} from "react";
import Modal from "react-modal";

/**
 * TODO: Show some spinning image or something
 */
export default class LoadingModel extends Component {
    constructor(props) {
        super(props);
        this.state = {
            show: props.show
        }
    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.show !== props.show) {
            this.setState({show: props.show});
        }
    }

    render() {
        return (
            <Modal isOpen={this.state.show}>
                <p>Loading</p>
            </Modal>
        );
    }
}