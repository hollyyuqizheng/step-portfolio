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

  private static final String PROPERTY_NAME_USER_ID = "userId"; 
  private static final String PROPERTY_NAME_USER_INFO = "UserInfo"; 
  private static final String PROPERTY_NAME_NICKNAME = "nickname"; 

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
        new Query(PROPERTY_NAME_USER_INFO)
            .setFilter(new Query.FilterPredicate(PROPERTY_NAME_USER_ID, Query.FilterOperator.EQUAL, userId));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();

    String nickname = null;
    if (entity != null) {
      nickname = (String) entity.getProperty(PROPERTY_NAME_NICKNAME);
    }

    Optional<String> nicknameOptional = Optional.ofNullable(nickname);
    return nicknameOptional;
  }
}
