/**
 * Sends a POST request to the translate server to receive the 
 * translated text. 
 */
function translateSection(elementId, languageCode) {

  const boxToTranslate = document.getElementById(elementId)
  const textToTranslate = boxToTranslate.innerText;

  const resultContainer = document.getElementById(elementId.concat('-result'));

  resultContainer.innerText = 'Loading...';

  const params = new URLSearchParams();
  params.append('elementId', elementId); 
  params.append('textToTranslate', textToTranslate);
  params.append('languageCode', languageCode);

  fetch('/translate', {method: 'POST',body: params})
    .then(response => response.text())
    .then((translatedMessage) => { 
      resultContainer.innerText = translatedMessage;
    });
} 
