package pathstore.system;

/**
 * These are the statuses that the pathstore_applications.node_schemas table uses. The cycle for
 * starting is
 *
 * <p>WAITING_INSTALL -> INSTALLING -> INSTALLED which causes the next node in the sequence to go to INIT if
 * applicable
 *
 * <p>The cycle for removing is
 *
 * <p>WAITING_REMOVE -> REMOVING -> REMOVED
 */
enum ProccessStatus {
  WAITING_INSTALL,
  INSTALLING,
  INSTALLED,
  WAITING_REMOVE,
  REMOVING,
  REMOVED
}
