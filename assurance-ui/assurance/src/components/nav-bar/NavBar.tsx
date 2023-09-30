import { Nav } from "react-bootstrap"

export default function NavBar({disabled}) {
    return (
        <Nav variant="tabs" className="nav-bar mx-auto pb-3" justify>
            <Nav.Item>
                <Nav.Link disabled={disabled} className="nav-bar-left-cap" eventKey="ScanContent">Scan</Nav.Link>
            </Nav.Item>
            <Nav.Item>
                <Nav.Link disabled={disabled} className="nav-bar-right-cap" eventKey="HistoryContent">History</Nav.Link>
            </Nav.Item>
        </Nav>
    );
}