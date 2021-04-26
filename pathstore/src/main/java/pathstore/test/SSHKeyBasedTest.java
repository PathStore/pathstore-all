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

import com.jcraft.jsch.JSchException;
import pathstore.system.deployment.utilities.SSHUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// host, username, port, priv key location, passphrase
public class SSHKeyBasedTest {
  public static void main(String args[]) throws IOException, JSchException {
    if (args.length <= 4) {
      System.out.println(
          "arg 1 is host, arg 2 is username, arg 3 is port, arg 4 is priv key location, arg 5 is passphrase (optional)");
      return;
    }

    File privKey = new File(args[3]);

    byte[] file = Files.readAllBytes(privKey.toPath());

    SSHUtil sshUtil =
        new SSHUtil(
            args[0], args[1], Integer.parseInt(args[2]), file, args.length == 5 ? args[4] : null);

    sshUtil.execCommand("docker ps");

    sshUtil.disconnect();
  }
}
