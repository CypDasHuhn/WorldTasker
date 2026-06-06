# Further Requirements

## shared description

### Change material

shared Change material interface. 3 rows, the middle slot players can put an item into. this slot needs to set cancelEvent to false.
at the bottom right there is a 'save' button, which invokes the hook and leads back to before.

### Chat input

When chat input is made, the next thing typed is input for the chat, and enters you back into the interface

## List view

- list of todos
- item: player head of author
  - on click, move to detail view
- filters button
  - on click, move to filter view
- new todo button
  - chat input for todo name
  - followed by chat input for todo description
- random todo (within query)
  - on click, move to detail view of random todo from the list (considers filters too)
- down/up scrollers

## Detail view

- jump to location button
- rename button
  - require confirmation (rooster-ui confirmation interface)
  - input new name in chat
- delete button
  - require confirmation
- history button
- tag button
  - lore already shows tags + inherrited ones
  - on click, moves you to namespace view assigning mode

### History view

- list of history entries
- work button (chat input for comment)
- complete button
- scroller

## Filters view

- tag filter button
  - on click, move to namespace view query mode
- cycle item, cycles between Default, All, Completed (Todo work status)
- author filter button
  - on click, move to author view query mode
- distance filter button (shift click to toggle filter on off, left to up by 1, right to down by 1)

## Author view

- list of players
  - item: player head of author
  - query mode: on click, toggle between include, exclude, neutral

## namespace overview

- each mode (edit, query, assign like in tags overview) is a sub interface, we have a core parent for shared logic.
- current namespace interface is only query mode. "modes" arent part of context, they are different interfaces. the same logic goes for the tags view.

- List of namespaces
- item: namespace material. db default = bookshelf
  - on click, move to detail view (Tag Overview)

## Tag overview

- list of tags
  - item: tag material, db default = paper
  - on click:
    - edit mode: move to tag detail view
    - query mode: State toggle (include, exclude, neutral)
    - assigning mode: state toggle (on/off)
- edit mode
  - add tag button
  - delete namespace button (requires confirmation)
  - rename namespace button (requires confirmation, chat input)

## Tag detail view

- change material button
- delete button (requires confirmation)
- rename (requires confirmation, chat input)
- inheritance button
- on click, move to inheritance detail view
- list of inherited tags
- add (inheritance) tag button
  - moves to assigning mode namespace overview

## Db required changes

- namespace has material (default bookshelf)
- tag has material (default paper)
- history references player id, using rooster-sql PlayerManager
