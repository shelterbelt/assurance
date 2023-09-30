import { ListGroup } from "react-bootstrap"

import ResultItemRenderer from '../result-item-renderer/ResultItemRenderer';

export default function ResultsPanel({results}) {

    function handleResultItemSelcted() {
        console.log("Result item selected");
    }

    function handleMergeButtonClicked(mergeOption) {
        console.log("Merge button clicked: " + mergeOption);
    }
    
    function loadResults(results) {
        const listItems = results.map(result => 
            <ResultItemRenderer key={result.id} result={result} onResultItemSelected={handleResultItemSelcted} onMergeButtonClicked={handleMergeButtonClicked}></ResultItemRenderer>
        );
        return (<>{listItems}</>);
    }

    return (
        <div className="results-panel-container">
            <ListGroup className="results-list-group">
                {loadResults(results)}
            </ListGroup>
        </div>
    );
}
