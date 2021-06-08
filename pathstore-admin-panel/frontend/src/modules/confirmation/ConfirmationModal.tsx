/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, {FunctionComponent, useCallback, useContext} from "react";
import Modal from "react-bootstrap/Modal";
import {ConfirmationModalContext} from "../../contexts/ConfirmationModalContext";
import {Button} from "react-bootstrap";

/**
 * This component is used to present the user with a modal that explains they've clicked a button in which a webrequest
 * will be made, this gives the user the option to back out from their submission request if it was accidental or they've
 * changed their mind
 * @constructor
 */
export const ConfirmationModal: FunctionComponent = () => {

    // load data from context
    const {visible, data, close} = useContext(ConfirmationModalContext);

    // On the yes submission close the modal fire then call the callback
    const submission = useCallback((event: any) => {
        if (close && data?.onClick) {
            close();
            data.onClick();
        }
    }, [close, data]);

    return (
        <Modal show={visible}
               size={"xl"}
               centered
        >
            <Modal.Header>
                <Modal.Title>Submission Confirmation</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <p>{data?.message}</p>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={submission}>Yes</Button>
                <Button onClick={close}>No</Button>
            </Modal.Footer>
        </Modal>
    );
};