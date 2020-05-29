import React, {FunctionComponent} from "react";
import {Error} from "../utilities/ApiDeclarations";
import {Button} from "react-bootstrap";
import Modal from "react-bootstrap/Modal";

/**
 * Properties file for {@link ErrorResponseModal}
 */
interface ErrorResponseModalProperties {
    /**
     * Whether to show the modal or not
     */
    readonly show: boolean

    /**
     * List of errors
     */
    readonly data: Error[]

    /**
     * Callback function to close modal
     */
    readonly callback: () => void
}

/**
 * This component is used to parse and display an error message generated from the api (error code 400)
 *
 * @param props
 * @constructor
 */
export const ErrorResponseModal: FunctionComponent<ErrorResponseModalProperties> = (props) => {
    let message = "The following errors occured: ";

    for (let i = 0; i < props.data.length; i++)
        message += " " + (i + 1) + ". " + props.data[i].error;

    return (
        <Modal show={props.show}
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
                <Button onClick={props.callback}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};