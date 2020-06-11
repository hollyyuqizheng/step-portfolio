package com.google.sps.data;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceConfig.Builder;  
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Entity;
import java.util.Optional; 

public final class Util {

  /**
   * Sets up strong consistency for datastore service. 
   * This strong consistency ensures that freshness is more important than availability
   * so that the most up-to-date data is returned and displayed on the page.
   */ 
  public static DatastoreService getDatastoreServiceWithConsistency() { 
    DatastoreServiceConfig datastoreConfig = 
        DatastoreServiceConfig.Builder.withReadPolicy(new ReadPolicy(Consistency.STRONG)).deadline(5.0);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService(datastoreConfig);
    return datastore; 
  }

  /**
   * @return an Optional of type String that represents the user's nickname. 
   */
  public Optional<String> getUserNickname(DatastoreService datastore, String userId) {
    Query query =
        new Query("UserInfo")
            .setFilter(new Query.FilterPredicate("userId", Query.FilterOperator.EQUAL, userId));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();

    String nickname;
    if (entity == null) {
      nickname = null; 
    } else {
      nickname = (String) entity.getProperty("nickname");
    } 

    Optional<String> nicknameOptional = Optional.ofNullable(nickname);
    return nicknameOptional;
  }
}