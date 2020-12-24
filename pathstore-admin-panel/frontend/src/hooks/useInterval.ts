import {useEffect} from "react";

/**
 * Simple custom hook to call a callback function at a granularity of delay
 *
 * @param userCallback call back function
 * @param delay how often to call the call back function
 */
export function useInterval(userCallback: () => void, delay: number) {
    // Set up the interval.
    useEffect(() => {
        function tick() {
            userCallback();
        }

        if (delay !== null) {
            let id = setInterval(tick, delay);
            return () => clearInterval(id);
        }
    }, [userCallback, delay]);
}