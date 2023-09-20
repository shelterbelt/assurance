import { Button, ListGroup, Container, Row, Col } from "react-bootstrap"

export default function ScanList() {
    return (
        <Container className="scan-list-container">
            <Row>
                <Col className="px-0">
                    <ListGroup className="scan-list-group">
                    </ListGroup>
                </Col>
            </Row>
            <Row className="spacer-row" />
            <Row className="actions-row">
                <Col className="px-0">
                    <Button className="scan-manage-button" variant="secondary">New</Button>
                </Col>
                <Col className="spacer-col"/>
                <Col className="px-0">
                    <Button className="scan-manage-button" variant="secondary">Delete</Button>
                </Col>
            </Row>        
        </Container>
    );
}
