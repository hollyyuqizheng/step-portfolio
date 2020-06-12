package com.google.sps.servlets;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.sps.data.Util; 
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap; 
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/translate")
public class TranslateServlet extends HttpServlet {

  private static final String PROPERTY_NAME_LANGUAGE_CODE = "languageCode";
  private static final String PROPERTY_NAME_TEXT_TO_TRANSLATE = "textToTranslate";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the request parameters.
    String originalText = request.getParameter(PROPERTY_NAME_TEXT_TO_TRANSLATE);
    String languageCode = request.getParameter(PROPERTY_NAME_LANGUAGE_CODE);

    // Do the translation.
    Translate translate = TranslateOptions.getDefaultInstance().getService();
    Translation translation =
        translate.translate(originalText, Translate.TranslateOption.targetLanguage(languageCode));
    String translatedText = translation.getTranslatedText();

    // Output the translation.
    response.setContentType("text/html; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().println(translatedText);
  }
}
