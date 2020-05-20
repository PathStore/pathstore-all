import React, {Component} from "react";
import {Button, Form} from "react-bootstrap";
import {Server} from "../../../utilities/ApiDeclarations";

/**
 * Properties definition for {@link ServerForm}
 */
interface ServerFormProperties {
    /**
     * Callback function to call if the user input is valid.
     */
    readonly onFormSubmitCallback: (
        ip: string | undefined,
        username: string | undefined,
        password: string | undefined,
        ssh_port: number | undefined,
        rmi_port: number | undefined,
        name: string | undefined,
        clearForm: () => void
    ) => void;

    /**
     * Optional server option to have default values for the form (used for updates)
     */
    readonly server?: Server | undefined;
}

/**
 * State definition for {@link ServerForm}
 */
interface ServerFormState {
    /**
     * Ip of server
     */
    readonly ip: string | undefined

    /**
     * Username for server
     */
    readonly username: string | undefined

    /**
     * Passed for server (Must be re-entered on modification)
     */
    readonly password: string | undefined

    /**
     * ssh port for server
     */
    readonly ssh_port: number | undefined

    /**
     * Rmi port for server
     */
    readonly rmi_port: number | undefined

    /**
     * Name of server
     */
    readonly name: string | undefined
}

/**
 * This class is used to represent a server update / addition form.
 *
 * Used by:
 * @see AddServers
 * @see ModifyServerModal
 */
export default class ServerForm extends Component<ServerFormProperties, ServerFormState> {
    /**
     * Initialize props and state
     *
     * @param props
     */
    constructor(props: ServerFormProperties) {
        super(props);

        this.state = {
            ip: this.props?.server?.ip,
            username: this.props?.server?.username,
            password: "",
            ssh_port: this.props?.server?.ssh_port,
            rmi_port: this.props?.server?.rmi_port,
            name: this.props?.server?.name
        }
    }

    /**
     * Sanitize input to make sure there is some form of input. Then call the callback function to handle
     * the submission of the form
     *
     * @param event
     */
    onFormSubmit = (event: any): void => {
        event.preventDefault();

        const ip = this.state.ip;
        const username = this.state.username;
        const password = this.state.password;
        const name = this.state.name;

        if (ip === undefined || username === undefined || password === undefined || name === undefined ||
            ip === "" || username === "" || password === "" || name === "") {
            alert("You must final in the ip, username, password and name boxes");
            return;
        }

        this.props.onFormSubmitCallback(
            this.state.ip,
            this.state.username,
            this.state.password,
            this.state.ssh_port,
            this.state.rmi_port,
            this.state.name,
            this.clearForm
        );
    };

    /**
     * This function is used to allow the parent component to clear the form
     */
    clearForm = () =>
        this.setState({
            ip: "",
            username: "",
            password: "",
            ssh_port: 22,
            rmi_port: 1099,
            name: ""
        });

    /**
     * Generic on change function to update state information
     *
     * @param event event from control form
     */
    onChange = (event: any): void =>
        this.setState({[event.target.name]: event.target.value} as ServerFormState);

    /**
     * Renders the form
     */
    render() {
        return <Form onSubmit={this.onFormSubmit}>
            <Form.Group controlId="ip">
                <Form.Label>IP Address</Form.Label>
                <Form.Control type="text" name="ip" onChange={this.onChange} value={this.state.ip}/>
            </Form.Group>
            <Form.Group controlId="username">
                <Form.Label>Username</Form.Label>
                <Form.Control type="text" name="username" onChange={this.onChange}
                              value={this.state.username}/>
            </Form.Group>
            <Form.Group controlId="password">
                <Form.Label>Password</Form.Label>
                <Form.Control type="password" name="password" onChange={this.onChange}
                              value={this.state.password}/>
            </Form.Group>
            <Form.Group controlId="ssh_port">
                <Form.Label>SSH Port</Form.Label>
                <Form.Control type="number" name="ssh_port" onChange={this.onChange} defaultValue={22}
                              value={this.state.ssh_port}/>
            </Form.Group>
            <Form.Group controlId="rmi_port">
                <Form.Label>RMI Port</Form.Label>
                <Form.Control type="number" name="rmi_port" onChange={this.onChange} defaultValue={1099}
                              value={this.state.rmi_port}/>
            </Form.Group>
            <Form.Group controlId="name">
                <Form.Label>Server Name</Form.Label>
                <Form.Control type="text" name="name" onChange={this.onChange} value={this.state.name}/>
            </Form.Group>
            <Button variant="primary" type="submit">
                Submit
            </Button>
        </Form>;
    }
};