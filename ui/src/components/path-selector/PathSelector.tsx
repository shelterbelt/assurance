import { ChangeEvent } from 'react';
import { Form, FloatingLabel, InputGroup } from 'react-bootstrap';

interface PathSelectorDialogOptions {
    title?: string;
    buttonLabel?: string;
    properties?: string[];
}

interface PathSelectorProps {
    label: string;
    value: string;
    onChange: (path: string) => void;
    additionalClasses?: string[];
    dialogOptions: PathSelectorDialogOptions;
}

interface PathSelectionResult {
    canceled: boolean;
    filePaths: string[];
}

/**
 * Controlled path selector. The parent owns the `value`/`onChange` pair so
 * the form state is observable for validation and submit. The browse button
 * delegates to the OS-native folder picker exposed by the preload bridge.
 */
export default function PathSelector({
    label,
    value,
    onChange,
    additionalClasses = [],
    dialogOptions,
}: PathSelectorProps) {
    const classes = [...additionalClasses, 'scan-location-field'];

    function handleBrowseClicked() {
        const options = {
            ...dialogOptions,
            buttonLabel: dialogOptions.buttonLabel ?? 'Select',
            properties: dialogOptions.properties ?? ['openDirectory'],
        };
        window.assuranceapi.selectPath(options, (result: unknown) => {
            const r = result as PathSelectionResult | undefined;
            if (r && !r.canceled && r.filePaths && r.filePaths.length > 0) {
                onChange(r.filePaths[0]);
            }
        });
    }

    function handleTextChange(event: ChangeEvent<HTMLInputElement>) {
        onChange(event.target.value);
    }

    return (
        <InputGroup className="scan-location-field">
            <FloatingLabel label={label}>
                <Form.Control
                    type="text"
                    className={classes.join(' ')}
                    placeholder={label}
                    value={value}
                    onChange={handleTextChange}
                />
            </FloatingLabel>
            <InputGroup.Text onClick={handleBrowseClicked} role="button">
                ...
            </InputGroup.Text>
        </InputGroup>
    );
}
