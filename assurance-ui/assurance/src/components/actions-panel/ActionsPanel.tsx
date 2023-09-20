import { Button, Container, Row } from "react-bootstrap"
export default function ActionsPanel() {
    return (
        <Container className="actions-panel-container">
            <Row>
                <Button className="action-button" variant="secondary">Scan</Button><br />
            </Row>
            <Row className="spacer-row" />
            <Row>
                <Button className="action-button" variant="secondary">Scan and Merge</Button><br />
            </Row>
        </Container>
    );
}
