package com.zendesk.maxwell.filtering;

import com.amazonaws.util.StringInputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.io.StreamTokenizer.*;

public class FilterParser {
	private final StreamTokenizer tokenizer;
	private final InputStreamReader inputStream;

	public FilterParser(
		String input
	) throws UnsupportedEncodingException {
		this.inputStream = new InputStreamReader(new StringInputStream(input));
		this.tokenizer = new StreamTokenizer(inputStream);
	}

	public List<FilterPattern> parse() throws IOException {
		ArrayList<FilterPattern> patterns = new ArrayList<>();
		tokenizer.quoteChar('"');
		tokenizer.ordinaryChar('.');
		tokenizer.ordinaryChar('/');
		tokenizer.quoteChar('`');
		tokenizer.quoteChar('\'');
		tokenizer.quoteChar('"');
		tokenizer.nextToken();

		FilterPattern p;
		while ( (p = parseFilterPattern()) != null )
			patterns.add(p);

		return patterns;
	}

	private void skipToken(char token) throws IOException {
		if ( tokenizer.ttype != token ) {
			throw new IOException("Expected '" + token + "', saw: " + tokenizer.toString());
		}
		tokenizer.nextToken();
	}


	private FilterPattern parseFilterPattern() throws IOException {
		FilterPatternType type;

		if ( tokenizer.ttype == TT_EOF )
			return null;

		if ( tokenizer.ttype != TT_WORD )
			throw new IOException("expected [include, exclude, blacklist] in filter definition.");

		switch(tokenizer.sval.toLowerCase()) {
			case "include":
				type = FilterPatternType.INCLUDE;
				break;
			case "exclude":
				type = FilterPatternType.EXCLUDE;
				break;
			case "blacklist":
				type = FilterPatternType.BLACKLIST;
				break;
			default:
				throw new IOException("Unknown filter keyword: " + tokenizer.sval);
		}
		tokenizer.nextToken();


		skipToken(':');
		Pattern dbPattern = parsePattern();
		skipToken('.');
		Pattern tablePattern = parsePattern();

		if ( tokenizer.ttype == ',' )
			tokenizer.nextToken();

		return new FilterPattern(type, dbPattern, tablePattern);
	}

	private Pattern parsePattern() throws IOException {
		Pattern pattern;
		switch ( tokenizer.ttype ) {
			case '/':
				pattern = Pattern.compile(parseRegexp());
				break;
			case '*':
				pattern = Pattern.compile("");
				break;
			case TT_WORD:
			case '`':
			case '\'':
			case '"':
				pattern = Pattern.compile("^" + tokenizer.sval + "$");
				break;
			default:
				throw new IOException();
		}
		tokenizer.nextToken();
		return pattern;
	}

	private String parseRegexp() throws IOException {
		char ch, lastChar = 0;
		String s = "";
		while ( true ) {
			ch = (char) inputStream.read();
			if ( ch == '/' && lastChar != '\\' )
				break;

			s += ch;
			lastChar = ch;
		}
		return s;
	}
}
