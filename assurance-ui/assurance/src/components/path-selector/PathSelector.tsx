import { Form, FloatingLabel, InputGroup } from "react-bootstrap";
import { useRef } from "react";

export default function PathSelector({label, additionalClasses, dialogOptions}) {
    const field = useRef(null);

    additionalClasses.push("scan-location-field");

    dialogOptions.buttonLabel = "Select";
    dialogOptions.properties = ["openDirectory"];

    function handleFileSelectorClicked() {
        window.assuranceapi.selectPath(dialogOptions, loadPathValue);
    }

    function loadPathValue(pathResult) {
        let selectedPath = "";
        if (pathResult && !pathResult.canceled) {
            selectedPath = pathResult.filePaths[0];
            field.current.value = selectedPath;
        }
    }

    return (
        <InputGroup className="scan-location-field">
            <FloatingLabel label={label}>
                <Form.Control type="text" className={additionalClasses.join(" ")} placeholder={label} ref={field} />
            </FloatingLabel>
            <InputGroup.Text onClick={handleFileSelectorClicked}>...</InputGroup.Text>
        </InputGroup>
    );
}
