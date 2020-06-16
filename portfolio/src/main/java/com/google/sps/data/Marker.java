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

package com.google.sps.data;

/** Represents a marker on the map. */
public class Marker {

  public final long id; 
  public final double lat;
  public final double lng;
  public final String content;

  public Marker(long id, double lat, double lng, String content) {
    this.id = id; 
    this.lat = lat;
    this.lng = lng;
    this.content = content;
  }
}
