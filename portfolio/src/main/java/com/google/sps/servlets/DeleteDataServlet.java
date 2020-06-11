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

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions; 
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceConfig.Builder;  
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.sps.data.Util; 
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List; 
import java.util.ArrayList; 
import java.util.Optional;

/** Servlet responsible for deleting all quotes in Datastore. */
@WebServlet("/deleteQuote")
public class DeleteDataServlet extends HttpServlet {

  private static final String PROPERTY_NAME_QUOTE = "Quote";
  private static final String PROPERTY_NAME_USER_EMAIL = "userEmail";

  // Global variable for an instance of the Util helper class
  private static Util util; 
  
  // Global variable for an instance of DatastoreService
  private static DatastoreService datastore; 

  public DeleteDataServlet() {
    util = new Util();
    datastore = util.getDatastoreServiceWithConsistency(); 
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    String userEmail = userService.getCurrentUser().getEmail(); 

    Query query = new Query(PROPERTY_NAME_QUOTE);
    PreparedQuery quotes = datastore.prepare(query);

    // Only delete quotes that were submitted by the currently logged in user. 
    List<Key> keyList = new ArrayList<Key>();
    quotes.asIterable()
        .forEach((quoteEntity) -> {
          if (quoteEntity.getProperty(PROPERTY_NAME_USER_EMAIL).equals(userEmail)) {
            keyList.add(quoteEntity.getKey());
          } 
        });

    datastore.delete(keyList);
  }
}
