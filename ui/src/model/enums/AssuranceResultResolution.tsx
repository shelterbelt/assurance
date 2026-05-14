type AssuranceResultResolution =
    | 'UNRESOLVED'
    | 'REPLACE_SOURCE'
    | 'REPLACE_TARGET'
    | 'DELETE_SOURCE'
    | 'DELETE_TARGET'
    | 'PROCESSING_ERROR_ENCOUNTERED';

export default AssuranceResultResolution;
