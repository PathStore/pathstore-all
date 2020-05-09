import {Component} from "react";
import ReactDOM from "react-dom";
import {Server, Error} from "../../../utilities/ApiDeclarations";
import {webHandler} from "../../../utilities/Utils";
import {Button, Form} from "react-bootstrap";
import React from "react";
import {LoadingModal} from "../../LoadingModal";
import {ErrorResponseModal} from "../../ErrorResponseModal";
import {ServerCreationResponseModal} from "./ServerCreationResponseModal";

/**
 * Properties definition for {@link AddServers}
 */
interface AddServersProperties {
    /**
     * List of servers from api
     */
    readonly servers: Server[]

    /**
     * Callback to refresh component props
     */
    readonly callback: () => void
}

/**
 * State definition for {@link AddServers}
 */
interface AddServersState {
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
 * This class is used by {@link NodeDeploymentModal} to allow the user to add servers
 */
export default class AddServers extends Component<AddServersProperties, AddServersState> {

    /**
     * Used to clear message form
     */
    private messageForm: any;

    /**
     * Initializes props and state
     *
     * @param props
     */
    constructor(props: AddServersProperties) {
        super(props);

        this.state = {
            loadingModalShow: false,
            responseModalShow: false,
            responseModalData: null,
            responseModalError: false,
            responseModalErrorData: []
        }
    }

    /**
     * Load all data from form and make the api call.
     *
     * @param event
     */
    onFormSubmit = (event: any): void => {
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
                .then((response: Server) => this.setState({
                        loadingModalShow: false,
                        responseModalShow: true,
                        responseModalData: response,
                        responseModalError: false
                    }, () => {
                        // @ts-ignore
                        ReactDOM.findDOMNode(this.messageForm).reset();
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
     * First figure out if you need to show the loading modal
     *
     * Second figure out if you need to display a response modal and if so which one
     *
     * Finally render display form
     *
     * @returns {*}
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

        return (
            <div>
                {loadingModal}
                {responseModal}
                <h3>Create Server</h3>
                <Form onSubmit={this.onFormSubmit} ref={(form: any) => this.messageForm = form}>
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