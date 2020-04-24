import ReactDOM from 'react-dom'
import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import {Button} from "react-bootstrap";

export default class AddServer extends Component {

    messageForm = null;

    constructor(props) {
        super(props);

        this.state = {
            ip: "",
            username: "",
            password: ""
        }
    }

    onIpChange = (event) => this.setState({ip: event.target.value});

    onUserNameChange = (event) => this.setState({username: event.target.value});

    onPasswordChange = (event) => this.setState({password: event.target.value});

    onFormSubmit = (event) => {
        event.preventDefault();

        let url = "/api/v1/servers"
            + "?ip=" + this.state.ip
            + "&username=" + this.state.username
            + "&password=" + this.state.password;

        fetch(url, {
            method: 'POST'
        }).then(response => response.json())
            .then(response => {
                ReactDOM.findDOMNode(this.messageForm).reset();
                alert(response);
            });
    };

    render() {
        return (
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
        )
    }
}