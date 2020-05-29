import React, {FunctionComponent} from "react";
import Modal from "react-bootstrap/Modal";

/**
 * Properties definition for {@link LoadingModal}
 */
interface LoadingModalProperties {
    /**
     * Whether to show the modal or not
     */
    readonly show: boolean
}

/**
 * Forced loading modal that cannot be closed by user.
 *
 * @param props
 * @constructor
 */
export const LoadingModal: FunctionComponent<LoadingModalProperties> = (props) =>
    <Modal show={props.show} size='sm' centered>
        <Modal.Body>
            <p>Loading....</p>
        </Modal.Body>
    </Modal>;