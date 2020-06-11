package com.google.sps.servlets;

import com.google.appengine.api.users.User; 
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.sps.data.Quote; 
import com.google.sps.data.Util; 
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import java.util.List; 
import java.util.ArrayList;
import java.util.Map; 
import java.util.HashMap; 
import java.util.Optional;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private static final String INDEX_URL = "/index.html"; 

  // Constants for property of a Quote item in Datastore.
  private static final String PROPERTY_NAME_QUOTE = "Quote";
  private static final String PROPERTY_NAME_TEXT = "text";
  private static final String PROPERTY_NAME_TIMESTAMP = "timestamp"; 
  private static final String PROPERTY_NAME_USER_EMAIL = "userEmail";
  private static final String PROPERTY_NAME_NICKNAME = "nickname";

  // Constants for information to be put into response of a GET request.
  private static final String IS_USER_LOGGED_IN = "loggedIn"; 
  private static final String REDIRECT_URL_PARAM = "redirectUrl";
  private static final String HOME_URL = "/"; 

  // Constant for fetching quotes from Datastore
  private static final int DEFAULT_NUM_QUOTES = 5;

  // Global variable for an instance of the Util helper class
  private static Util util; 
  
  // Global variable for an instance of DatastoreService
  private static DatastoreService datastore; 

  public DataServlet() {
    util = new Util();
    datastore = util.getDatastoreServiceWithConsistency(); 
  }
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");

    UserService userService = UserServiceFactory.getUserService();

    Map<String, String> responseMap = new HashMap<String, String>();
    User currentUser = userService.getCurrentUser(); 

    if (userService.isUserLoggedIn()) {
      String logoutUrl = userService.createLogoutURL(HOME_URL);

      Optional<String> nicknameOptional = util.getUserNickname(datastore, currentUser.getUserId());
      nicknameOptional.ifPresent(nickname -> responseMap.put(PROPERTY_NAME_NICKNAME, nickname));
      
      responseMap.put(IS_USER_LOGGED_IN, "true");
      responseMap.put(REDIRECT_URL_PARAM, logoutUrl); 

      // Only display quotes if the user is logged in
      int numQuoteToDisplay = getNumQuoteDisplayed(request, DEFAULT_NUM_QUOTES);
      String quotesJson = getQuoteListJson(numQuoteToDisplay);
      responseMap.put(PROPERTY_NAME_QUOTE, quotesJson);
    } else {
      String loginUrl = userService.createLoginURL(HOME_URL);
      responseMap.put(IS_USER_LOGGED_IN, "false");
      responseMap.put(REDIRECT_URL_PARAM, loginUrl); 
    }

    Gson gson = new Gson();
    response.getWriter().println(gson.toJson(responseMap)); 
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    UserService userService = UserServiceFactory.getUserService();

    if (userService.isUserLoggedIn()) {
      User currentUser = userService.getCurrentUser(); 
      putQuoteIntoDatastore(request, currentUser);
    } 

    response.sendRedirect(INDEX_URL);
  } 

  /**
   * Takes in an ArrayList of quotes and converts it into a string 
   * that is in JSON format. 
   * @param a list of quotes
   * @return the quote list formated as a JSON string
   */ 
  private String convertQuotesToJson(List<Quote> quoteList) {  
    Gson gson = new Gson();
    String json = gson.toJson(quoteList);
    return json;
  }

  /**
   * Creates a new entity for each quote and puts each new quote into Datastore
   * @param request: the HTTP request; 
   *        userInfo: a HashMap that contains user information connected to each quote
   */ 
  private void putQuoteIntoDatastore(HttpServletRequest request, User user) {
    String quote = request.getParameter("quote");   
    if (quote.length() > 0) {
      long timestampMillis = System.currentTimeMillis();

      Entity quoteEntity = new Entity(PROPERTY_NAME_QUOTE);
      quoteEntity.setProperty(PROPERTY_NAME_TEXT, quote);
      quoteEntity.setProperty(PROPERTY_NAME_TIMESTAMP, timestampMillis);
      quoteEntity.setProperty(PROPERTY_NAME_USER_EMAIL, user.getEmail()); 

      Optional<String> nicknameOptional = util.getUserNickname(datastore, user.getUserId());
      nicknameOptional.ifPresent(nickname -> quoteEntity.setProperty(PROPERTY_NAME_NICKNAME, nickname));
 
      datastore.put(quoteEntity);
    }
  }

  /**
   * Fetches quotes from Datastore and puts them into a list in JSON format. 
   */ 
  private String getQuoteListJson(int numQuoteToDisplay) {
    Query query = new Query(PROPERTY_NAME_QUOTE).addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery quotes = datastore.prepare(query);
   
    // Add each quote to the quote list whose content will be written as 
    // the response from the servlet. 
    List<Quote> quoteList = new ArrayList<>();
    quotes.asList(FetchOptions.Builder.withLimit(numQuoteToDisplay))
        .forEach((quoteEntity) -> {
          quoteList.add(extractQuoteFromEntity(quoteEntity));
        });

    String quotesJson = convertQuotesToJson(quoteList);
    return quotesJson; 
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

  /**
   * Extracts the attributes of a quoteEntity and puts them into a new
   * instance of Quote. 
   */ 
  private Quote extractQuoteFromEntity(Entity quoteEntity) {
    long quoteId = quoteEntity.getKey().getId();
    String text = (String) quoteEntity.getProperty(PROPERTY_NAME_TEXT);
    long timestampMillis = (long) quoteEntity.getProperty(PROPERTY_NAME_TIMESTAMP);
    String userEmail = (String) quoteEntity.getProperty(PROPERTY_NAME_USER_EMAIL); 
    String nickname = (String) quoteEntity.getProperty(PROPERTY_NAME_NICKNAME); 
    Quote newQuote = new Quote(quoteId, text, timestampMillis, userEmail, nickname);
    return newQuote; 
  } 

}
