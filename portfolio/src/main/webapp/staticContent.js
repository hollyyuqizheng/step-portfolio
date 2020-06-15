/** This file handles all the static UI elements of the portfolio. */

/**
 * Adds a list of hobbies to the page by displaying the hobby's description text
 * and updating the wrapper's background color so that it appears 
 */
function addHobby() {
  const hobbies = 'playing the Clarinet, reading mystery novels, (trying to) speak Spanish';
  
  const hobbyContainer = document.getElementById('hobby-container');
  hobbyContainer.innerText = hobbies;
  
  const hobbyWrapper = document.getElementById('hobby-wrapper');
  hobbyWrapper.style.backgroundColor = 'antiquewhite';
}

/**
 * Adds description for each project in project section. 
 * Changes the text of the project description box so that the description text appears. 
 * Changes the background color of the description box so that it seems like the box appears
 * If the project doesn't exist, a general "something is wrong" is displayed in the description box 
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
  
  const wrapperName = 'project-description-';
  const wrapper = document.getElementById(wrapperName.concat(project));
  
  wrapper.innerText = projectNameToData[projectName]['projectDetail']; 
  wrapper.style.backgroundColor = projectNameToData[projectName]['backgroundColor']; 
}

/**
 * Creates a data key-value pair object for each project. 
 */
function createProjectData(projectDetail, backgroundColor) {
  projectData = {}
  projectData['projectDetail'] = projectDetail;
  projectData['backgroundColor'] = backgroundColor; 
  return projectData; 
}

/**
 * Enables the highlighting of a selected section from the navigation bar. 
 * This function is called by the clicking of any one of the boxes in the navigation bar.
 * Input: section to select
 * Effect: selected section will have their border shown, and all the other sections 
 *         will not have the border. 
 */
function highlightSection(selectedIdString) {
  document.getElementById(selectedIdString).style.border = 'solid';
  document.getElementById(selectedIdString).style.borderColor = '#c5cf0a';

  const navbarSections = document.getElementsByClassName('navbar-section');

  Object.values(navbarSections).forEach(
    (section) => {
      if (section.id !== selectedIdString) {
        section.style.border = 'none'; 
      }
    }); 
}
