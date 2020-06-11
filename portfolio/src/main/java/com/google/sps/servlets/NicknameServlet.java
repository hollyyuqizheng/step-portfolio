// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceConfig.Builder;  
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.sps.data.Util; 
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional; 

@WebServlet("/nickname")
public class NicknameServlet extends HttpServlet {

  private static final String NICKNAME = "nickname";
  private static final String USER_ID = "userId"; 

  // Constant for query type of UserInfo
  private static final String USER_INFO = "UserInfo";

  // Constant for redirect URLs. 
  private static final String NICKNAME_URL = "/nickname";
  private static final String INDEX_URL = "/index.html";

  // Global variable for an instance of DatastoreService
  private static DatastoreService datastore; 

  public NicknameServlet() {
    Util util = new Util();
    datastore = util.getDatastoreServiceWithConsistency(); 
  }

  /**
   * Handles GET request. This servlet will only be sent a request to if the logged in user 
   * does not yet have a nickname.
   */ 
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println("<h1>Set Nickname</h1>");

    UserService userService = UserServiceFactory.getUserService();

    out.println("<p>Set your nickname here:</p>");
    out.println("<form method=\"POST\" action=\"/nickname\">");
    out.println("<input name=\"nickname\" />");
    out.println("<br/>");
    out.println("<button>Submit</button>");
    out.println("</form>");
  }

  /**
   * Handles POST request by putting a nickname into Datastore and 
   * associate it with a user id. 
   */ 
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) {
      response.sendRedirect(NICKNAME_URL);
      return;
    }

    String nickname = request.getParameter(NICKNAME);
    String userId = userService.getCurrentUser().getUserId();

    Entity entity = new Entity(USER_INFO, userId);
    entity.setProperty(USER_ID, userId);
    entity.setProperty(NICKNAME, nickname);

    datastore.put(entity);

    response.sendRedirect(INDEX_URL);
  }
}
