import React, {FunctionComponent} from "react";
import Modal from "react-modal";

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
    <Modal isOpen={props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
        <p>Loading</p>
    </Modal>;