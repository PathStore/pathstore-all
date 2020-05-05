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
     * server: html info on the server info that, that node has
     * isOpen: denotes whether the modal is open or not.
     */
    constructor(props) {
        super(props);
        this.state = {
            message: [],
            server: null,
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
     * Update components data every 1 sec
     */
    componentDidMount() {
        this.loadData();

        this.setState({timer: setInterval(this.loadData, 1000)});
    }

    /**
     * Clear interval every second
     */
    componentWillUnmount() {
        clearInterval(this.state.timer);
    }

    /**
     * Query all applications and parse to only the nodeid that is currently selected
     */
    loadData = () => {
        fetch('/api/v1/application_management')
            .then(response => response.json())
            .then(message => {
                const filtered = message.filter(i => i.nodeid === this.props.node);

                let messages = [];

                for (let i = 0; i < filtered.length; i++)
                    messages.push(this.createMessageObject(filtered[i]));


                this.setState({
                    message: this.formatClickEvent(messages),
                    server: this.formatServer(this.props.topology, this.props.servers),
                    currentMessage: this.props.node
                });
            });
    };

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
     * Creates the server information section of the node information.
     *
     * First find the serverUUID from the topology information. Then filter all servers to get the server object needed to display the information to the user
     *
     * @param topology
     * @param servers
     * @returns {*}
     */
    formatServer = (topology, servers) => {

        const deployObject = topology.filter(i => i.id === this.props.node);

        console.log(servers.length);

        const object = servers.filter(i => i.server_uuid === deployObject[0].serverUUID);

        return <div>
            <p>Server Information</p>
            <p>UUID: {object[0].server_uuid}</p>
            <p>IP: {object[0].ip}</p>
            <p>Username: {object[0].username}</p>
            <p>Name: {object[0].name}</p>
        </div>;
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
                {this.state.server}
                <Table>{this.state.message}</Table>
                <button onClick={this.closeModal}>close</button>
            </Modal>
        );
    }
}