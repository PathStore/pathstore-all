import React, {createContext, FunctionComponent} from "react";
import {APIContextType, useAPIContext} from "../hooks/useAPIContext";

/**
 * API Context. This is a global context which stores all the data gathered from the api
 */
export const APIContext = createContext<Partial<APIContextType>>({});

/**
 * This is the parent provider for the the API Context which is wrapped around the entry point of the program so it is
 * a global context
 *
 * @param props
 * @constructor
 * @see useAPIContext
 */
export const APIContextProvider: FunctionComponent = (props) =>
    <APIContext.Provider value={useAPIContext()}>
        {props.children}
    </APIContext.Provider>;
