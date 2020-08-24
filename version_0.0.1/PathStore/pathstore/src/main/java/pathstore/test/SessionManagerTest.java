package pathstore.test;

import pathstore.sessions.PathStoreSessionManager;
import pathstore.sessions.SessionToken;

import java.io.IOException;

public class SessionManagerTest {
  public static void main(String[] args) throws IOException {
    PathStoreSessionManager.init("/home/myles/Documents/sessionFile.txt");

    SessionToken testSessionToken =
        PathStoreSessionManager.getInstance().getTableToken("test-session");

    testSessionToken.addEntry("a");
    testSessionToken.addEntry("b");
    testSessionToken.addEntry("c");
    testSessionToken.addEntry("d");

    SessionToken testSessionToken1 =
        PathStoreSessionManager.getInstance().getTableToken("test-session-1");

    testSessionToken1.addEntry("e");
    testSessionToken1.addEntry("f");
    testSessionToken1.addEntry("g");
    testSessionToken1.addEntry("h");

    PathStoreSessionManager.getInstance().close();
  }
}
