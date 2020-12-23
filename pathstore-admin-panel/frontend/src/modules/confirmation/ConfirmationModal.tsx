import React, {FunctionComponent, useCallback, useContext} from "react";
import Modal from "react-bootstrap/Modal";
import {ConfirmationModalContext} from "../../contexts/ConfirmationModalContext";
import {Button} from "react-bootstrap";

/**
 * This component is used to present the user with a modal that explains they've clicked a button in which a webrequest
 * will be made, this gives the user the option to back out from their submission request if it was accidental or they've
 * changed their mind
 * @constructor
 */
export const ConfirmationModal: FunctionComponent = () => {

    // load data from context
    const {visible, data, close} = useContext(ConfirmationModalContext);

    // On the yes submission close the modal fire then call the callback
    const submission = useCallback((event: any) => {
        if (close && data?.onClick) {
            close();
            data.onClick();
        }
    }, [close, data]);

    return (
        <Modal show={visible}
               size={"xl"}
               centered
        >
            <Modal.Header>
                <Modal.Title>Submission Confirmation</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <p>{data?.message}</p>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={submission}>Yes</Button>
                <Button onClick={close}>No</Button>
            </Modal.Footer>
        </Modal>
    );
};