import React, {FunctionComponent, useContext} from "react";
import Modal from "react-bootstrap/Modal";
import {Button} from "react-bootstrap";
import {ServerCreationResponseModalContext} from "../../contexts/ServerCreationResponseModalContext";

/**
 * This component is used to display the response of a successful server submission
 * @constructor
 */
export const ServerCreationResponseModal: FunctionComponent = () => {

    // load modal state context
    const {visible, data, close} = useContext(ServerCreationResponseModalContext);

    return (
        <Modal show={visible} size={'lg'} centered>
            <Modal.Header>
                <Modal.Title>Server Created</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <p>{data ?
                    ("Successfully create server with uuid: " + data.server_uuid + " and ip: " + data.ip) :
                    "Error parsing success response"}</p>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={close}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};