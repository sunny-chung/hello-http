# Transform Replaces

## Block-offset transforms

- Transformed positions are never mapped from original positions
- New transform inserts to the same position of existing block-offset transforms would append the new text to the end of it

### `simpleBlockTransformReplaces` first test case

```mermaid
block-beta
    columns 20
    40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59

    O40["<"] O41["r"] O42["o"] O43["w"] O44[" "] O45["b"] O46["r"] O47["e"] O48["a"] O49["k"] O50["<"] O51[" "] O52["s"] O53["h"] O54["o"] O55["u"] O56["l"] O57["d"] O58[" "] O59["h"]
    space:20
    T40["<"] T41["r"] T42["o"] T43["w"] T44[" "] T45["-"] T46["+"] T47["-"] T48["+"] T49["-"] T50["h"] T51["o"] T52["u"] T53["l"] T54["d"] T55[" "] T56["h"] T57["<"] T58["a"] T59["p"]
    space:20
    o40["<"] o41["r"] o42["o"] o43["w"] o44[" "] o45["b"] o46["r"] o47["e"] o48["a"] o49["k"] o50["<"] o51[" "] o52["s"] o53["h"] o54["o"] o55["u"] o56["l"] o57["d"] o58[" "] o59["h"]
    
    style O45 fill:#d00
    style O46 fill:#d00
    style O47 fill:#d00
    style O48 fill:#d00
    style O49 fill:#d00
    style O50 fill:#d00
    style O51 fill:#d00
    style O52 fill:#d00
    
    style T45 fill:#060
    style T46 fill:#060
    style T47 fill:#060
    style T48 fill:#060
    style T49 fill:#060

    style o45 fill:#d00
    style o46 fill:#d00
    style o47 fill:#d00
    style o48 fill:#d00
    style o49 fill:#d00
    style o50 fill:#d00
    style o51 fill:#d00
    style o52 fill:#d00
    
    O43-->T43
    O44-->T44
    O53-->T50
    O54-->T51
    
    O45 --> T50
    O46 --> T50
    O47 -.-> T50
    O48 -.-> T50
    O49 -.-> T50
    O50 -.-> T50
    O51 -.-> T50
    O52 -.-> T50
    
    T43 --> o43
    T44 --> o44
    T50 --> o53
    T51 --> o54
    
    T45 --> o45
    T46 --> o45
    T47 --> o45
    T48 --> o45
    T49 --> o45
```

## Incremental-offset transforms

- Transformed positions are mapped 1-to-1 from original positions, until the earliest end of either one
- New transform inserts to the same position of existing incremental-offset transforms would append the new text to the start of it

### `mixedTransformReplaces` first test case

```mermaid
block-beta
    columns 20
    30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49

    O30["<"] O31["B"] O32["C"] O33["D"] O34["E"] O35["F"] O36["G"] O37["H"] O38["I"] O39["J"] O40["<"] O41["r"] O42["o"] O43["w"] O44[" "] O45["b"] O46["r"] O47["e"] O48["a"] O49["k"]
    space:20
    space:20
    T30["<"] T31["B"] T32["l"] T33["o"] T34["n"] T35["g"] T36[" "] T37["r"] T38["e"] T39["p"] T40["l"] T41["a"] T42["c"] T43["e"] T44["m"] T45["e"] T46["n"] T47["t"] T48["H"] T49["I"]
    space:20
    o30["<"] o31["B"] o32["C"] o33["D"] o34["E"] o35["F"] o36["G"] o37["H"] o38["I"] o39["J"] o40["<"] o41["r"] o42["o"] o43["w"] o44[" "] o45["b"] o46["r"] o47["e"] o48["a"] o49["k"]
    
    style O32 fill:#d00
    style O33 fill:#d00
    style O34 fill:#d00
    style O35 fill:#d00
    style O36 fill:#d00
    style T32 fill:#060
    style T33 fill:#060
    style T34 fill:#060
    style T35 fill:#060
    style T36 fill:#060
    style T37 fill:#060
    style T38 fill:#060
    style T39 fill:#060
    style T40 fill:#060
    style T41 fill:#060
    style T42 fill:#060
    style T43 fill:#060
    style T44 fill:#060
    style T45 fill:#060
    style T46 fill:#060
    style T47 fill:#060
    
    O30-->T30
    O31-->T31
    O37-->T48
    O38-->T49
    
    O32 -.-> T32
    O33 -.-> T33
    O34 -.-> T34
    O35 -.-> T35
    O36 -.-> T36
```

## Transform inserts

Transform inserts behaves the same as block-offset transform replaces without deletions.
