# Requirements

## APIs
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
- metadata
  - Returns a list of user - ip:port 

## Architecture
- UsersMetadataRepository
  - Holds mappings of users metadata
- Listener (Registers a socket server and handles connections)
  - ConnectionHandler (Handles socket input and output through commands)
  - MessageMediator (Mediates messages between connections)
  - CommandProcessor (Processes commands and calls the needed services)
  - ActiveConnections (Handles the active connections)
- ConsoleInputListener
  - Captures console inputs
