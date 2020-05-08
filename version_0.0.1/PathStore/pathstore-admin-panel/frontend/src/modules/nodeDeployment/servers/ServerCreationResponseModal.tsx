import {Server} from "../../../utilities/ApiDeclarations";
import React, {FunctionComponent} from "react";
import Modal from "react-modal";

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
    <Modal isOpen={props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
        <p>{props.data !== null ?
            ("Successfully create server with uuid: " + props.data.server_uuid + " and ip: " + props.data.ip) :
            "Error parsing success response"}</p>
        <button onClick={props.callback}>close</button>
    </Modal>;