import React, {FunctionComponent, useCallback, useContext} from "react";
import {Button} from "react-bootstrap";
import {
    ConfirmationModalContext,
    ConfirmationModalData,
    ConfirmationModalProvider
} from "../../contexts/ConfirmationModalContext";

/**
 * This component is used to allow the verification from the user that they confirm that they want to submit a given
 * request. The user must pass the message to display and how they want to proceed if the user says yes
 *
 * @param props
 * @constructor
 * @see HelperConfirmationButton
 */
export const ConfirmationButton: FunctionComponent<ConfirmationModalData> = (props) =>
    <ConfirmationModalProvider>
        <HelperConfirmationButton onClick={props.onClick} message={props.message}>
            {props.children}
        </HelperConfirmationButton>
    </ConfirmationModalProvider>;

/**
 * Helper component to the above component to use a prop callback function to not use inlined props functions
 * @param props
 * @constructor
 */
const HelperConfirmationButton: FunctionComponent<ConfirmationModalData> = (props) => {
    // load show
    const {show} = useContext(ConfirmationModalContext);

    /**
     * On Click show {@link ConfirmationModal} with the passed props
     */
    const onClick = useCallback((event: any) => {
        if (show)
            show(props as ConfirmationModalData);
    }, [show, props]);

    return (
        <Button onClick={onClick}>
            {props.children}
        </Button>
    );
};