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

import React, {PropsWithChildren, useCallback} from "react";

/**
 * Properties for a row component which has a generic type callback
 */
interface ObjectRowProperties<T> {
    /**
     * What value to pass as props
     */
    readonly value: T;

    /**
     * What function to call on click
     */
    readonly handleClick: (value: T) => void
}

/**
 * This generic component is used to have a row in a table that sends an object on creation and passed the same object
 * on click into a call back function
 * @param value value to pass back
 * @param handleClick how to pass that value back
 * @param children children to display the value to the user
 * @constructor
 */
export const ObjectRow = <T,>({value, handleClick, children}: PropsWithChildren<ObjectRowProperties<T>>) => {

    // call the handle click function on row click
    const onClick = useCallback(() => handleClick(value), [handleClick, value]);

    return (
        <tr onClick={onClick}>
            {children}
        </tr>
    );
};