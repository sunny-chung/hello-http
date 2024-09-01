# Text Value Mutation Callback

## Method 1: Receive via Flow

Listen to changes via reactive `Flow`. Convert `BigText` back to `String` on every event. Use a `debounce` operator to reduce number of conversions.

### Advantage
- Clear control of expensive operations

### Disadvantage
- Need to maintain a unique cache key per text instance.
- The expensive `BigText#fullString` is invoked frequently (but the interval is controllable).
- Result is not immediately obtained, which may be a trouble in state synchronization.

```kotlin
val bigTextFieldState = rememberBigTextFieldState(cacheKey, textValue.text)
val scrollState = rememberScrollState()

bigTextFieldState.valueChangesFlow
    .debounce(1.seconds().toMilliseconds())
    .onEach {
        onTextChange?.let { onTextChange ->
            onTextChange(it.bigText.fullString())
        }
        bigTextValueId = it.changeId
    }
    .launchIn(CoroutineScope(Dispatchers.Main))

BigMonospaceTextField(
    textFieldState = bigTextFieldState,
    visualTransformation = visualTransformationToUse,
    fontSize = LocalFont.current.codeEditorBodyFontSize,
    scrollState = scrollState,
    modifier = Modifier.fillMaxSize()
)
```
## Method 2: Wrap BigText as CharSequence

### Advantage
- Able to keep the declarative code pattern
- The actual value is immediately available when it is needed
- The expensive `BigText#fullString` is never invoked if it is not needed

### Disadvantage
- Need to change the usage of String type in models and composables to CharSequence (or BigText).
- The expensive `BigText#fullString` may be invoked (via `BigTextAsCharSequence#toString`) out of control.
- Lots of code changes to an existing code base, hence a significant risk to existing projects.

```kotlin
val bigTextValue = BigText.wrap(newText)
val scrollState = rememberScrollState()
val bigTextViewState = remember(bigTextValue) { BigTextViewState() }

BigMonospaceTextField(
    text = bigTextValue,
    onTextChange = {
        onTextChange?.let { onTextChange ->
            onTextChange(it.bigText.asCharSequence())
        }
        bigTextValueId = it.changeId
    },
    viewState = bigTextViewState,
    visualTransformation = visualTransformationToUse,
    fontSize = LocalFont.current.codeEditorBodyFontSize,
    scrollState = scrollState,
    onTextLayout = { layoutResult = it },
    modifier = Modifier.fillMaxSize()
)
```

