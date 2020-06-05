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
import com.google.appengine.api.datastore.FetchOptions; 
import com.google.appengine.api.datastore.FetchOptions.Builder; 
import com.google.gson.Gson;
import com.google.sps.data.Quote; 
import java.util.List; 
import java.util.ArrayList; 

/** Servlet that returns some example content. */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private static final String QUOTE = "Quote";
  private static final String TEXT = "text";
  private static final String TIMESTAMP = "timestamp"; 
  private static final int DEFAULT_NUM_QUOTES = 5;

  /**
    * Retrives all quotes that are stored in Datastore
    * and puts them into a list of quotes. This list is then written
    * as the response from the server. 
    */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query(QUOTE).addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery quotes = datastore.prepare(query);

    // Get the parameter for number to display, with 5 as the default value.
    int numQuoteToDisplay = getNumQuoteDisplayed(request, DEFAULT_NUM_QUOTES);
   
    // Add each quote to the quote list whose content will be written as 
    // the response from the servlet. 
    List<Quote> quoteList = new ArrayList<>();
    quotes.asList(FetchOptions.Builder.withLimit(numQuoteToDisplay))
        .forEach((quoteEntity) -> {
          quoteList.add(extractQuoteFromEntity(quoteEntity));
        });
    
    // Put the thread to sleep for a short period of time.
    // This adds buffer time for querying Datastore. 
    try {
      Thread.sleep(200);
    } catch (Exception e) {
      System.out.println(e);
    }

    String quotesJson = convertToJson(quoteList);
    response.setContentType("application/json");
    response.getWriter().println(quotesJson); 
  }

  /**
   * Receives the quote that the user has inputed in the survey bar.
   * This new quote is changed into an Entity that is put into Datastore.
   * Each quote entity has a "text" and a "timstamp" field. 
   */ 
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    String quote = request.getParameter("quote");

    // Only update the new quote if it is not an empty string. 
    if (quote.length() > 0) {
      long timestampMillis = System.currentTimeMillis();

      Entity quoteEntity = new Entity(QUOTE);
      quoteEntity.setProperty(TEXT, quote);
      quoteEntity.setProperty(TIMESTAMP, timestampMillis);

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      datastore.put(quoteEntity);
    }
    
    response.sendRedirect("/index.html");
  }

  /**
   * Extracts the attributes of a quoteEntity and puts them into a new
   * instance of Quote. 
   */ 
  private Quote extractQuoteFromEntity(Entity quoteEntity) {
    long id = quoteEntity.getKey().getId();
    String text = (String) quoteEntity.getProperty(TEXT);
    long timestampMillis = (long) quoteEntity.getProperty(TIMESTAMP);
    Quote newQuote = new Quote(id, text, timestampMillis);
    return newQuote; 
  }

  /**
   * Handles the returning of request parameter. 
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
   * Takes in an ArrayList of quotes and converts it into a string 
   * that is in JSON format. 
   * @param a list of quotes
   * @return the quote list formated as a JSON string
   */ 
  private String convertToJson(List<Quote> quoteList) {  
    Gson gson = new Gson();
    String json = gson.toJson(quoteList);
    return json;
  }

  /**
   * Reads the numToDisplay parameter that is set by the user from the survey bar. 
   * @param request: the request; defaultNum: default number to display set in doGet method. 
   * @return number of quotes to display 
   */ 
  private int getNumQuoteDisplayed(HttpServletRequest request, int defaultNumToDisplay) {
    String numQuoteDisplayParam = request.getParameter("numToDisplay");

    try {
      return Integer.parseInt(numQuoteDisplayParam);
    } catch (NumberFormatException e) {
      return defaultNumToDisplay; 
    }
  }
}
