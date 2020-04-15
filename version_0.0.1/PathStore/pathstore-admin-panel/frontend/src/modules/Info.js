import React, {Component} from "react";
import Modal from "react-modal";
import Table from "react-bootstrap/Table";

export default class Info extends Component {
    constructor(props) {
        super(props);
        this.state = {
            message: [],
            isOpen: true
        };
    }

    componentWillReceiveProps(props, nextContext) {
        if (props.refresh !== this.props.refresh)
            this.setState({isOpen: true, message: []}, () => this.componentDidMount());
    }

    componentDidMount() {
        fetch('/api/v1/application_management')
            .then(response => response.json())
            .then(message => {
                const filtered = message.filter(i => i.nodeid === this.props.node);

                let messages = [];

                for (let i = 0; i < filtered.length; i++)
                    messages.push(this.createMessageObject(filtered[i]));


                this.setState({message: this.formatClickEven(messages), currentMessage: this.props.node});
            });
    }

    createMessageObject = (object) => {
        return {
            nodeid: object.nodeid,
            application: object.keyspace_name,
            status: object.process_status,
            waiting: object.wait_for,
            jobUUID: object.process_uuid
        }
    };

    formatClickEven = (messages) => {

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

    closeModal = () => {
        this.setState({isOpen: false});
    };

    render() {
        return (
            <Modal isOpen={this.state.isOpen}>
                <Table>{this.state.message}</Table>
                <button onClick={this.closeModal}>close</button>
            </Modal>
        );
    }
}