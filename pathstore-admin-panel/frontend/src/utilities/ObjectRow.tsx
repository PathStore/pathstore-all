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