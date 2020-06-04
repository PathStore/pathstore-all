import React, {FunctionComponent, useContext} from "react";
import {Button} from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import {ErrorModalContext} from "../contexts/ErrorModalContext";

/**
 * This component is used to parse and display an error message generated from the api (error code 400)
 *
 * @constructor
 * @see ErrorModalContext
 */
export const ErrorResponseModal: FunctionComponent = () => {

    const {visible, data, close} = useContext(ErrorModalContext);

    let message = "The following errors occured: ";

    if (data)
        for (let i = 0; i < data.length; i++)
            message += " " + (i + 1) + ". " + data[i].error;

    return (
        <Modal show={visible}
               size='lg'
               centered
        >
            <Modal.Header>
                <Modal.Title>An Error has occured</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <p>{message}</p>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={close}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};