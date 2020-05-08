import React, {FunctionComponent} from "react";
import Modal from "react-modal";
import {Error} from "../utilities/ApiDeclarations";

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
        <Modal isOpen={props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
            <p>{message}</p>
            <button onClick={props.callback}>close</button>
        </Modal>
    );
};