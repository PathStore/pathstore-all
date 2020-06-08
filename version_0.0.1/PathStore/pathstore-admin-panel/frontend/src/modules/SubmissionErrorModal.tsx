import React, {FunctionComponent} from "react";
import {Button, Modal} from "react-bootstrap";

/**
 * Properties definition for {@link SubmissionErrorModal}
 */
interface SubmissionErrorModalProps {
    /**
     * Whether to show the modal or nit
     */
    readonly show: boolean;

    /**
     * Callback to parent component to flip show from true to false
     */
    readonly close: () => void;
}

/**
 * This component is used to inform the user of some error that has occurred during the submission of some form
 * @param props
 * @constructor
 */
export const SubmissionErrorModal: FunctionComponent<SubmissionErrorModalProps> = (props) =>
    <Modal show={props.show}
           size='lg'
           centered>
        <Modal.Header>
            <Modal.Title>Submission Error Modal</Modal.Title>
        </Modal.Header>
        <Modal.Body>
            {props.children}
        </Modal.Body>
        <Modal.Footer>
            <Button onClick={props.close}>Close</Button>
        </Modal.Footer>
    </Modal>;