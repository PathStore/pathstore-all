# Schema Loader

This is a brief explanation of how the schema loading process.

This will not explain the initial state generation algorithm but the algorithm for state transition

The table below is an example of a network where 1 <- 2 <- 3 where '<-' represents a parent pointer

| nodeid | keyspace_name | process_status | wait_for | process_id |
| -----: |:-------------:| :------------- | -------: | :--------: |
| 1      | demo          | WAITING        | -1       | UUID       |
| 2      | demo          | WAITING        | 1        | UUID       |
| 3      | demo          | WAITING        | 2        | UUID       |

## Master Schema Loader algorithm

First we query all the rows. From this we will get the latest state of the tuple of (nodeid, keyspace_name).

This data is stored inside a map of keyspace_name to a set of rows that are relevant to this keyspace operation.

Then we iterate over each set and parse the data into a set of WAITING entries and INSTALLED entries.

The condition for the state transition is if we have a node at the waiting state and its wait_for value is -1 or W if it is -1 we transition this node to INSTALLING.

If it is waiting for a node W and W is in our pre-determined INSTALLED set then we can transition this node to INSTALLING.

## Slave Schema Loader algorith

Currently we query all rows in this table. If the nodeid is the current node id of the node that the slave daemon is running on. Then we check to see if the state is INSTALLING. If it is then we install said keyspace and then we transition the state to INSTALLED. Which will move the next node in the chain, if any to the INSTALLING state.

