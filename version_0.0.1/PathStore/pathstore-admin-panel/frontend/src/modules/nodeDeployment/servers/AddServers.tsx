import {Component} from "react";
import {Server, Error} from "../../../utilities/ApiDeclarations";
import {webHandler} from "../../../utilities/Utils";
import React from "react";
import {LoadingModal} from "../../LoadingModal";
import {ErrorResponseModal} from "../../ErrorResponseModal";
import {ServerCreationResponseModal} from "./ServerCreationResponseModal";
import ServerForm from "./ServerForm";

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
            + "?ip=" + ip
            + "&username=" + username
            + "&password=" + password
            + "&sshPort=" + (ssh_port === undefined ? 22 : ssh_port)
            + "&rmiPort=" + (rmi_port === undefined ? 1099 : rmi_port)
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
                        clearForm();
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
                <ServerForm onFormSubmitCallback={this.onFormSubmit}/>
            </div>
        )
    }
}