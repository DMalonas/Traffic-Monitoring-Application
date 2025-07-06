###Threading, Messaging, and Coordination View###
#################################################

                                    +---------------------------------------------+
                                    |           Console Input Loop                |
                                    |---------------------------------------------|
                                    | - Reads user input in a loop                |
                                    | - Forwards "ELECTION"/"LEAVE" to ParkNode   |
                                    |                                             |
                                    | *Concurrency role:*                         |
                                    |   - Acts as the main entry point            |
                                    |   - Triggers concurrent subsystems (threads)|
                                    |   - Remains responsive to user commands     |
                                    |                                             |
                                    | *Concurrency tech used:*                    |
                                    |   - Scanner (blocking stdin I/O)            |
                                    |   - Runs on main thread                     |
                                    +---------------------+-----------------------+
                                                          |
                                                          v
                                    +---------------------+-----------------------+
                                    |                 ParkNode                    |
                                    |---------------------------------------------|
                                    | - Stores all shared state (neighbors, etc.) |
                                    | - Coordinates elections, communication      |
                                    | - Checks status of nodes                    |
                                    |                                             |
                                    | *Concurrency role:*                         |
                                    |   - Shared state manager                    |
                                    |   - Accessed by multiple threads            |
                                    |   - Handles race conditions manually        |
                                    |   - Manages distributed coordination        |
                                    |   - Starts multiple threads for I/O &       |
                                    |     monitoring                              |
                                    |   - Synchronizes state changes              |
                                    |     (e.g., coordinator updates)             |
                                    |                                             |
                                    | *Concurrency tech used:*                    |
                                    |   - Thread spawning                         |
                                    |     (via ParkNodeReceiver, NodeAliveChk)    |
                                    |   - Shared fields without synchronization   |
                                    |   - Uses sleep() to simulate delays         |
                                    |   - Hosts thread-launching methods          |
                                    +---------------------+-----------------------+
                                                          |
                                                          v
            +----------------------------+------------------------------+-----------------------------+
            |                            |                              |                             |
            v                            v                              v                             v
  +----------------------------+  +-----------------------------+ +-----------------------------+ +-----------------------------+
  | ParkNodeCommunicator       |  | ParkNodeReceiver            | | NodeAliveChecker            | | processIncomingMessage      |
  |----------------------------|  |-----------------------------| |-----------------------------| |-----------------------------|
  | - Sends socket messages    |  | - Accepts incoming sockets  | | - Sends heartbeats          | | - Routes incoming messages  |
  | - Starts receiver, checker |  | - Dispatches to handlers    | | - Monitors liveness         | | - Mutates ParkNode state    |
  | - Forwards messages        |  |                             | |                             | |                             |
  |                            |  |                             | |                             | |                             |
  | *Concurrency role:*        |  | *Concurrency role:*         | | *Concurrency role:*         | | *Concurrency role:*         |
  | - Launches two components  |  | - Blocking thread per node  | | - Background monitor        | | - Central switchboard       |
  | - Bridges I/O & logic      |  | - Handlers incoming mesages | | - Reacts to node failure    | | - Parses & dispatches data  |
  |                            |  |                             | |                             | |                             |
  | *Concurrency tech used:*   |  | *Concurrency tech used:*    | | *Concurrency tech used:*    | | *Concurrency tech used:*    |
  | - new Thread(...)          |  | - ServerSocket.accept()     | | - Thread.sleep(5000) loop   | | - Called inside I/O thread  |
  | - Socket I/O               |  | - Thread per connection     | | - new Thread(...)           | | - Accesses shared fields    |
  +----------------------------+  +-----------------------------+ +-----------------------------+ +-----------------------------+

                                                |
                                                v
                           +---------------------------------------------+
                           |    Shared Mutable State (inside ParkNode)   |
                           |---------------------------------------------|
                           | - coordinatorId, neighbors, queues, etc.    |
                           | - Used by many threads                      |
                           |                                             |
                           | *Concurrency role:*                         |
                           |   - Center of contention                    |
                           |   - Requires safe access strategies         |
                           |                                             |
                           | *(Non)/Concurrency tech used:*              |
                           |   - No locks/syncs used                     |
                           |   - Heavily dependent on ordering           |
                           +---------------------------------------------+

                                                |
                                                v
                           +---------------------------------------------+
                           |     SharedResourceUtilities (Static)        |
                           |---------------------------------------------|
                           | - Reads/writes to Shared/Resource.txt       |
                           | - Manages ticket IDs and capacity           |
                           |                                             |
                           | *Concurrency role:*                         |
                           |   - Global shared resource manager          |
                           |   - Vulnerable to race conditions           |
                           |                                             |
                           | *(Non)/Concurrency tech used:*              |
                           |   - BufferedReader/Writer (I/O blocking)    |
                           |   - No file-locking or atomicity            |
                           +---------------------------------------------+

                                   +--------------------------------+
                                  |  ClientFunctionalityClass       |
                                  |---------------------------------|
                                  | - Reads from ClientFile.txt     |
                                  | - Sends socket messages         |
                                  |                                 |
                                  | *Concurrency role:*             |
                                  |   - Autonomous thread           |
                                  |   - Not coordinated             |
                                  |                                 |
                                  | *(Non)/Concurrency tech used:*  |
                                  |   - new Socket(...)             |
                                  |   - PrintWriter, BufferedReader |
                                  +---------------------------------+
