/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package pathstore.test;

import pathstore.sessions.PathStoreSessionManager;
import pathstore.sessions.SessionToken;

import java.io.IOException;

public class SessionManagerTest {
  public static void main(String[] args) throws IOException, InterruptedException {

    SessionToken testSessionToken =
        PathStoreSessionManager.getInstance().getTableToken("test-session");

    testSessionToken.addEntry("a");
    testSessionToken.addEntry("b");
    testSessionToken.addEntry("c");
    testSessionToken.addEntry("d");

    PathStoreSessionManager.getInstance().swap();

    System.out.println("Check file now");
    Thread.sleep(10000);

    SessionToken testSessionToken1 =
        PathStoreSessionManager.getInstance().getTableToken("test-session-1");

    testSessionToken1.addEntry("e");
    testSessionToken1.addEntry("f");
    testSessionToken1.addEntry("g");
    testSessionToken1.addEntry("h");

    PathStoreSessionManager.getInstance().close();
  }
}
