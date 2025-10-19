# Requirements

### Commands
- register
  - Attributes
    - username
    - a set of files
  - Adds these files to the user from the map.
  - Cases
    - If user exists
      - If some file exists, ignore and throw a warning
    - If user doesn't exist, throw an error
- unregister
  - Attributes
    - username
    - a set of files
  - Removes these files from the user
  - Cases
    - If the user exists
      - If some file doesn't exist, throw a warning (same for duplicates)
    - If the user doesn't exist, throw an error
- list-files
  - Returns a list of user - file (ordered by user)

### Architecture
- Processor
  - Resolves commands and calls the needed services
- UsersMetadataRepository
  - Holds mappings of users metadata
- Listener
  - The main listener that adds new connections and handles server state
- ConsoleInputListener
  - Captures console inputs
- EventDispatcher
  - Handles all types of operations within it.

