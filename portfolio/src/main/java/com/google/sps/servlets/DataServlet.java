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
import java.util.ArrayList;
import com.google.gson.Gson;

/** Servlet that returns some example content. */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ArrayList<String> quotes = createQuotesArray();
    String quotesJson = convertToJson(quotes);

    response.setContentType("text/html;");
    response.getWriter().println(quotesJson);
  } 

  /**
   * This function creates an ArrayList containing a few hard-coded quotes. 
   */ 
  private ArrayList createQuotesArray() {
    ArrayList<String> quotes = new ArrayList<String>();
    quotes.add("It's all now you see. Yesterday won't be over until tomorrow and tomorrow began ten thousand years ago.");
    quotes.add("To imagine -- to dream about things that have not happened -- is among mankind's deepest needs.");
    return quotes; 
  }

  /**
   * This function takes in an ArrayList of quotes and converts it into a string 
   * that is in JSON format. 
   * Right now, this conversion is hard-coded. 
   * Input type: ArrayList<String>
   * Output type: String 
   */ 
  private String convertToJson(ArrayList<String> quotes){  
    // String json = "{";
    // json += "\"1\": ";
    // json += "\"" + quotes.get(0) + "\"";
    // json += ", ";
    // json += "\"2\": ";
    // json += "\"" + quotes.get(1) + "\"";
    // json += "}";
    // return json;

    Gson gson = new Gson();
    String json = gson.toJson(quotes);
    return json;
  }
}
