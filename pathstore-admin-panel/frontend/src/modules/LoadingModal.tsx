import React, {FunctionComponent, useContext} from "react";
import Modal from "react-bootstrap/Modal";
import {LoadingModalContext} from "../contexts/LoadingModalContext";

/**
 * Forced loading modal that cannot be closed by user.
 *
 * @constructor
 * @see LoadingModalContext
 */
export const LoadingModal: FunctionComponent = () =>
    <Modal show={useContext(LoadingModalContext).visible} size='sm' centered>
        <Modal.Body>
            <p>Loading....</p>
        </Modal.Body>
    </Modal>;

