/**********
*
* Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
***********/
package smartpath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HttpCodeDownloadHandler implements HttpHandler
{
	@Override
	public void handle(HttpExchange t) throws IOException {
	    String root = "/tmp";
	    URI uri = t.getRequestURI();
	    String location = (uri.getPath());
	    System.out.println( root + location);
	    File file = new File(root + location).getCanonicalFile();
	    if (!file.getPath().startsWith(root)) {
	      // Suspected path traversal attack: reject with 403 error.
	      String response = "403 (Forbidden)\n";
	      t.sendResponseHeaders(403, response.length());
	      OutputStream os = t.getResponseBody();
	      os.write(response.getBytes());
	      os.close();
	    } else if (!file.isFile()) {
	      // Object does not exist or is not a file: reject with 404 error.
	      String response = "404 (Not Found)\n";
	      t.sendResponseHeaders(404, response.length());
	      OutputStream os = t.getResponseBody();
	      os.write(response.getBytes());
	      os.close();
	    } else {
	      // Object exists and is a file: accept with response code 200.
	      t.sendResponseHeaders(200, 0);
	      OutputStream os = t.getResponseBody();
	      FileInputStream fs = new FileInputStream(file);
	      final byte[] buffer = new byte[0x10000];
	      int count = 0;
	      while ((count = fs.read(buffer)) >= 0) {
	        os.write(buffer,0,count);
	      }
	      fs.close();
	      os.close();
	    }
	  }
}