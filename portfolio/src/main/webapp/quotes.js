/** This file handles login and the quote submission box.*/

/**
 * Fetches the text from /data, with additional parameters
 * added to the request's URL string. 
 * Then, the function puts the received text into the quoteWrapper
 * that gets displayed on the UI.  
 */
function getQuotes() {
  const mainUrl = '/data';
  const numToDisplayUrl = '?numToDisplay=';
  const numToDisplay = document.getElementById('num-to-display').value; 
  const fetchUrl = mainUrl.concat(numToDisplayUrl).concat(numToDisplay);

  fetch(fetchUrl)
    .then(response => response.json())
    .then(
      /* Create a list item for each quote */
      (responseJson) => {
        updateLoginStatusElements(responseJson);
        if (responseJson['Quote']) {
          const quotesJson = JSON.parse(responseJson['Quote']);
          createQuotesList(quotesJson);
        }      
      });
}

/**
 * Creates a list for all fetched quotes from the GET request. 
 */
function createQuotesList(quotesJson) {
  if (quotesJson) {
    const quoteListElement = document.getElementById('quote-wrapper');
    quoteListElement.innerHTML = '';

    Object.values(quotesJson).forEach(
      (quote) => {
        quoteListElement.appendChild(createQuotesListElement(quote)); 
      }); 
  }
}

/**
 * Creates a li element that contains each individual quote;
 * this li element will be added to the Quotes section in the HTML file.  
 */
function createQuotesListElement(quote) {
  const liElement = document.createElement('li');
  const nickname = quote.nickname;  

  if (!nickname) {
    liElement.innerText = 'Unknown user: '.concat(quote.text);
  } else {
    liElement.innerText = nickname.concat(': ').concat(quote.text);
  }
  return liElement;
}

/**
 * Updates divs and texts on the UI according to the user's login status. 
 */
function updateLoginStatusElements(responseJson) {
  // Divs for quotes and map marker submission sections
  const showAfterLoginQuotes = document.getElementById('show-after-login-quotes'); 
  const showAfterLoginMap = document.getElementById('show-after-login-map'); 

  // Headers that contain signs for "Please log in first"
  const loginText = document.getElementById('login-text');
  const loginTextMap = document.getElementById('login-text-map');

  const loginStatus = document.getElementById('login-status'); 
  const redirectText = document.getElementById('redirect-text');
  redirectText.href = responseJson['redirectUrl'];

  const status = responseJson['loggedIn'];
  if (status === 'true') {
    showAfterLoginQuotes.style.visibility = 'visible';
    showAfterLoginMap.style.visibility = 'visible';
    loginText.style.visibility = 'hidden';
    loginTextMap.style.visibility = 'hidden';
    redirectText.innerText = 'Log out ';
    handleNicknameDisplay(responseJson);  
  } else {
    showAfterLoginQuotes.style.visibility = 'hidden';
    showAfterLoginMap.style.visibility = 'hidden';
    loginText.style.visibility = 'visible';
    loginTextMap.style.visibility = 'visible';
    loginStatus.innerText = 'Not logged in yet';
    redirectText.innerText = 'Log in ';    
  }     
}

/**
 * Displays the username's nickname if a nickname is stored in the response
 * Json returned by GET request. If a nickname does not exist,
 * the textbox prompting the user to create a nickname appears. 
 */
function handleNicknameDisplay(responseJson) {
  const nickname = responseJson['nickname'];
  const createNicknameText = document.getElementById('create-nickname'); 

  if (!nickname) {
    createNicknameText.style.visibility = 'visible';
  } else {
    createNicknameText.style.visibility = 'hidden'; 
    const loginStatus = document.getElementById('login-status'); 
    loginStatus.innerText = 'Logged in as '.concat(nickname); 
  }
}

/**
 * Performs a POST request to deleteQuote servlet, which 
 * handles the deletion of all quotes in Datastore. 
 */
function cleanData() {
  fetch('/deleteQuote', {method: 'POST'})
    .then(() => {
      refreshQuotes(); 
    });
}

/**
 * Handles the visual refreshing of the quote list element 
 * by setting its content to empty. 
 */
function refreshQuotes() {
  const quoteListElement = document.getElementById('quote-wrapper');
  quoteListElement.innerHTML = '';

  // Refetches all current quotes from Datastrore after deleting visually.
  // This ensures that quotes that are not deleted are shown on the page again. 
  getQuotes(); 
}
