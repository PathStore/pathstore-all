import {ApplicationCreationSuccess} from "../../utilities/ApiDeclarations";
import React, {FunctionComponent} from "react";
import Modal from "react-modal";
import {Button} from "react-bootstrap";

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
    <Modal isOpen={props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
        <p>{props.data !== null ?
            ("Successfully create application named: " + props.data.keyspace_created) :
            "Error parsing application success response"}</p>
        <Button onClick={props.callback}>close</Button>
    </Modal>;