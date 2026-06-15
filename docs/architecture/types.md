# Type Model

Initial workflow data types should stay small and composable.

Suggested primitives:
- `String`
- `Int`
- `Double`
- `Boolean`

Recommendation:
- Prefer `Double` as the default decimal type.
- Keep `Float` out of the first public model unless a concrete use case appears.

Suggested containers:
- `List<T>`
- `Nullable<T>`
- `Record`
- `Enum`
- `Opaque`

Notes:
- Prefer generic composition over inventing a new primitive for every node family.
- `List<T>` should preserve the element type so downstream validation stays type-safe.
- `Nullable<T>` should wrap any type, not just primitives.
