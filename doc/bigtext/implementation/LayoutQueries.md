# Layout Queries

## Definition of Row Index

- 0: The first row to the user. Start from the beginning of the string.
- 1: The 2nd row to the user. Start from the 1st (index = 0) row break.
- 2: The 3rd row to the user. Start from the 2nd (index = 1) row break.
- 3: The 4th row to the user. Start from the 3rd (index = 2) row break.

### Example (Test case `BigTextImplLayoutTest#insertTriggersRelayout1`)

For a string `"abcd\nABCDEFGHIJ<BCDEFGHIJ<xyz\nabcd"` and chunk size is `10`, mapping is as follows:

| Char Index | Row Index | Line Index |
|------------|-----------|------------|
| 0          | 0         | 0          |
| 5          | 1         | 1          |
| 15         | 2         | 1          |
| 25         | 3         | 1          |
| 30         | 4         | 2          |


## `findLineIndexByRowIndex`

### Locate the interested node

#### Example

Assume we want to locate the node containing row index 3.

Case 1: The node contains the 3rd row break.

Case 2: The node is the last node, contains the 2nd row break and "is end with force row break".

#### General algorithm

Assume we want to locate the node containing row index `x`.

Case 1: The node contains the `x`-th row break.

Case 2: The node is the last node, contains the `(x - 1)`-th row break and "is end with force row break".

Case 3: The node is the first node and `x` is 0. `x` is ahead of all row breaks.
