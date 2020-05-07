import ReactDOM from 'react-dom'
import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import {Button} from "react-bootstrap";
import Table from "react-bootstrap/Table";
import LoadingModal from "../LoadingModal";
import ServerCreationResponseModal from "./ServerCreationResponseModal";
import {webHandler} from "../Utils";
import ErrorResponseModal from "../ErrorResponseModal";

export default class Servers extends Component {

    constructor(props) {
        super(props);

        this.state = {
            loadingModalShow: false,
            responseModalShow: false,
            responseModalData: null,
            responseModalError: false
        }
    }

    onFormSubmit = (event) => {
        event.preventDefault();

        const ip = event.target.elements.ip.value.trim();
        const username = event.target.elements.username.value.trim();
        const password = event.target.elements.password.value.trim();
        const name = event.target.elements.name.value.trim();

        let url = "/api/v1/servers"
            + "?ip=" + ip
            + "&username=" + username
            + "&password=" + password
            + "&name=" + name;

        this.setState({loadingModalShow: true}, () => {
            fetch(url, {
                method: 'POST'
            }).then(webHandler)
                .then(response => this.setState({
                        loadingModalShow: false,
                        responseModalShow: true,
                        responseModalData: response,
                        responseModalError: false
                    }, () => {
                        ReactDOM.findDOMNode(this.messageForm).reset();
                        this.props.callback();
                    })
                ).catch(response => this.setState({
                    loadingModalShow: false,
                    responseModalShow: true,
                    responseModalData: response,
                    responseModalError: true
                })
            );
        });
    };

    callBack = () => this.setState({responseModalShow: false});

    render() {

        const loadingModal = this.state.loadingModalShow ? <LoadingModal show={this.state.loadingModalShow}/> : null;

        const responseModal =
            this.state.responseModalShow ?
                this.state.responseModalError ?
                    <ErrorResponseModal show={this.state.responseModalShow}
                                        data={this.state.responseModalData}
                                        callback={this.callBack}/>
                    :
                    <ServerCreationResponseModal show={this.state.responseModalShow}
                                                 data={this.state.responseModalData}
                                                 callback={this.callBack}/>
                : null;

        const tbody = [];

        for (let i = 0; i < this.props.servers.length; i++)
            tbody.push(
                <tr key={i}>
                    <td>{this.props.servers[i].server_uuid}</td>
                    <td>{this.props.servers[i].ip}</td>
                    <td>{this.props.servers[i].name}</td>
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
                        <th>Server Name</th>
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
                        <Form.Control type="text" placeholder="ip address of server"/>
                    </Form.Group>
                    <Form.Group controlId="username">
                        <Form.Label>Username</Form.Label>
                        <Form.Control type="text" placeholder="username for login"/>
                    </Form.Group>
                    <Form.Group controlId="password">
                        <Form.Label>Password</Form.Label>
                        <Form.Control type="password" placeholder="password for login"/>
                    </Form.Group>
                    <Form.Group controlId="name">
                        <Form.Label>Server Name</Form.Label>
                        <Form.Control type="text" placeholder="Name to identify server (This is only to aid you)"/>
                    </Form.Group>
                    <Button variant="primary" type="submit">
                        Submit
                    </Button>
                </Form>
            </div>
        )
    }
}