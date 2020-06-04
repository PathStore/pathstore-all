import React, {FunctionComponent, useCallback, useContext} from "react";
import {LoadingModalContext} from "../../contexts/LoadingModalContext";
import {ErrorModalContext} from "../../contexts/ErrorModalContext";
import {APIContext} from "../../contexts/APIContext";
import {ServerCreationResponseModalContext} from "../../contexts/ServerCreationResponseModalContext";
import {webHandler} from "../../utilities/Utils";
import {Server} from "../../utilities/ApiDeclarations";
import ServerForm from "./ServerForm";

/**
 * This component is used to render a form to add a server to the server pool
 * @constructor
 */
export const AddServers: FunctionComponent = () => {

    // load force refresh
    const {forceRefresh} = useContext(APIContext);

    // load the loading modal context
    const loadingModal = useContext(LoadingModalContext);

    // load error modal context
    const errorModal = useContext(ErrorModalContext);

    // load show from server creation response modal context
    const {show} = useContext(ServerCreationResponseModalContext);

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
    const onFormSubmit = useCallback((
        ip: string | undefined,
        username: string | undefined,
        password: string | undefined,
        ssh_port: number | undefined,
        rmi_port: number | undefined,
        name: string | undefined,
        clearForm: () => void
    ): void => {

        if (loadingModal.show && loadingModal.close && errorModal.show && forceRefresh) {

            let url = "/api/v1/servers"
                + "?ip=" + ip
                + "&username=" + username
                + "&password=" + password
                + "&ssh_port=" + (ssh_port === undefined ? 22 : ssh_port)
                + "&rmi_port=" + (rmi_port === undefined ? 1099 : rmi_port)
                + "&name=" + name;

            /*
                    clearForm();
            this.props.callback();
             */
            loadingModal.show();
            fetch(url, {
                method: 'POST'
            })
                .then(webHandler)
                .then((s: Server) => {
                    if (show)
                        show(s);
                    clearForm();
                    forceRefresh();
                })
                .catch(errorModal.show)
                .finally(loadingModal.close);
        }
    }, [loadingModal, errorModal, forceRefresh, show]);

    return (
        <>
            <h3>Create Server</h3>
            <ServerForm onFormSubmitCallback={onFormSubmit}/>
        </>
    );
};