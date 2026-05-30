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
