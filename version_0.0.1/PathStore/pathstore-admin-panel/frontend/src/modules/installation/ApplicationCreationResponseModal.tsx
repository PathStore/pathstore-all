import {ApplicationCreationSuccess} from "../../utilities/ApiDeclarations";
import React, {FunctionComponent} from "react";
import {Button} from "react-bootstrap";
import Modal from "react-bootstrap/Modal";

/**
 * Properties definition for {@link ApplicationCreationResponseModal}
 */
interface ApplicationCreationResponseModalProperties {
    /**
     * Whether to show the modal or not
     */
    readonly show: boolean

    /**
     * What was the response from the api
     */
    readonly data: ApplicationCreationSuccess | null

    /**
     * Callback function to close modal
     */
    readonly callback: () => void
}

/**
 * This component is used to display a successful response of an application installation
 *
 * @param props
 * @constructor
 */
export const ApplicationCreationResponseModal: FunctionComponent<ApplicationCreationResponseModalProperties> = (props) =>
    <Modal show={props.show} size={'lg'} centered>
        <Modal.Header>
            <Modal.Title>Application Created</Modal.Title>
        </Modal.Header>
        <Modal.Body>
            <p>{props.data !== null ?
                ("Successfully create application named: " + props.data.keyspace_created) :
                "Error parsing application success response"}</p>
        </Modal.Body>
        <Modal.Footer>
            <Button onClick={props.callback}>close</Button>
        </Modal.Footer>
    </Modal>;