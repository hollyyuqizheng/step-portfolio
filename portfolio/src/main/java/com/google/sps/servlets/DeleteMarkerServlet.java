package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;  
import com.google.sps.data.Util; 
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List; 
import java.util.ArrayList; 

/** Servlet responsible for deleting map markers. */
@WebServlet("/deleteMarker")
public class DeleteMarkerServlet extends HttpServlet {

  private static final String PROPERTY_NAME_MARKER = "Marker";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = Util.getDatastoreServiceWithConsistency(); 

    Query query = new Query(PROPERTY_NAME_MARKER);
    PreparedQuery markers = datastore.prepare(query);

    List<Key> toDeleteKeyList = new ArrayList<Key>();
    markers.asIterable().forEach((markerEntity) -> {
      toDeleteKeyList.add(markerEntity.getKey());
    }); 

    datastore.delete(toDeleteKeyList);
  }
}
