# File Transfer Server .v1
This version of a FTS is a simple implementation with synchronous workflow. The idea is to mend it into the current architecture and initiate simple File Transfers.

1. The server listens on port 8021 (This is constant)
2. Clients find the host, connect via the constant port.

# Workflow
1. Client 1 connects to client 2's host:8021
2. Client 2 accepts and awaits for data
3. Client 1 calls "download {file}"
4. Client 2 responds:
   * If file is available - OK {filesize}
   * Else - ERROR {message}
5. Client processes response:
   * startsWith OK - parse integer
     * If integer is malformed, close connection
   * startsWith ERROR - LOG and close connection
6. Saves data in .tmp file and then re-creates it with new name.

The connection might fail at any time, so this must be considered.
