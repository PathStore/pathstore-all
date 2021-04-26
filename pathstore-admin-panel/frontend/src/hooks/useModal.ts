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

import {useState} from "react";

/**
 * Definition for the what data is needed by a given modal.
 *
 * @type T refers to the type of data you wish to store for a given modal. You need to provide this data on {@link ModalInfo#show}
 */
export interface ModalInfo<T> {
    /**
     * Whether the modal is visible or not. This is used for internal use within the modal only
     */
    readonly visible: boolean;

    /**
     * Data given to the modal on {@link ModalInfo#show}
     */
    readonly data: T | undefined;

    /**
     * This will set {@link ModalInfo#visible} to true and set {@link ModalInfo#data} to v
     */
    readonly show: (v?: T | undefined) => void;

    /**
     * This will set {@link ModalInfo#visible} to false and set {@link ModalInfo#data} to undefined
     */
    readonly close: () => void;
}

/**
 * This function is used by a provider to gather the data to allow all children components to enter the given context
 */
export function useModal<T>(): ModalInfo<T> {
    // visible state
    const [visible, setVisible] = useState<boolean>(false);

    // what data to send
    const [data, setData] = useState<T | undefined>(undefined);

    // how to handle show
    const show = (v?: T | undefined) => {
        setVisible(true);
        setData(v);
    };

    // how to handle close
    const close = () => {
        setVisible(false);
        setData(undefined);
    };

    return {visible, data, show, close};
}