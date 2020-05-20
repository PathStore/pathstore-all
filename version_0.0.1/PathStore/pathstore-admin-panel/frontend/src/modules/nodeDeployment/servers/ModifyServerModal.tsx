import React, {Component} from "react";
import Modal from "react-modal";
import {Button, Form} from "react-bootstrap";
import {Error, Server} from "../../../utilities/ApiDeclarations";
import {webHandler} from "../../../utilities/Utils";
import {LoadingModal} from "../../LoadingModal";
import {ErrorResponseModal} from "../../ErrorResponseModal";
import {ServerCreationResponseModal} from "./ServerCreationResponseModal";

/**
 * Properties definition for {@link ModifyServerModal}
 */
interface ModifyServerModalProperties {
    /**
     * Whether to show the modal or not
     */
    readonly show: boolean;

    /**
     * Callback to close modal
     */
    readonly callback: () => void;

    /**
     * Server object from on click
     */
    readonly server: Server | undefined;

    /**
     * Callback function to refresh all component props
     */
    readonly forceRefresh: () => void
}

/**
 * State definition for {@link ModifyServerModal}
 */
interface ModifyServerModalState {
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

    /**
     * Whether the loading modal is shown
     */
    readonly loadingModalShow: boolean

    /**
     * Whether the response modal is show
     */
    readonly responseModalShow: boolean

    /**
     * What server info to give to the response modal
     */
    readonly responseModalData: Server | null

    /**
     * Whether to load the error response modal
     */
    readonly responseModalError: boolean

    /**
     * Error response modal data (errors)
     */
    readonly responseModalErrorData: Error[]
}

/**
 * This modal is used to modify a given free server object. The user can delete or modify the information
 */
export default class ModifyServerModal extends Component<ModifyServerModalProperties, ModifyServerModalState> {

    /**
     * Initializes props and state
     *
     * @param props
     */
    constructor(props: ModifyServerModalProperties) {
        super(props);

        this.state = {
            ip: this.props.server?.ip,
            username: this.props.server?.username,
            password: "",
            ssh_port: this.props.server?.ssh_port,
            rmi_port: this.props.server?.rmi_port,
            name: this.props.server?.name,
            loadingModalShow: false,
            responseModalShow: false,
            responseModalData: null,
            responseModalError: false,
            responseModalErrorData: []
        }
    }

    /**
     * Requests the given node gets deleted
     */
    delete = (): void => {
        fetch('/api/v1/servers?serverUUID=' + this.props.server?.server_uuid, {
            method: 'DELETE'
        })
            .then(webHandler)
            .then(() => {
                this.props.forceRefresh();
                this.props.callback();
            })
            .catch(response => {
                alert(JSON.stringify(response));
            })

    };


    /**
     * Generic on change function to update state information
     *
     * @param event event from control form
     */
    onChange = (event: any): void =>
        this.setState({[event.target.name]: event.target.value} as ModifyServerModalState);

    /**
     * Submit the new information to the server for submission
     *
     * @param event
     */
    onFormSubmit = (event: any): void => {
        event.preventDefault();

        let url = "/api/v1/servers"
            + "?serverUUID=" + this.props.server?.server_uuid
            + "&ip=" + this.state.ip
            + "&username=" + this.state.username
            + "&password=" + this.state.password
            + "&sshPort=" + (this.state.ssh_port === undefined ? 22 : this.state.ssh_port)
            + "&rmiPort=" + (this.state.rmi_port === undefined ? 1099 : this.state.rmi_port)
            + "&name=" + this.state.name;

        this.setState({loadingModalShow: true}, () => {
            fetch(url, {
                method: 'PUT'
            }).then(webHandler)
                .then((response: Server) => this.setState({
                        loadingModalShow: false,
                        responseModalShow: true,
                        responseModalData: response,
                        responseModalError: false
                    }, () => {
                        this.props.forceRefresh();
                        this.props.callback();
                    })
                ).catch((response: Error[]) => this.setState({
                    loadingModalShow: false,
                    responseModalShow: true,
                    responseModalErrorData: response,
                    responseModalError: true
                })
            );
        });
    };

    /**
     * Callback for modals to close themselves
     */
    callBack = (): void => this.setState({responseModalShow: false});

    /**
     * Render optional modals (loading and response)
     *
     * Render the form iff the server object isn't undefined (it shouldn't be unless an internal error occurs)
     */
    render() {

        const loadingModal = this.state.loadingModalShow ? <LoadingModal show={this.state.loadingModalShow}/> : null;

        const responseModal =
            this.state.responseModalShow ?
                this.state.responseModalError ?
                    <ErrorResponseModal show={this.state.responseModalShow}
                                        data={this.state.responseModalErrorData}
                                        callback={this.callBack}/>
                    :
                    <ServerCreationResponseModal show={this.state.responseModalShow}
                                                 data={this.state.responseModalData}
                                                 callback={this.callBack}/>
                : null;

        return this.props.server !== undefined ?
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                {loadingModal}
                {responseModal}
                <h2>Server Modification Modal</h2>
                <h3>Delete Modal</h3>
                <Button onClick={this.delete}>Delete Server</Button>
                <br/>
                <h3>Update Modal</h3>
                <Form onSubmit={this.onFormSubmit}>
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
                        <Form.Control type="number" name="ssh_port" onChange={this.onChange}
                                      value={this.state.ssh_port}/>
                    </Form.Group>
                    <Form.Group controlId="rmi_port">
                        <Form.Label>RMI Port</Form.Label>
                        <Form.Control type="number" name="rmi_port" onChange={this.onChange}
                                      value={this.state.rmi_port}/>
                    </Form.Group>
                    <Form.Group controlId="name">
                        <Form.Label>Server Name</Form.Label>
                        <Form.Control type="text" name="name" onChange={this.onChange} value={this.state.name}/>
                    </Form.Group>
                    <Button variant="primary" type="submit">
                        Submit
                    </Button>
                </Form>
                <br/>
                <Button onClick={this.props.callback}>close</Button>
            </Modal>
            : null;
    }
};