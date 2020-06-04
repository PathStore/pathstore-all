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
    const [visible, setVisible] = useState<boolean>(false);

    const [data, setData] = useState<T | undefined>(undefined);

    const show = (v?: T | undefined) => {
        setVisible(true);
        setData(v);
    };

    const close = () => {
        setVisible(false);
        setData(undefined);
    };

    return {visible, data, show, close};
}