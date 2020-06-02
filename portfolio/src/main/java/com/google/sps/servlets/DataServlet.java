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
import com.google.gson.Gson;
import java.util.List; 
import java.util.ArrayList; 

/** Servlet that returns some example content. */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  //This global variable will hold all quotes that the user has inputed through the survey bar. 
  List<String> quoteList = new ArrayList<String> ();

  /**
   * This method converts the quoteList into JSON format and print that as the server's response. 
   */ 
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String quotesJson = convertToJson(quoteList);
    response.setContentType("text/html;");
    response.getWriter().println(quotesJson);
  } 

  /**
   * In this method, a new quote entered by the user is added to quoteList.
   * Then, this method redirects the page back to itself, which reloads the page. 
   */ 
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String quote = getParameter(request, "quote", ""); 
    quoteList.add(quote);

    response.sendRedirect("/index.html");
  }

  /**
   * This method handles the returning of request parameter 
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
}
