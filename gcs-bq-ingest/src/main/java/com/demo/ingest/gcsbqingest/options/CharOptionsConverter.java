package com.demo.ingest.gcsbqingest.options;

import com.google.common.base.Strings;
import com.google.devtools.common.options.Converter;
import com.google.devtools.common.options.OptionsParsingException;

/** For converting string input to characters */
public class CharOptionsConverter implements Converter<Character> {
    
	@Override
    public Character convert(String input) throws OptionsParsingException {
      if (Strings.isNullOrEmpty(input) || input.length() > 1) {
        throw new OptionsParsingException("Not a character.");
      }

      return input.charAt(0);
    }

    @Override
    public String getTypeDescription() {
      return "CSV delimiter";
    }
  }