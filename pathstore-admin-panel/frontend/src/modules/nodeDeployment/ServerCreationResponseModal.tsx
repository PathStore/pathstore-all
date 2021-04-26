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
import Modal from "react-bootstrap/Modal";
import {Button} from "react-bootstrap";
import {ServerCreationResponseModalContext} from "../../contexts/ServerCreationResponseModalContext";

/**
 * This component is used to display the response of a successful server submission
 * @constructor
 */
export const ServerCreationResponseModal: FunctionComponent = () => {

    // load modal state context
    const {visible, data, close} = useContext(ServerCreationResponseModalContext);

    return (
        <Modal show={visible} size={'lg'} centered>
            <Modal.Header>
                <Modal.Title>Server Created</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <p>{data ?
                    ("Successfully create server with uuid: " + data.server_uuid + " and ip: " + data.ip) :
                    "Error parsing success response"}</p>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={close}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};