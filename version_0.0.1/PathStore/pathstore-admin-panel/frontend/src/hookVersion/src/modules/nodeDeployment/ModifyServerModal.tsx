import React, {FunctionComponent, useCallback, useContext} from "react";
import Modal from "react-bootstrap/Modal";
import {Button} from "react-bootstrap";
import {ModifyServerModalContext} from "../../contexts/ModifyServerModalContext";
import {LoadingModalContext} from "../../contexts/LoadingModalContext";
import {ErrorModalContext} from "../../contexts/ErrorModalContext";
import {APIContext} from "../../contexts/APIContext";
import ServerForm from "./ServerForm";
import {webHandler} from "../../utilities/Utils";

/**
 * This component is used to allow the user to modify a free server record whether that be to update its properties
 * or to delete it.
 * @constructor
 */
export const ModifyServerModal: FunctionComponent = () => {

    // force refresh all data in the api when the user submits their changes
    const {forceRefresh} = useContext(APIContext);

    // loading modal to display on submission
    const loadingModal = useContext(LoadingModalContext);

    // error modal to display any errors
    const errorModal = useContext(ErrorModalContext);

    // data for this given modal
    const {visible, data, close} = useContext(ModifyServerModalContext);

    /**
     * This callback is used when the user clicks the delete button.
     *
     * It will submit the information to the api for deletion
     */
    const deleteServer = useCallback((): void => {
        if (loadingModal.show && loadingModal.close && data && errorModal.show) {
            loadingModal.show();
            fetch('/api/v1/servers?server_uuid=' + data?.server_uuid, {
                method: 'DELETE'
            })
                .then(webHandler)
                .then(() => {
                    if (forceRefresh && close) {
                        forceRefresh();
                        close();
                    }
                })
                .catch(errorModal.show)
                .finally(loadingModal.close);
        }

    }, [loadingModal, data, forceRefresh, close, errorModal.show]);

    /**
     * This callback is used when the user submits the form.
     *
     * It will update the data and send it to the api
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

        let url = "/api/v1/servers"
            + "?server_uuid=" + data?.server_uuid
            + "&ip=" + ip
            + "&username=" + username
            + "&password=" + password
            + "&ssh_port=" + (ssh_port === undefined ? 22 : ssh_port)
            + "&rmi_port=" + (rmi_port === undefined ? 1099 : rmi_port)
            + "&name=" + name;

        if (loadingModal.show && loadingModal.close && errorModal.show)
            loadingModal.show();
        fetch(url, {
            method: 'PUT'
        })
            .then(webHandler)
            .then(() => {
                clearForm();
                if (forceRefresh && close) {
                    forceRefresh();
                    close()
                }
            })
            .catch(errorModal.show)
            .finally(loadingModal.close);
    }, [loadingModal, forceRefresh, close, errorModal.show, data]);

    return (
        <Modal show={visible} size={'xl'} centered>
            <Modal.Header>
                <Modal.Title>Server Modification Modal</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <h3>Delete Modal</h3>
                <Button onClick={deleteServer}>Delete Server</Button>
                <hr/>
                <h3>Update Modal</h3>
                <ServerForm onFormSubmitCallback={onFormSubmit} server={data}/>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={close}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};