# Planning

## Todo object

### Derived

- Location
- Author
- Origin Date

### Supplied

- Name
- Description
- Tag List

### Modified

- Completion Date

### History

#### Derived

- Timestamp
- Author

#### Supplied

- Comment

## Tag Object

- Name
- Namespace

## Commands

- Todo
  - Add \[name] \[description] \[tags]
  - Edit \[name]
    - Complete \ Reactivate
    - Description
    - Tags
      - Set \[tags]
      - Add \[tags]
      - Remove \[tags]
    - Work \[Comment]
  - Get
    - --near \[Chunk Radius, default = 8]
    - --tags \[Tag List DSL]
    - --name \[Name]
    - --author \[Author]
    - --time \[created/worked/completed] \[operator] \[Date]
  - Remove \[name]
  - Tags
    - List
    - Add \[Namespace] \[Tagname]
    - Remove \[Tagname]
    - Rename \[Tagname] \[New Name]
    - Namespaces
      - List
      - Add \[Namespace]
      - Remove \[Namespace]
      - Rename \[Namespace] \[New Name]

---

# Design Decisions (2026-06-07)

## Feature 1: Namespace Cardinality (`allows_multiple`)

### Concept

Each namespace gets a boolean column `allows_multiple`. If `true`, a todo can have
multiple tags from that namespace at once. If `false`, only one tag at a time.

### Decision 1: Column type and default

- **Chose**: `bool("allows_multiple").default(true)` – Exposed maps this to integer
  0/1 in SQLite. Default `true` is backwards-compatible: existing namespaces stay
  multi-tag until explicitly changed.

### Decision 2: Enforcement scope

**Option A**: Only enforce single-tag for the scope namespace (minimal change).
**Option B**: Enforce single-tag for *all* namespaces marked `allows_multiple=false`.

**Chose B** – A namespace toggle that has no effect outside the scope context would
be confusing UX. If a user marks a namespace as single-tag, TagManager should
prevent assigning a second tag from that namespace to the same todo. The scope
mechanism is the *primary* consumer but not the only one.

### Decision 3: TodoScopeManager constraint

- `TodoScopeManager.load()` now additionally validates that the configured scope
  namespace has `allows_multiple = false`. If it does not, scope resolution is
  disabled with a warning.

---

## Feature 2: Query Profiles

### Concept

A query profile stores a `TodoFilter` snapshot under a name. The user can save
the current filter, list saved profiles, delete them, and apply one to replace
the active filter.

### Decision 1: Storage format

**Option A**: Serialize `TodoFilter` to JSON, store in a text column.
**Option B**: Individual columns for each filter field.

**Chose A** (JSON via Gson) – The `TodoFilter` is a nested data class. Storing
individual columns would require a join table for included/excluded tag lists and
complex migrations every time the filter gains a field. JSON is extensible and
human-debuggable. Gson is already a project dependency.

### Decision 2: Table design

```sql
QueryProfiles(id INT PK, name VARCHAR(64) UNIQUE, filter_json TEXT)
```

Single table, no foreign keys needed – tag IDs stored in JSON are opaque;
deleted tags simply produce no matches.

### Decision 3: Command structure

```
/todo profiles
├── list                          – show all saved profiles
├── create <name>                 – create an empty query profile
├── delete <name>                 – delete a saved profile
├── apply <name>                  – open TodoListInterface with the loaded filter
```

`create` produces an empty filter profile; the user then configures it from the
Filters UI and uses "Save as Profile" to fill in the filter state.

### Decision 4: UI location

New interface `ProfileListInterface` accessible from:

- `FiltersInterface` via a "Query Profiles" button
- The "Save as Profile" button in `FiltersInterface` captures the current filter

The profile list shows profile names with filter summaries. Clicking a profile
applies it and opens the todo list.
