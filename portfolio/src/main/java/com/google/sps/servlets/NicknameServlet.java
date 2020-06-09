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
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/nickname")
public class NicknameServlet extends HttpServlet {

  /**
   * Handles GET request. If the user is logged in, display a page for them to input 
   * nickname. This servlet will only be sent a request to if the logged in user 
   * does not yet have a nickname.
   */ 
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println("<h1>Set Nickname</h1>");

    UserService userService = UserServiceFactory.getUserService();
    String nickname = getUserNickname(userService.getCurrentUser().getUserId());
    out.println("<p>Set your nickname here:</p>");
    out.println("<form method=\"POST\" action=\"/nickname\">");
    out.println("<input name=\"nickname\" value=\"" + nickname + "\" />");
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
      response.sendRedirect("/nickname");
      return;
    }

    String nickname = request.getParameter("nickname");
    String id = userService.getCurrentUser().getUserId();

    DatastoreService datastore = getDatastoreServiceWithConsistency();
    Entity entity = new Entity("UserInfo", id);
    entity.setProperty("id", id);
    entity.setProperty("nickname", nickname);

    datastore.put(entity);

    response.sendRedirect("/index.html");
  }

  /**
   * Returns the nickname of the user with id, or empty String if the user has not set a nickname.
   */
  private String getUserNickname(String id) {
    DatastoreService datastore = getDatastoreServiceWithConsistency();
    Query query =
        new Query("UserInfo")
            .setFilter(new Query.FilterPredicate("id", Query.FilterOperator.EQUAL, id));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();
    if (entity == null) {
      return "";
    }
    String nickname = (String) entity.getProperty("nickname");
    return nickname;
  }

  /**
   * Sets up strong consistency for datastore service. 
   * This Strong Consistency ensures that freshness is more important than availability
   * so that the most up-to-date data is returned and displayed on the page.
   */ 
  private DatastoreService getDatastoreServiceWithConsistency() { 
    DatastoreServiceConfig datastoreConfig = 
        DatastoreServiceConfig.Builder.withReadPolicy(new ReadPolicy(Consistency.STRONG)).deadline(5.0);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService(datastoreConfig);
    return datastore; 
  }
}
