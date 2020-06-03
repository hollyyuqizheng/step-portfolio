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
 * Adds a list of hobbies to the page by displaying the hobby's description text
 * and updating the wrapper's background color so that it appears 
 */
function addHobby() {
  const hobbies = 'playing the Clarinet, reading mystery novels, (trying to) speak Spanish';
  
  const hobbyContainer = document.getElementById('hobbyContainer');
  hobbyContainer.innerText = hobbies;
  
  const hobbyWrapper = document.getElementById('hobbyWrapper');
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
  const projectName = project.toUpperCase(); 

  const projectNameToData = {
    NLP: createProjectData(
      'Building language models', 
      '#6ccfe0'),
    TA: createProjectData(
      'Held weekly office hours and review sessions for introductory Data Structure and Algorithms class', 
      '#f7d36f'),
    HELMET: createProjectData(
      'Integrated speech command to a \"smart\" bike helmet built by a team of 9 friends', 
      '#e0b9f0'),
    UNKNOWN: createProjectData(
      'Something is wrong, project doesn\'t exist', 
      'black')
  }
  
  if (!(projectName in projectNameToData)) {
    projectName = 'UNKNOWN'; 
  };
  
  const wrapperName = 'projectDescription';
  const wrapper = document.getElementById(wrapperName.concat(project));
  
  wrapper.innerText = projectNameToData[projectName]['projectDetail']; 
  wrapper.style.backgroundColor = projectNameToData[projectName]['backgroundColor']; 
}

/**
 * This is the helper function for addProjectDescription.
 * This function creates a data key-value pair object for each project. 
 */
function createProjectData(projectDetail, backgroundColor) {
  projectData = {}
  projectData['projectDetail'] = projectDetail;
  projectData['backgroundColor'] = backgroundColor; 
  return projectData; 
}

/**
 * This function enables the highlighting of a selected section from the navigation bar. 
 * This function is called by the clicking of any one of the boxes in the navigation bar.
 * Input: section to select
 * Effect: selected section will have their border shown, and all the other sections 
 *         will not have the border. 
 */
function highlightSection(selectedIdString) {
  document.getElementById(selectedIdString).style.border = 'solid';
  document.getElementById(selectedIdString).style.borderColor = '#c5cf0a';

  const navbarSections = document.getElementsByClassName('navbarSection');

  Object.values(navbarSections).forEach(
    (section) => {
      if (section.id !== selectedIdString) {
        section.style.border = 'none'; 
      }
    }); 
}

/**
 * This function fetches the text from /data.
 * Then, the function puts the received text into the quoteWrapper. 
 */
function getQuotes() {
  fetch('/data')
      .then(response => response.json())
      .then((quotesJson) => {
        const quoteListElement = document.getElementById('quoteWrapper');
        quoteListElement.innerHTML = '';

        Object.values(quotesJson).forEach(
          (quote) => {
            quoteListElement.appendChild(createQuotesListElement(quote)); 
          });        
      });
}

/**
 * This is a helper function, called inside getQuotes. 
 * This function creates a li element that contains each individual quote;
 * this li element will be added to the Quotes section in the HTML file.  
 */
function createQuotesListElement(quote) {
  const liElement = document.createElement('li');
  liElement.innerText = quote;
  return liElement;
}

/**
 * This function performs a POST request to deleteQuote servlet, which 
 * handles the deletion of all quotes in Datastore. 
 */
function cleanData() {
  fetch('/deleteQuote', {method: 'POST'})
    .then(() => {
      refreshQuotes(); 
    });
}

/**
 * This function handles the visual refreshing of the quote list element 
 * by setting its content to empty. 
 */
function refreshQuotes() {
  const quoteListElement = document.getElementById('quoteWrapper');
  quoteListElement.innerHTML = '';
}