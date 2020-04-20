import React, {Component} from "react";
import Modal from "react-modal";
import Table from "react-bootstrap/Table";

/**
 * This class displays the current applications installed on a specific node.
 *
 * The rendering of this modal is triggered by clicking on a node in the tree of the ViewTopology Class
 */
export default class NodeInfoModal extends Component {

    /**
     * State:
     *
     * message: queried from application_management and parsed into a table
     * isOpen: denotes whether the modal is open or not.
     */
    constructor(props) {
        super(props);
        this.state = {
            message: [],
            isOpen: true
        };
    }

    /**
     * Used to remount component if the view topology says to
     */
    componentWillReceiveProps(props, nextContext) {
        if (props.refresh !== this.props.refresh)
            this.setState({isOpen: true, message: []}, () => this.componentDidMount());
    }

    /**
     * Query all applications and filter for this node
     * Create readable objects for each row in the json array
     * Then create the table for that readable list of applications
     */
    componentDidMount() {
        fetch('/api/v1/application_management')
            .then(response => response.json())
            .then(message => {
                const filtered = message.filter(i => i.nodeid === this.props.node);

                let messages = [];

                for (let i = 0; i < filtered.length; i++)
                    messages.push(this.createMessageObject(filtered[i]));


                this.setState({message: this.formatClickEvent(messages), currentMessage: this.props.node});
            });
    }

    /**
     * Create readable object for passed data
     *
     * @param object
     * @returns {{jobUUID: *, application: *, waiting: *, nodeid: *, status: *}}
     */
    createMessageObject = (object) => {
        return {
            nodeid: object.nodeid,
            application: object.keyspace_name,
            status: object.process_status,
            waiting: object.wait_for,
            jobUUID: object.process_uuid
        }
    };

    /**
     * Format the states into a table
     *
     * @param messages
     * @returns {[]}
     */
    formatClickEvent = (messages) => {

        let response = [];

        response.push(
            <thead key={0}>
            <tr>
                <th>Nodeid</th>
                <th>Application</th>
                <th>Status</th>
                <th>Waiting</th>
                <th>Job UUID</th>
            </tr>
            </thead>
        );

        let body = [];

        for (let i = 0; i < messages.length; i++) {

            let currentObject = messages[i];

            body.push(
                <tr>
                    <td>{currentObject.nodeid}</td>
                    <td>{currentObject.application}</td>
                    <td>{currentObject.status}</td>
                    <td>{currentObject.waiting}</td>
                    <td>{currentObject.jobUUID}</td>
                </tr>)
        }

        response.push(
            <tbody key={1}>
            {body}
            </tbody>
        );

        return response;
    };

    /**
     * This function is called when the user clicks the close button on the modal
     */
    closeModal = () => {
        this.setState({isOpen: false});
    };

    render() {
        return (
            <Modal isOpen={this.state.isOpen} style={{overlay: {zIndex: 1}}}>
                <Table>{this.state.message}</Table>
                <button onClick={this.closeModal}>close</button>
            </Modal>
        );
    }
}