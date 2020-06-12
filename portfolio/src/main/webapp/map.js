/** 
 * This file contains code for handling the map on the page. 
 * Some of these functions are borrowed from Week 4's Map tutorial example 'marker-storage'. 
 */

var editMarker; 

var map;

var allMarkers; 

/** Creates a map and adds it to the page. */
function createMap() {
  allMarkers = [];

  // Center the map at the middle of the Pacific so that both sides can show. 
  const latlongMidwayIsland = {lat: 28.2072, lng: -177.3735}; 

  const mapOptions = {
    center: latlongMidwayIsland,  
    zoom: 3 
  }; 

  // Initialize a new map and put my own markers on it. 
  map = new google.maps.Map(
    document.getElementById('simple-map'), 
    mapOptions
  );
  createPremadeMarkers(); 

  updateUserMarkers(); 

  map.addListener('click', (event) => {
    // Handles creating an edit marker for user to edit and 
    // storing this marker's information in Datastore. 
    // Also creates the new marker graphically and its info window. 
    createMarkerForEdit(event.latLng.lat(), event.latLng.lng());
  });
}

/**
 * Creates a list of already-set markers for the map. 
 * These markers cannot be deleted by the user. 
 */
function createPremadeMarkers() {
  const latlongBrownUniv = {lat: 41.8268, lng: -71.4025}; 
  const latlongHighSchool = {lat: 42.75, lng: -70.8983};
  const latlongBeijing = {lat: 39.9042, lng: 116.4074}; 

  const markerBrownUniv = new google.maps.Marker({
    map: map,
    position: latlongBrownUniv,
  });
  const infoBrownUniv = 'Where I am right now: Providence, RI'; 
  createPremadeInfoWindow(map, markerBrownUniv, infoBrownUniv); 

  const markerHighSchool = new google.maps.Marker({
    map: map,
    position: latlongHighSchool,
  });
  const infoHighSchool = 'Where I went to high school: Byfield, MA'; 
  createPremadeInfoWindow(map, markerHighSchool, infoHighSchool); 

  const markerBeijing = new google.maps.Marker({
    map: map,
    position: latlongBeijing,
  });
  const infoBeijing = 'Where I grew up: Beijing, China'; 
  createPremadeInfoWindow(map, markerBeijing, infoBeijing); 
}

/**
 * Creates an information window for a pre-made marker on the map. 
 */
function createPremadeInfoWindow(map, marker, description) {
  const contentString = description;
  const infowindow = new google.maps.InfoWindow({
    content: contentString 
  });

  marker.addListener('click', function() {
    infowindow.open(map, marker);
  });
}

/** Fetches markers from Datastore and adds them to the map. */
function updateUserMarkers() {
  fetch('/marker').then(response => response.json()).then(
      (markerJson) => { 
        markerJson.forEach((marker) => {   
          createUserMarkerForDisplay(marker);  
        });
        if (markerJson.content) {
          updateMarkerListContent(markerJson);
        }
      });
}

/** Creates a marker based on a JSON entry passed in as the argument. */
function createUserMarkerForDisplay(markerJson) {
  const marker = 
    new google.maps.Marker({
      position: {
        lat: markerJson.lat, 
        lng: markerJson.lng
      }, 
      map: map
    });

  marker.setMap(map);
  allMarkers.push(marker); 
  createUserInfoWindow(map, marker, markerJson.content); 
}

/** Creates the info window for a marker created by the user */
function createUserInfoWindow (map, marker, content) {
  const infowindow = new google.maps.InfoWindow({
    content: content 
  });

  // Single click for opening the info window. 
  marker.addListener('click', function() {
    infowindow.open(map, marker);
  });
}

/** Creates a marker that shows a textbox the user can edit. */
function createMarkerForEdit(lat, lng) {
  // If there is already an edit marker showing, removes it. 
  if (editMarker) {
    editMarker.setMap(null);
  }

  editMarker = new google.maps.Marker({position: {lat: lat, lng: lng}, map: map});

  const infoWindow = new google.maps.InfoWindow({content: buildInfoWindowInput(lat, lng)});

  // When the user closes the editable info window, remove the marker.
  google.maps.event.addListener(infoWindow, 'closeclick', () => {
    editMarker.setMap(null);
  });

  infoWindow.open(map, editMarker);
}

/**
 * Builds and returns HTML elements that show an editable textbox and a submit
 * button. Calls helper function postMarker to send new marker to Datastore. 
 */
function buildInfoWindowInput(lat, lng) {
  const textBox = document.createElement('textarea');
  const submitButton = document.createElement('button');
  submitButton.appendChild(document.createTextNode('Submit'));

  const containerDiv = document.createElement('div');
  containerDiv.appendChild(textBox);
  containerDiv.appendChild(document.createElement('br'));
  containerDiv.appendChild(submitButton);

  submitButton.onclick = () => {
    // Add new marker if info window is not empty. 
    if (textBox.value) {
      const markerJson = {
        lat: lat,
        lng: lng,
        content: textBox.value
      };
      postMarker(markerJson);
      createUserMarkerForDisplay(markerJson); 
      updateMarkerListContent(markerJson);
    } else {
      editMarker.setMap(null);
    }
  };

  return containerDiv;
}

/** Sends a marker to be stored in Datastore. */
function postMarker(markerJson) { 
  const params = new URLSearchParams();
  params.append('lat', markerJson.lat);
  params.append('lng', markerJson.lng);
  params.append('content', markerJson.content);
  fetch('/marker', {method: 'POST', body: params})
      .then(editMarker.setMap(null)); 
}

/** Adds a new marker's latitude and longitude information to the  
  * list below the map.
  */
function updateMarkerListContent(markerJson) { 
  const markerListElement = document.getElementById('marker-list');
  const markerItem = document.createElement('li');

  markerItem.innerText = markerJson.content + ': '
      + 'Latitude: ' + markerJson.lat + ', '
      + 'Longitude: ' + markerJson.lng; 

  markerItem.style.fontFamily = 'Trebuchet MS'; 

  markerListElement.appendChild(markerItem); 
}

/** Deletes markers graphically and from Datastore. */
function deleteMarkers() { 
  allMarkers.forEach((marker) => {
    marker.setMap(null);
    marker = null; 
  });
  allMarkers = []; 
  document.getElementById('marker-list').innerHTML = '';
  fetch('/deleteMarker', {method: 'POST'});
}
