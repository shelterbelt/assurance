type AssuranceResultReason =
    | 'UNDETERMINED'
    | 'FILE_DIRECTORY_MISMATCH'
    | 'COMPARE_FAILED'
    | 'TARGET_DOES_NOT_EXIST'
    | 'SOURCE_DOES_NOT_EXIST'
    | 'FILE_NULL'
    | 'SYMBOLIC_LINK_MISMATCH';

export default AssuranceResultReason;
