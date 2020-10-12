import React, {FunctionComponent, useCallback, useContext} from "react";
import Modal from "react-bootstrap/Modal";
import {Button} from "react-bootstrap";
import {ModifyServerModalContext} from "../../contexts/ModifyServerModalContext";
import {LoadingModalContext} from "../../contexts/LoadingModalContext";
import {ErrorModalContext} from "../../contexts/ErrorModalContext";
import {APIContext} from "../../contexts/APIContext";
import {webHandler} from "../../utilities/Utils";
import {ServerForm} from "./ServerForm";
import {SubmissionErrorModalProvider} from "../../contexts/SubmissionErrorModalContext";
import {SERVER_AUTH_TYPE} from "../../utilities/ApiDeclarations";

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
        authType: string | undefined,
        privateKey: File | undefined,
        passphrase: string | undefined,
        password: string | undefined,
        ssh_port: number | undefined,
        grpc_port: number | undefined,
        name: string | undefined,
        clearForm: () => void
    ): void => {
        if (ip && username && name && data?.server_uuid) {
            let formData = new FormData();
            formData.append("server_uuid", data.server_uuid);
            formData.append("ip", ip);
            formData.append("username", username);
            formData.append("ssh_port", (ssh_port === undefined ? 22 : ssh_port).toString());
            formData.append("grpc_port", (grpc_port === undefined ? 1099 : grpc_port).toString());
            formData.append("name", name);


            if (authType === "Password" && password) {
                formData.append("auth_type", SERVER_AUTH_TYPE[SERVER_AUTH_TYPE.PASSWORD]);
                formData.append("password", password);
            } else if (authType === "Key" && privateKey) {
                formData.append("auth_type", SERVER_AUTH_TYPE[SERVER_AUTH_TYPE.IDENTITY]);
                formData.append("privateKey", privateKey);

                if (passphrase)
                    formData.append("passphrase", passphrase);
            } else {
                alert("ERROR in add server call");
                return;
            }

            if (loadingModal.show && loadingModal.close && errorModal.show)
                loadingModal.show();
            fetch("/api/v1/servers", {
                method: 'PUT',
                body: formData
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
        }
    }, [data, loadingModal, forceRefresh, close, errorModal.show]);

    return (
        <Modal show={visible} size={'xl'} centered>
            <Modal.Header>
                <Modal.Title>Server Modification Modal</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <h3>Delete Modal</h3>
                <Button onClick={deleteServer}>Delete Server</Button>
                Temporary removal until PUT requests can receive files
                <hr/>
                <h3>Update Modal</h3>
                <SubmissionErrorModalProvider>
                    <ServerForm onFormSubmitCallback={onFormSubmit} server={data}/>
                </SubmissionErrorModalProvider>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={close}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};