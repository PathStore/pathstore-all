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

import React, {FunctionComponent, useContext} from "react";
import {Button} from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import {ErrorModalContext} from "../contexts/ErrorModalContext";

/**
 * This component is used to parse and display an error message generated from the api (error code 400)
 *
 * @constructor
 * @see ErrorModalContext
 */
export const ErrorResponseModal: FunctionComponent = () => {

    const {visible, data, close} = useContext(ErrorModalContext);

    let message = "The following errors occured: ";

    if (data)
        for (let [index, value] of data.entries())
            message += " " + (index + 1) + ". " + value.error;

    return (
        <Modal show={visible}
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
                <Button onClick={close}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};