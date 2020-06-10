package com.google.sps.servlets;

import com.google.appengine.api.users.User; 
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceConfig.Builder;  
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.sps.data.Quote; 
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

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private static final String QUOTE = "Quote";

  // Constants for property of a Quote item in Datastore.
  private static final String TEXT = "text";
  private static final String TIMESTAMP = "timestamp"; 
  private static final String USER_EMAIL = "userEmail";
  private static final String NICKNAME = "nickname";

  // Constants for information to be put into response of a GET request.
  private static final String IS_USER_LOGGED_IN = "loggedIn"; 
  private static final String REDIRECT_URL = "redirectUrl";
  private static final String HOME_URL = "/"; 

  // Constant for fetching quotes from Datastore
  private static final int DEFAULT_NUM_QUOTES = 5;
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");

    UserService userService = UserServiceFactory.getUserService();

    Map<String, String> responseMap = new HashMap<String, String>();
    User currentUser = userService.getCurrentUser(); 

    if (userService.isUserLoggedIn()) {
      String logoutUrl = userService.createLogoutURL(HOME_URL);
      String nickname = getUserNickname(currentUser.getUserId());  

      responseMap.put(IS_USER_LOGGED_IN, "true");
      responseMap.put(REDIRECT_URL, logoutUrl); 
      responseMap.put(NICKNAME, nickname); 

      // Only display quotes if the user is logged in
      String quotesJson = getQuoteListJson(request);
      responseMap.put(QUOTE, quotesJson);
    } else {
      String loginUrl = userService.createLoginURL(HOME_URL);
      responseMap.put(IS_USER_LOGGED_IN, "false");
      responseMap.put(REDIRECT_URL, loginUrl); 
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

    response.sendRedirect("/index.html");
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

      Entity quoteEntity = new Entity(QUOTE);
      quoteEntity.setProperty(TEXT, quote);
      quoteEntity.setProperty(TIMESTAMP, timestampMillis);
      quoteEntity.setProperty(USER_EMAIL, user.getEmail()); 
      quoteEntity.setProperty(NICKNAME, getUserNickname(user.getUserId()));

      DatastoreService datastore = getDatastoreServiceWithConsistency();
      datastore.put(quoteEntity);
    }
  }

  /**
   * Fetches quotes from Datastore and puts them into a list in JSON format. 
   */ 
  private String getQuoteListJson(HttpServletRequest request) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query(QUOTE).addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery quotes = datastore.prepare(query);

    // Get the parameter for number to display.
    int numQuoteToDisplay = getNumQuoteDisplayed(request, DEFAULT_NUM_QUOTES);
   
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
    String text = (String) quoteEntity.getProperty(TEXT);
    long timestampMillis = (long) quoteEntity.getProperty(TIMESTAMP);
    String userEmail = (String) quoteEntity.getProperty(USER_EMAIL); 
    String nickname = (String) quoteEntity.getProperty(NICKNAME); 
    Quote newQuote = new Quote(quoteId, text, timestampMillis, userEmail, nickname);
    return newQuote; 
  } 

  /**
   * Returns the nickname of the user with id, or null if the user has not set a nickname.
   */
  private String getUserNickname(String userId) {
    DatastoreService datastore = getDatastoreServiceWithConsistency();
    Query query =
        new Query("UserInfo")
            .setFilter(new Query.FilterPredicate("userId", Query.FilterOperator.EQUAL, userId));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();
    if (entity == null) {
      return null;
    }
    String nickname = (String) entity.getProperty("nickname");
    return nickname;
  }
}
