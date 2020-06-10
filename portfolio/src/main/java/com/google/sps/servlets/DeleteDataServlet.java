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
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List; 
import java.util.ArrayList; 

/** Servlet responsible for deleting all quotes in Datastore. */
@WebServlet("/deleteQuote")
public class DeleteDataServlet extends HttpServlet {

  private static final String QUOTE = "Quote";
  private static final String USER_EMAIL = "userEmail";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    String userEmail = userService.getCurrentUser().getEmail(); 

    DatastoreService datastore = getDatastoreServiceWithConsistency();
    Query query = new Query(QUOTE);
    PreparedQuery quotes = datastore.prepare(query);

    // Only delete quotes that were submitted by the currently logged in user. 
    List<Key> keyList = new ArrayList<Key>();
    quotes.asIterable()
        .forEach((quoteEntity) -> {
          if (quoteEntity.getProperty(USER_EMAIL).equals(userEmail)) {
            keyList.add(quoteEntity.getKey());
          } 
        });

    datastore.delete(keyList);
  }

  /**
   * Sets up strong consistency for datastore service. 
   * This strong consistency ensures that freshness is more important than availability
   * so that the most up-to-date data is returned and displayed on the page.
   */ 
  private DatastoreService getDatastoreServiceWithConsistency() { 
    DatastoreServiceConfig datastoreConfig = 
        DatastoreServiceConfig.Builder.withReadPolicy(new ReadPolicy(Consistency.STRONG)).deadline(5.0);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService(datastoreConfig);
    return datastore; 
  }
}
