package com.google.sps.servlets;

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
import java.util.HashMap; 

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private static final String QUOTE = "Quote";
  private static final String TEXT = "text";
  private static final String TIMESTAMP = "timestamp"; 
  private static final String USEREMAIL = "userEmail";
  private static final String LOGGEDIN = "loggedIn"; 
  private static final String REDIRECTURL = "redirectUrl";
  private static final String NICKNAME = "nickname";
  private static final int DEFAULT_NUM_QUOTES = 5;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");

    UserService userService = UserServiceFactory.getUserService();
    HashMap<String, String> responseMap = new HashMap<String, String>();

    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      String urlToRedirectToForLogOut = "/";
      String logoutUrl = userService.createLogoutURL(urlToRedirectToForLogOut);
      String nickname = getUserNickname(userService.getCurrentUser().getUserId());  

      responseMap.put(LOGGEDIN, "true");
      responseMap.put(USEREMAIL, userEmail);
      responseMap.put(REDIRECTURL, logoutUrl); 
      responseMap.put(NICKNAME, nickname); 

      // Only display quotes if the user is logged in
      String quotesJson = getQuoteListJson(request);
      responseMap.put(QUOTE, quotesJson);
    } else {
      String urlToRedirectToForLogIn = "/";
      String loginUrl = userService.createLoginURL(urlToRedirectToForLogIn);
      responseMap.put(LOGGEDIN, "false");
      responseMap.put(USEREMAIL, null);
      responseMap.put(REDIRECTURL, loginUrl); 
      responseMap.put(NICKNAME, null); 
      responseMap.put(QUOTE, null);
    }

    Gson gson = new Gson();
    response.getWriter().println(gson.toJson(responseMap)); 
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    UserService userService = UserServiceFactory.getUserService();

    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      String nickname = getUserNickname(userService.getCurrentUser().getUserId()); 

      // HashMap that contains user information: email address, nickname.
      HashMap<String, String> userInfo = new HashMap<String, String>();
      userInfo.put(USEREMAIL, userEmail);
      userInfo.put(NICKNAME, nickname); 

      putQuoteIntoDatastore(request, userInfo);
    } 

    response.sendRedirect("/index.html");
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
  private void putQuoteIntoDatastore(HttpServletRequest request, HashMap<String, String> userInfo) {
    String quote = request.getParameter("quote");   
    if (quote.length() > 0) {
      long timestampMillis = System.currentTimeMillis();

      Entity quoteEntity = new Entity(QUOTE);
      quoteEntity.setProperty(TEXT, quote);
      quoteEntity.setProperty(TIMESTAMP, timestampMillis);
      quoteEntity.setProperty(USEREMAIL, userInfo.get(USEREMAIL)); 
      quoteEntity.setProperty(NICKNAME, userInfo.get(NICKNAME));

      // This Strong Consistency ensures that freshness is more important than availability
      // so that the most up-to-date data is returned and displayed on the page. 
      DatastoreServiceConfig datastoreConfig = 
          DatastoreServiceConfig.Builder.withReadPolicy(new ReadPolicy(Consistency.STRONG)).deadline(5.0);

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      datastore.put(quoteEntity);
    }
  }

  /**
   * Fetches quotes from Datastore and put them into a list in JSON format. 
   */ 
  private String getQuoteListJson(HttpServletRequest request) {
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
    long id = quoteEntity.getKey().getId();
    String text = (String) quoteEntity.getProperty(TEXT);
    long timestampMillis = (long) quoteEntity.getProperty(TIMESTAMP);
    String userEmail = (String) quoteEntity.getProperty(USEREMAIL); 
    String nickname = (String) quoteEntity.getProperty(NICKNAME); 
    Quote newQuote = new Quote(id, text, timestampMillis, userEmail, nickname);
    return newQuote; 
  } 

  /**
   * Returns the nickname of the user with id, or empty String if the user has not set a nickname.
   */
  private String getUserNickname(String id) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query =
        new Query("UserInfo")
            .setFilter(new Query.FilterPredicate("id", Query.FilterOperator.EQUAL, id));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();
    if (entity == null) {
      return null;
    }
    String nickname = (String) entity.getProperty("nickname");
    return nickname;
  }
}
