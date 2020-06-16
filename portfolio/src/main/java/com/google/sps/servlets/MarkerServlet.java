package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.sps.data.Marker;
import com.google.sps.data.Util; 
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handles fetching and saving markers data. */
@WebServlet("/marker")
public class MarkerServlet extends HttpServlet {

  private static final String PROPERTY_NAME_MARKER = "Marker";

  // Constants for request parameters and properties for Marker entities. 
  private static final String PROPERTY_NAME_LAT = "lat";
  private static final String PROPERTY_NAME_LONG = "lng"; 
  private static final String PROPERTY_NAME_CONTENT = "content";
  private static final String PROPERTY_NAME_NICKNAME = "nickname";
  
  // Global variable for an instance of DatastoreService
  private static DatastoreService datastore; 

  public MarkerServlet() {
    datastore = Util.getDatastoreServiceWithConsistency(); 
  }

  /** Responds with a JSON array containing marker data. */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    Collection<Marker> markers = getMarkersFromDatastore();
    Gson gson = new Gson();
    String markersJson = gson.toJson(markers);
    response.getWriter().println(markersJson);
  }

  /** Accepts a POST request containing a new marker. */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) {
    double lat = Double.parseDouble(request.getParameter(PROPERTY_NAME_LAT));
    double lng = Double.parseDouble(request.getParameter(PROPERTY_NAME_LONG));
    String markerContent = request.getParameter(PROPERTY_NAME_CONTENT);

    UserService userService = UserServiceFactory.getUserService();
    if (userService.isUserLoggedIn()) {
      putMarkerInDatastore(lat, lng, markerContent);
    }   
  }

  /** Fetches markers from Datastore. */
  private Collection<Marker> getMarkersFromDatastore() {
    Collection<Marker> markers = new ArrayList<>();

    Query query = new Query(PROPERTY_NAME_MARKER);
    PreparedQuery results = datastore.prepare(query);

    results.asIterable().forEach((markerEntity) -> {
      long markerId = markerEntity.getKey().getId(); 
      double lat = (double) markerEntity.getProperty(PROPERTY_NAME_LAT);
      double lng = (double) markerEntity.getProperty(PROPERTY_NAME_LONG);
      String content = (String) markerEntity.getProperty(PROPERTY_NAME_CONTENT);

      Marker marker = new Marker(markerId, lat, lng, content);
      markers.add(marker);
    }); 

    return markers;
  }

  /** Stores a marker in Datastore. */
  public void putMarkerInDatastore(double lat, double lng, String markerContent) { 
    Entity markerEntity = new Entity(PROPERTY_NAME_MARKER);
    markerEntity.setProperty(PROPERTY_NAME_LAT, lat);
    markerEntity.setProperty(PROPERTY_NAME_LONG, lng);
    markerEntity.setProperty(PROPERTY_NAME_CONTENT, markerContent);

    UserService userService = UserServiceFactory.getUserService();
    Optional<String> nicknameOptional = Util.getUserNickname(datastore, userService.getCurrentUser().getUserId());
    nicknameOptional.ifPresent(nickname -> markerEntity.setProperty(PROPERTY_NAME_NICKNAME, nickname));
    datastore.put(markerEntity);
  }
}
