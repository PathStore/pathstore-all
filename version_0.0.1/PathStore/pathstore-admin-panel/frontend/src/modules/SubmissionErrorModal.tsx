import React, {FunctionComponent, useContext} from "react";
import {Button, Modal} from "react-bootstrap";
import {SubmissionErrorModalContext} from "../contexts/SubmissionErrorModalContext";

/**
 * This component is used to display a submission error to the user
 * @constructor
 */
export const SubmissionErrorModal: FunctionComponent = () => {

    // dereference context
    const {visible, data, close} = useContext(SubmissionErrorModalContext);

    return (
        <Modal show={visible}
               size='lg'
               centered>
            <Modal.Header>
                <Modal.Title>Submission Error Modal</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <p>{data}</p>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={close}>Close</Button>
            </Modal.Footer>
        </Modal>
    );
};