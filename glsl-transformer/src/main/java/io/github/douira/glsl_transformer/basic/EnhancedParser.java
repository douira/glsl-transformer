package io.github.douira.glsl_transformer.basic;

import java.util.function.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import io.github.douira.glsl_transformer.*;
import io.github.douira.glsl_transformer.GLSLParser.TranslationUnitContext;
import io.github.douira.glsl_transformer.cst.token_filter.TokenFilter;
import io.github.douira.glsl_transformer.tree.ExtendedContext;

/**
 * The enhanced parser does more than just parsing. It also does lexing,
 * token filtering and switching between parsing modes on demand. It also
 * handles error listeners.
 */
public class EnhancedParser implements ParserInterface {
  private static class ThrowingErrorListener extends BaseErrorListener {
    public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

    @Override
    public void syntaxError(
        Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
        String msg, RecognitionException e) throws ParseCancellationException {
      throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg, e);
    }
  }

  private static EnhancedParser INSTANCE;

  // initialized with null since they need an argument
  private final GLSLLexer lexer = new GLSLLexer(null);
  private final GLSLParser parser = new GLSLParser(null);
  private boolean throwParseErrors = true;

  {
    parser.removeErrorListeners();
    lexer.removeErrorListeners();
  }

  /**
   * Enum for the parsing strategy.
   */
  public enum ParsingStrategy {
    /**
     * The default strategy that tries to use SLL first and then falls back to LL.
     */
    SLL_AND_LL_ON_ERROR,

    /**
     * The fast strategy that uses SLL only.
     */
    SLL_ONLY,

    /**
     * The slow strategy that uses LL only. This is useful if it's known that there
     * will be errors and parsing with SLL can be omitted in the first place.
     */
    LL_ONLY
  }

  private ParsingStrategy parsingStrategy = ParsingStrategy.SLL_AND_LL_ON_ERROR;

  /**
   * This is a debug property. This consumer will be called with errors parse
   * cancellation errors generated by the parser in sll and ll mode.
   */
  public BiConsumer<ParseCancellationException, ParseCancellationException> internalErrorConsumer;

  /**
   * The last parsed input stream. This property can be used together with the
   * parse methods since they don't give direct access to the internally created
   * input stream and token stream.
   */
  protected IntStream input;

  /**
   * The last parsed tokens stream.
   * 
   * @see #input
   */
  protected BufferedTokenStream tokenStream;

  /**
   * Optionally a token filter source that applies a token filter before parsing.
   * It filters the tokens coming from the lexer before the parser consumes them.
   * The contained token filter can be {@code null} if no filter is to be used.
   */
  private FilterTokenSource tokenSource = new FilterTokenSource(lexer);
  private TokenFilter<?> parseTokenFilter;

  /**
   * Creates a new parser and specifies if parse errors should be
   * thrown during parsing. If they should not be thrown they will not be reported
   * or printed to the console. ANTLR will attempt to recover from errors during
   * parsing any construct a parse tree containing error nodes. These nodes can
   * mess up transformation and printing. Do not expect anything to function
   * properly if an error was encountered during parsing.
   * 
   * Custom error handlers can be registered on the parser and lexer manually. For
   * example, an error handler similar to ConsoleErrorListener that allows
   * recovery and only collects the errors instead of printing them could be
   * created.
   * 
   * @param throwParseErrors If {@code true}, the parser throw any
   *                         parse errors encountered during parsing
   */
  public EnhancedParser(boolean throwParseErrors) {
    this.throwParseErrors = throwParseErrors;
  }

  /**
   * Creates a new parser that throws parse errors by default.
   */
  public EnhancedParser() {
  }

  /**
   * Gets the internal singleton instance of the parser. This should generally not
   * be used by external library users.
   */
  public static EnhancedParser getInternalInstance() {
    if (INSTANCE == null) {
      INSTANCE = new EnhancedParser(true);
    }
    return INSTANCE;
  }

  /**
   * Sets if the parser should be re-run in LL parsing mode if the SLL parsing
   * mode return an error. This is generally only necessary if it's important that
   * errors are only reported if there are actually errors. Keep in mind that LL
   * parsing mode is much slower than SLL.
   * 
   * @param parsingStrategy The parsing strategy to use
   */
  public void setParsingStrategy(ParsingStrategy parsingStrategy) {
    this.parsingStrategy = parsingStrategy;
  }

  /**
   * Sets the parsing strategy to {@link ParsingStrategy#SLL_ONLY}. This is the
   * faster strategy.
   */
  public void setSLLOnly() {
    setParsingStrategy(ParsingStrategy.SLL_ONLY);
  }

  /**
   * Sets the parsing strategy to {@link ParsingStrategy#LL_ONLY}. This is the
   * slower strategy.
   */
  public void setLLOnly() {
    setParsingStrategy(ParsingStrategy.LL_ONLY);
  }

  /**
   * The returned parser (and lexer) may contain no token stream or a wrong token
   * stream. However, the parser should not be used for parsing manually anyway.
   * The state and contents of the parser are set up correctly when the
   * transformation is performed.
   * 
   * {@inheritDoc}
   */
  public GLSLParser getParser() {
    return parser;
  }

  public GLSLLexer getLexer() {
    return lexer;
  }

  public BufferedTokenStream getTokenStream() {
    return tokenStream;
  }

  /**
   * Sets the token filter to use before parsing. It's placed between the lexer
   * and the token stream.
   * 
   * @param parseTokenFilter The new parse token filter
   */
  public void setParseTokenFilter(TokenFilter<?> parseTokenFilter) {
    this.parseTokenFilter = parseTokenFilter;
    this.tokenSource.setTokenFilter(parseTokenFilter);
  }

  public TokenFilter<?> getParseTokenFilter() {
    return parseTokenFilter;
  }

  /**
   * Parses a string as a translation unit.
   * 
   * @param str The string to parse
   * @return The parsed string as a translation unit parse tree
   */
  public TranslationUnitContext parse(String str) {
    return parse(str, GLSLParser::translationUnit);
  }

  /**
   * Parses a string using a parser method reference into a parse tree.
   * 
   * @param <RuleType>  The type of the resulting parsed node
   * @param str         The string to parse
   * @param parseMethod The parser method reference to use for parsing
   * @return The parsed string as a parse tree that has the given type
   */
  public <RuleType extends ExtendedContext> RuleType parse(
      String str,
      Function<GLSLParser, RuleType> parseMethod) {
    return parse(str, null, parseMethod);
  }

  /**
   * Parses a string using a parser method reference into a parse tree.
   * 
   * @param <RuleType>  The type of the resulting parsed node
   * @param str         The string to parse
   * @param parent      The parent to attach to the parsed node
   * @param parseMethod The parser method reference to use for parsing
   * @return The parsed string as a parse tree that has the given type
   */
  public <RuleType extends ExtendedContext> RuleType parse(
      String str,
      ExtendedContext parent,
      Function<GLSLParser, RuleType> parseMethod) {
    return parse(CharStreams.fromString(str), parent, parseMethod);
  }

  /**
   * Parses an int stream (which is similar to a string) using a parser method
   * reference into a parse tree. This method exists so non-string streams can
   * also be parsed.
   * 
   * @param <RuleType>  The type of the resulting parsed node
   * @param stream      The int stream to parse
   * @param parent      The parent to attach to the parsed node
   * @param parseMethod The parser method reference to use for parsing
   * @return The parsed string as a parse tree that has the given type
   */
  private <RuleType extends ExtendedContext> RuleType parse(
      IntStream stream,
      ExtendedContext parent,
      Function<GLSLParser, RuleType> parseMethod) {
    if (parseTokenFilter != null) {
      parseTokenFilter.resetState();
    }

    // setup lexer
    input = stream;
    lexer.setInputStream(input);

    // throw on lexer if enabled
    if (throwParseErrors) {
      lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
    } else {
      lexer.removeErrorListener(ThrowingErrorListener.INSTANCE);
    }
    lexer.reset();
    tokenStream = new CommonTokenStream(tokenSource);
    parser.setTokenStream(tokenStream);
    parser.reset();

    RuleType node;
    if (parsingStrategy == ParsingStrategy.SLL_AND_LL_ON_ERROR) {
      // never throw SLL errors
      parser.removeErrorListener(ThrowingErrorListener.INSTANCE);
      parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
      parser.setErrorHandler(new BailErrorStrategy());

      // try to parse with SLL mode
      try {
        node = parseMethod.apply(parser);
      } catch (ParseCancellationException SLLException) {
        // if there was an error in the SLL strategy either there is an error in the
        // string which should (possibly) be reported or the grammar is too difficult
        // for the SLL strategy to handle and the LL strategy has to be used instead
        // NOTE: it seems like the GLSL grammar never requires the LL strategy
        lexer.reset();
        parser.reset();

        // throw LL errors if enabled
        if (throwParseErrors) {
          parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        } else {
          parser.removeErrorListener(ThrowingErrorListener.INSTANCE);
        }
        parser.setErrorHandler(new DefaultErrorStrategy());
        parser.getInterpreter().setPredictionMode(PredictionMode.LL);

        ParseCancellationException possibleLLException = null;
        try {
          node = parseMethod.apply(parser);
        } catch (ParseCancellationException LLException) {
          possibleLLException = LLException;
          throw LLException;
        } finally {
          // notify the parse error consumer of both errors if they exist
          if (internalErrorConsumer != null) {
            internalErrorConsumer.accept(SLLException, possibleLLException);
          }
        }
      }
    } else {
      parser.getInterpreter().setPredictionMode(
          parsingStrategy == ParsingStrategy.SLL_ONLY ? PredictionMode.SLL : PredictionMode.LL);
      if (throwParseErrors) {
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
      } else {
        parser.removeErrorListener(ThrowingErrorListener.INSTANCE);
      }
      parser.setErrorHandler(new DefaultErrorStrategy());
      node = parseMethod.apply(parser);
    }

    node.setParent(parent);
    return node;
  }
}
