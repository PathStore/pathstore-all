import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {SubmissionErrorModal} from "../modules/SubmissionErrorModal";

/**
 * This context is used to work with the submission error modal
 */
export const SubmissionErrorModalContext = createContext<Partial<ModalInfo<string>>>({});

/**
 * Wrap this provider around a component that needs to display a submission error
 * @param props
 * @constructor
 */
export const SubmissionErrorModalProvider: FunctionComponent = (props) =>
    <SubmissionErrorModalContext.Provider value={useModal<string>()}>
        <SubmissionErrorModal/>
        {props.children}
    </SubmissionErrorModalContext.Provider>;