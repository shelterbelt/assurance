import { Button, ListGroup, Container, Row, Col } from "react-bootstrap"
export default function HistoryPanel() {
    return (
        <Container className="previous-scans-list-container">
            <Row>
                <Col className="px-0">
                    <ListGroup className="previous-scans-list-group">
                    </ListGroup>
                </Col>
            </Row>
            <Row className="spacer-row" />
            <Row className="actions-row">
                <Col className="px-0">
                    <Button className="results-manage-button" variant="secondary">Delete</Button>
                </Col>
                <Col className="spacer-col"/>
                <Col className="px-0">
                    <Button className="results-manage-button" variant="secondary">Resolve</Button>
                </Col>
            </Row>        
        </Container>
    );
}