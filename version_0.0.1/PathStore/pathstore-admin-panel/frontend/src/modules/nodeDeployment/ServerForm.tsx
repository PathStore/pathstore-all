import React, {FunctionComponent, useCallback, useContext, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {Server} from "../../utilities/ApiDeclarations";
import {SubmissionErrorModalContext} from "../../contexts/SubmissionErrorModalContext";

/**
 * Properties definition for {@link ServerForm}
 */
interface ServerFormProperties {
    /**
     * Callback function to call if the user input is valid.
     */
    readonly onFormSubmitCallback: (
        ip: string | undefined,
        username: string | undefined,
        password: string | undefined,
        ssh_port: number | undefined,
        rmi_port: number | undefined,
        name: string | undefined,
        clearForm: () => void
    ) => void;

    /**
     * Optional server option to have default values for the form (used for updates)
     */
    readonly server?: Server | undefined;
}

/**
 * This class is used to represent a server update / addition form.
 *
 * Used by:
 * @see AddServers
 * @see ModifyServerModal
 */
export const ServerForm: FunctionComponent<ServerFormProperties> = (props) => {

    // load submission modal
    const submissionErrorModal = useContext(SubmissionErrorModalContext);

    // store ip
    const [ip, setIp] = useState<string | undefined>(props.server?.ip);

    // store username
    const [username, setUsername] = useState<string | undefined>(props.server?.username);

    // store auth type
    const [authType, setAuthType] = useState<string | undefined>(undefined);

    // store optional passphrase
    const [passphrase, setPassphrase] = useState<string | undefined>(undefined);

    // store password
    const [password, setPassword] = useState<string>("");

    // store ssh port
    const [sshPort, setSshPort] = useState<number | undefined>(props.server?.ssh_port);

    // store rmi port
    const [rmiPort, setRmiPort] = useState<number | undefined>(props.server?.rmi_port);

    // store name
    const [name, setName] = useState<string | undefined>(props.server?.name);

    // clear form
    const clearForm = useCallback(() => {
        setIp("");
        setUsername("");
        setPassword("");
        setSshPort(22);
        setRmiPort(1099);
        setName("");
    }, []);

    /**
     * If all values present send the data to the callback
     */
    const onFormSubmit = useCallback((event: any): void => {
        event.preventDefault();

        if (submissionErrorModal.show) {

            if (ip === undefined || username === undefined || password === undefined || name === undefined ||
                ip === "" || username === "" || password === "" || name === "") {
                submissionErrorModal.show("You must fill in the ip, username, password and name boxes");
                return;
            }

            props.onFormSubmitCallback(
                ip,
                username,
                password,
                sshPort,
                rmiPort,
                name,
                clearForm
            );
        }
    }, [ip, username, password, sshPort, rmiPort, name, clearForm, props, submissionErrorModal]);

    return (
        <Form onSubmit={onFormSubmit}>
            <Form.Group controlId="ip">
                <Form.Label>IP Address</Form.Label>
                <Form.Control type="text" name="ip" onChange={(event: any) => setIp(event.target.value)} value={ip}/>
            </Form.Group>
            <Form.Group controlId="username">
                <Form.Label>Username</Form.Label>
                <Form.Control type="text" name="username" onChange={(event: any) => setUsername(event.target.value)}
                              value={username}/>
            </Form.Group>
            <Form.Group controlId="authetication_method">
                <Form.Label>Authentication Method</Form.Label>
                <Form.Control as="select" onChange={(event: any) => setAuthType(event.target.value)}>
                    <option>n/a</option>
                    <option>Password</option>
                    <option>Key</option>
                </Form.Control>
            </Form.Group>
            {authType === "Password" ?
                <Form.Group controlId="password">
                    <Form.Label>Password</Form.Label>
                    <Form.Control type="password" name="password"
                                  onChange={(event: any) => setPassword(event.target.value)}
                                  value={password}/>
                </Form.Group>
                :
                null
            }
            {authType === "Key" ?
                <>
                    <Form.Group controlId="key">
                        <Form.Label>Private Key Upload</Form.Label>
                        <Form.File
                            label="Private Key"
                            lang="en"
                            custom
                            onChange={(event: any) => alert("File uploaded")}
                        />
                    </Form.Group>
                    <Form.Group controlId="passphrase">
                        <Form.Label>Passphrase</Form.Label>
                        <Form.Control type="password" name="password"
                                      onChange={(event: any) => setPassphrase(event.target.value)}
                                      value={passphrase}/>
                    </Form.Group>
                </>
                :
                null
            }
            <Form.Group controlId="ssh_port">
                <Form.Label>SSH Port</Form.Label>
                <Form.Control type="number" name="ssh_port" onChange={(event: any) => setSshPort(event.target.value)}
                              defaultValue={22}
                              value={sshPort}/>
            </Form.Group>
            <Form.Group controlId="rmi_port">
                <Form.Label>RMI Port</Form.Label>
                <Form.Control type="number" name="rmi_port" onChange={(event: any) => setRmiPort(event.target.value)}
                              defaultValue={1099}
                              value={rmiPort}/>
            </Form.Group>
            <Form.Group controlId="name">
                <Form.Label>Server Name</Form.Label>
                <Form.Control type="text" name="name" onChange={(event: any) => setName(event.target.value)}
                              value={name}/>
            </Form.Group>
            <Button variant="primary" type="submit">
                Submit
            </Button>
        </Form>
    );
};