# Code Practices

## Naming and State
- Name booleans with the `isXxx` / `hasXxx` pattern (e.g. `isLoading`, `isEditing`). Avoid bare adjectives or verbs.
- Prefer immutable `val` variables. Mutability (`var`) should be justified by local state needs.

## UI Construction
- We avoid Material 2/3 dependencies. Use foundation primitives (`BasicText`, `BasicTextField`, `Row`, `Column`, etc.) and our own components (`AppTextField`, `AppTextButton`, etc.). Prefer our own components over foundation components.
- Visit the package `com.sunnychung.application.multiplatform.hellohttp.ux` for reusable components.
- Before adding a library, verify it does **not** depend on Material. If it does, do not add it.
- Extract reusable UI patterns into dedicated composables (e.g. a styled `AppTextField` should live in a helper composable, not inline in many places).
- Keep composables layout-agnostic: always expose a `Modifier` parameter instead of relying on `BoxScope`/`ColumnScope` specific APIs.
- Leverage `verticalArrangement` and `horizontalArrangement`, plus padding modifiers, instead of inserting many `Spacer`s.
- Keep composables small and focused. Pass only the data they require (prefer `data class` props over entire state objects).
- Always use explicit imports (`import com.sunnychung...`). Wildcard imports (`*`) are forbidden.
- Reuse colors from `AppColor` (accessible via `LocalColor.current`). If new colors needed to be added, modify all color themes in the `AppColor` file.

## General Guidance
- Prefer simple, robust solutions over clever but fragile ones. If a straightforward approach works without bugs, choose it.
- Keep logging concise and meaningful (`log.v/d/i/w/e`). Avoid leaking PII or noisy stack traces.
- Commit Kotlin formatting conventions: 4-space indentation, trailing commas where Kotlin style encourages, and idiomatic use of `when`, null-safety, and collection helpers.
- Ensure new code includes tests or manual verification steps when touching networking or state synchronization. Document those steps in PRs.
