import { Nav } from "react-bootstrap"

interface NavBarProps {
    disabled: boolean;
}

export default function NavBar({disabled}: NavBarProps) {
    return (
        <Nav variant="tabs" className="nav-bar mx-auto" justify>
            <Nav.Item>
                <Nav.Link disabled={disabled} className="nav-bar-left-cap" eventKey="ScanContent">Scan</Nav.Link>
            </Nav.Item>
            <Nav.Item>
                <Nav.Link disabled={disabled} className="nav-bar-right-cap" eventKey="HistoryContent">History</Nav.Link>
            </Nav.Item>
        </Nav>
    );
}