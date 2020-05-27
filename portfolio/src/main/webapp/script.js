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



/**
 * Adds a random hobby to the page by displaying the hobby's description text
 * and updating the wrapper's background color so that it appears 
 */
function addRandomHobby() {
  const hobbies =
      ['playing the Clarinet', 'reading mystery novels', '(trying to) speak Spanish', 'making pasta from different cuisine'];

  // Pick a random greeting.
  const hobby = hobbies[Math.floor(Math.random() * hobbies.length)];

  // Add it to the page.
  const hobbyContainer = document.getElementById('hobby-container');
  hobbyContainer.innerText = hobby;

  const hobbyWrapper = document.getElementById('hobby-wrapper');
  hobbyWrapper.style.backgroundColor = 'antiquewhite';
}


/**
* This function is called when any of the project box is clicked.
* Input: string -- name of the selected project
* Results: 
* - change the text of the project description box so that the description text appears. 
* - change the background color of the description box so that it seems like the box appears
* - If the project doesn't exist, a general "something is wrong" is displayed in the description box 
*/
function addProjectDescription(project) {

    const details = {
        nlp: 'Building language models',
        ta: 'Held weekly office hours and review sessions for introductory Data Structure and Algorithms class',
        helmet: 'Integrated speech command to a \"smart\" bike helmet built by a team of 9 friends',
        unknown: 'Something is wrong, project doesn\'t exist'
    };

    const colors = {
        nlp: '#6ccfe0',
        ta: '#f7d36f',
        helmet: '#e0b9f0',
        unknown: 'black'
    };

    if (! (project in details)) {
        project = 'unknown'; 
    };

    const wrapperName = 'project-description-';
    const wrapper = document.getElementById(wrapperName.concat(project));

    wrapper.innerText = details[project]; 
    wrapper.style.backgroundColor = colors[project];
}