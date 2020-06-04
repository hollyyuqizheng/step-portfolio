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

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import java.util.List; 
import java.util.ArrayList; 

/** Servlet that returns some example content. */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  /**
    * This method retrives all quotes that are stored in Datastore
    * and puts them into a list of quotes. This list is then written
    * as the response from the server. 
    */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query("Quote").addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery quotes = datastore.prepare(query);

    // Get the parameter for number to display, with 5 as the default value.
    int numQuoteToDisplay = getNumQuoteDispalyed(request, 5);

    // Add each quote to the quote list whose content will be written as 
    // the response from the servlet. 
    List<String> quoteList = new ArrayList<>();
    int quoteCount = 1; 
    for (Entity quoteEntity : quotes.asIterable()) {
      if (quoteCount > numQuoteToDisplay) {
        break; 
      }
      String quoteText = (String) quoteEntity.getProperty("text");
      quoteList.add(quoteText);
      quoteCount++; 
    }

    String quotesJson = convertToJson(quoteList);
    response.setContentType("text/html;");
    response.getWriter().println(quotesJson);
  }

  /**
   * This method receives the quote that the user has inputed in the survey bar.
   * This new quote is changed into an Entity that is put into Datastore.
   * Each quote entity has a "text" and a "timstamp" field. 
   */ 
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    String quote = request.getParameter("quote"); 

    // Only update the new quote if it is not an empty string. 
    if (quote.length() > 0) {
      long timestamp = System.currentTimeMillis();

      Entity quoteEntity = new Entity("Quote");
      quoteEntity.setProperty("text", quote);
      quoteEntity.setProperty("timestamp", timestamp);

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      datastore.put(quoteEntity);
    }
    
    response.sendRedirect("/index.html");
  }



  /**
   * This method handles the returning of request parameter. 
   * @param request: the request from the survey
            name: the request parameter, which corresponds to a specific entry of the survey
            defaultValue: a default value for this survey entry's input
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */ 
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }



  /**
   * This function takes in an ArrayList of quotes and converts it into a string 
   * that is in JSON format. 
   * @param a list of quotes
   * @return the quote list formated as a JSON string
   */ 
  private String convertToJson(List<String> quotes) {  
    Gson gson = new Gson();
    String json = gson.toJson(quotes);
    return json;
  }

  /**
   * This method reads the numToDisplay parameter that is set by the user from the survey bar. 
   * @param request: the request; defaultNum: default number to display set in doGet method. 
   * @return number of quotes to display 
   */ 
  private int getNumQuoteDispalyed(HttpServletRequest request, int defaultNumToDisplay) {
    int numDisplay;
    String numQuoteDisplayParam = request.getParameter("numToDisplay");

    try {
      numDisplay = Integer.parseInt(numQuoteDisplayParam);
    } catch (NumberFormatException e) {
      numDisplay = defaultNumToDisplay; 
    }
    return numDisplay; 
  }
}
