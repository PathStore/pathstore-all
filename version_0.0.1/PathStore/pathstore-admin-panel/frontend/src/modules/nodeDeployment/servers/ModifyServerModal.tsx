import React, {Component} from "react";
import Modal from "react-modal";
import {Button} from "react-bootstrap";
import {Error, Server} from "../../../utilities/ApiDeclarations";
import {webHandler} from "../../../utilities/Utils";
import {LoadingModal} from "../../LoadingModal";
import {ErrorResponseModal} from "../../ErrorResponseModal";
import ServerForm from "./ServerForm";

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
     * Whether the loading modal is shown
     */
    readonly loadingModalShow: boolean

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
            loadingModalShow: false,
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
     * Load all data from form and make the api call.
     *
     * @param ip ip of server
     * @param username username of server to connect
     * @param password password of server to connect
     * @param ssh_port ssh port to connect on
     * @param rmi_port rmi port to host rmi server
     * @param name human readable name of server
     * @param clearForm ability to clear form
     */
    onFormSubmit = (
        ip: string | undefined,
        username: string | undefined,
        password: string | undefined,
        ssh_port: number | undefined,
        rmi_port: number | undefined,
        name: string | undefined,
        clearForm: () => void
    ): void => {

        let url = "/api/v1/servers"
            + "?serverUUID=" + this.props.server?.server_uuid
            + "&ip=" + ip
            + "&username=" + username
            + "&password=" + password
            + "&sshPort=" + (ssh_port === undefined ? 22 : ssh_port)
            + "&rmiPort=" + (rmi_port === undefined ? 1099 : rmi_port)
            + "&name=" + name;

        this.setState({loadingModalShow: true}, () => {
            fetch(url, {
                method: 'PUT'
            }).then(webHandler)
                .then((response: Server) => this.setState({
                        loadingModalShow: false,
                        responseModalError: false
                    }, () => {
                        clearForm();
                        this.props.forceRefresh();
                        this.props.callback();
                    })
                ).catch((response: Error[]) => this.setState({
                    loadingModalShow: false,
                    responseModalErrorData: response,
                    responseModalError: true
                })
            );
        });
    };

    /**
     * Callback for modals to close themselves
     */
    callBack = (): void => this.setState({responseModalError: false});

    /**
     * Render optional modals (loading and error)
     *
     * Render the form iff the server object isn't undefined (it shouldn't be unless an internal error occurs)
     */
    render() {

        const loadingModal =
            this.state.loadingModalShow ?
                <LoadingModal show={this.state.loadingModalShow}/>
                : null;

        const errorModal =
            this.state.responseModalError ?
                <ErrorResponseModal show={this.state.responseModalError}
                                    data={this.state.responseModalErrorData}
                                    callback={this.callBack}/>
                : null;

        return this.props.server !== undefined ?
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                {loadingModal}
                {errorModal}
                <h2>Server Modification Modal</h2>
                <h3>Delete Modal</h3>
                <Button onClick={this.delete}>Delete Server</Button>
                <br/>
                <h3>Update Modal</h3>
                <ServerForm onFormSubmitCallback={this.onFormSubmit} server={this.props.server}/>
                <br/>
                <Button onClick={this.props.callback}>close</Button>
            </Modal>
            : null;
    }
};