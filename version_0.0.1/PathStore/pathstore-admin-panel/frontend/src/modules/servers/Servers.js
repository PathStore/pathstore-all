import ReactDOM from 'react-dom'
import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import {Button} from "react-bootstrap";
import Table from "react-bootstrap/Table";
import LoadingModal from "../LoadingModal";
import ServerCreationResponseModal from "./ServerCreationResponseModal";

/**
 * This model is used to display all available servers and to create a server
 */
export default class Servers extends Component {

    /**
     * Represents form. used to clear form after submission
     * @type {null}
     */
    messageForm = null;

    /**
     *
     * State:
     * servers: list of servers that are parsed into an object array
     * ip: denotes currently inputted IP address
     * username: denotes currently inputted IP address
     * loadingModalShow: whether to show loading modal
     * responseModalShow: whe
     *
     * @param props
     */
    constructor(props) {
        super(props);

        this.state = {
            servers: [],
            ip: "",
            username: "",
            password: "",
            loadingModalShow: false,
            responseModalShow: false,
            responseModalData: null
        }
    }


    /**
     * Query all servers and store in state
     */
    componentDidMount() {
        fetch('/api/v1/servers')
            .then(response => response.json())
            .then(response => this.setState({servers: response}))
    }


    /**
     * Update state on change of ip form value
     *
     * @param event
     */
    onIpChange = (event) => this.setState({ip: event.target.value});

    /**
     * Update state on change of username form value
     *
     * @param event
     */
    onUserNameChange = (event) => this.setState({username: event.target.value});

    /**
     * Update state on change of password form value
     *
     * @param event
     */
    onPasswordChange = (event) => this.setState({password: event.target.value});

    /**
     * Create url for form submission, show loading modal and then render response modal on completion of request
     *
     * @param event
     */
    onFormSubmit = (event) => {
        event.preventDefault();

        let url = "/api/v1/servers"
            + "?ip=" + this.state.ip
            + "&username=" + this.state.username
            + "&password=" + this.state.password;

        this.setState({loadingModalShow: true}, () => {
            fetch(url, {
                method: 'POST'
            }).then(response => response.json())
                .then(response => {
                    this.setState({
                        loadingModalShow: false,
                        responseModalShow: true,
                        responseModalData: response
                    }, () => {
                        ReactDOM.findDOMNode(this.messageForm).reset();
                        this.componentDidMount();
                    });
                });
        });
    };

    /**
     * Call back for response modal to close
     */
    callBack = () => this.setState({responseModalShow: false});

    /**
     * Render modals if applicable, display table and add server form submission
     *
     * @returns {*}
     */
    render() {

        const loadingModal = this.state.loadingModalShow ? <LoadingModal show={this.state.loadingModalShow}/> : null;

        const responseModal = this.state.responseModalShow ?
            <ServerCreationResponseModal show={this.state.responseModalShow} data={this.state.responseModalData}
                                         callback={this.callBack}/> : null;

        const tbody = [];

        for (let i = 0; i < this.state.servers.length; i++)
            tbody.push(
                <tr key={i}>
                    <td>{this.state.servers[i].server_uuid}</td>
                    <td>{this.state.servers[i].ip}</td>
                </tr>
            );

        return (
            <div>
                {loadingModal}
                {responseModal}
                <Table>
                    <thead>
                    <tr>
                        <th>Server UUID</th>
                        <th>IP</th>
                    </tr>
                    </thead>
                    <tbody>
                    {tbody}
                    </tbody>
                </Table>
                <h3>Create Server</h3>
                <Form onSubmit={this.onFormSubmit} ref={form => this.messageForm = form}>
                    <Form.Group controlId="ip">
                        <Form.Label>IP Address</Form.Label>
                        <Form.Control type="text" placeholder="ip address of server" onChange={this.onIpChange}/>
                    </Form.Group>
                    <Form.Group controlId="username">
                        <Form.Label>Username</Form.Label>
                        <Form.Control type="text" placeholder="username for login" onChange={this.onUserNameChange}/>
                    </Form.Group>
                    <Form.Group controlId="password">
                        <Form.Label>Password</Form.Label>
                        <Form.Control type="password" placeholder="password for login"
                                      onChange={this.onPasswordChange}/>
                    </Form.Group>
                    <Button variant="primary" type="submit">
                        Submit
                    </Button>
                </Form>
            </div>
        )
    }
}