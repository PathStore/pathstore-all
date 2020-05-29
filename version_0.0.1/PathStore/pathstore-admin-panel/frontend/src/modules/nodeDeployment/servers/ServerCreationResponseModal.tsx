import {Server} from "../../../utilities/ApiDeclarations";
import React, {FunctionComponent} from "react";
import {Button} from "react-bootstrap";
import Modal from "react-bootstrap/Modal";

/**
 * Properties definition for {@link ServerCreationResponseModal}
 */
interface ServerCreationResponseModalProperties {
    /**
     * Whether to show the modal or not
     */
    readonly show: boolean

    /**
     * Success response for server
     */
    readonly data: Server | null

    /**
     * Callback function to close modal
     */
    readonly callback: () => void
}

/**
 * This component is used when an add server request as gone through successfully
 *
 * @param props
 * @constructor
 */
export const ServerCreationResponseModal: FunctionComponent<ServerCreationResponseModalProperties> = (props) =>
    <Modal show={props.show} size={'lg'} centered>
        <Modal.Header>
            <Modal.Title>Server Created</Modal.Title>
        </Modal.Header>
        <Modal.Body>
            <p>{props.data !== null ?
                ("Successfully create server with uuid: " + props.data.server_uuid + " and ip: " + props.data.ip) :
                "Error parsing success response"}</p>
        </Modal.Body>
        <Modal.Footer>
            <Button onClick={props.callback}>close</Button>
        </Modal.Footer>
    </Modal>;