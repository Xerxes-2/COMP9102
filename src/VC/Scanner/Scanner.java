/*
 *
 *
 * 	 Scanner.java 	                         
 *
 *
 */

package VC.Scanner;

import VC.ErrorReporter;

public final class Scanner {

  private SourceFile sourceFile;
  private boolean debug;

  private ErrorReporter errorReporter;
  private StringBuffer currentSpelling;
  private char currentChar;
  private int currentLine;
  private int currentColumn;
  private int startColumn;
  private int oldColumn;
  private SourcePosition sourcePos;

  // =========================================================

  public Scanner(SourceFile source, ErrorReporter reporter) {
    sourceFile = source;
    errorReporter = reporter;
    debug = false;

    currentChar = sourceFile.getNextChar();
    // you may initialise your counters for line and column numbers here
    currentLine = 1 + (Character.isWhitespace(currentChar) ? 1 : 0);
    currentColumn = 1;
    oldColumn = 1;
    startColumn = 1;
    currentSpelling = new StringBuffer("");
  }

  public void enableDebugging() {
    debug = true;
  }

  // accept gets the next character from the source program.

  private void accept() {
    // you may save the lexeme of the current token incrementally here
    currentSpelling.append(currentChar);

    currentChar = sourceFile.getNextChar();

    // you may also increment your line and column counters here
    switch (currentChar) {
      case '\r':
        if (inspectChar(1) == '\n') {
          currentChar = sourceFile.getNextChar();
        }
        currentLine++;
        oldColumn = currentColumn;
        currentColumn = 0;
        break;
      case '\n':
        currentLine++;
        oldColumn = currentColumn;
        currentColumn = 0;
        break;
      default:
        oldColumn = currentColumn;
        currentColumn++;
        break;
    }
  }

  // inspectChar returns the n-th character after currentChar
  // in the input stream.
  //
  // If there are fewer than nthChar characters between currentChar
  // and the end of file marker, SourceFile.eof is returned.
  //
  // Both currentChar and the current position in the input stream
  // are *not* changed. Therefore, a subsequent call to accept()
  // will always return the next char after currentChar.

  private char inspectChar(int nthChar) {
    return sourceFile.inspectChar(nthChar);
  }

  private int nextToken() {
    // Tokens: separators, operators, literals, identifiers and keyworods

    switch (currentChar) {
      // separators
      case '{':
        accept();
        return Token.LCURLY;
      case '}':
        accept();
        return Token.RCURLY;
      case '[':
        accept();
        return Token.LBRACKET;
      case ']':
        accept();
        return Token.RBRACKET;
      case '(':
        accept();
        return Token.LPAREN;
      case ')':
        accept();
        return Token.RPAREN;
      case ';':
        accept();
        return Token.SEMICOLON;
      case ',':
        accept();
        return Token.COMMA;

      // operators
      case '+':
        accept();
        return Token.PLUS;
      case '-':
        accept();
        return Token.MINUS;
      case '*':
        accept();
        return Token.MULT;
      case '/':
        accept();
        return Token.DIV;
      case '<':
        accept();
        if (currentChar == '=') {
          accept();
          return Token.LTEQ;
        } else
          return Token.LT;
      case '>':
        accept();
        if (currentChar == '=') {
          accept();
          return Token.GTEQ;
        } else
          return Token.GT;
      case '=':
        accept();
        if (currentChar == '=') {
          accept();
          return Token.EQEQ;
        } else
          return Token.EQ;
      case '!':
        accept();
        if (currentChar == '=') {
          accept();
          return Token.NOTEQ;
        } else
          return Token.NOT;
      case '|':
        accept();
        if (currentChar == '|') {
          accept();
          return Token.OROR;
        } else
          return Token.ERROR;
      case '&':
        accept();
        if (currentChar == '&') {
          accept();
          return Token.ANDAND;
        } else
          return Token.ERROR;

      case '"':
        int stringLine = currentLine;
        accept();
        currentSpelling = new StringBuffer("");
        while (currentChar != '"' && currentLine == stringLine) {
          if (currentChar == '\\') {
            char echar = ' ';
            switch (inspectChar(1)) {
              case 'b':
                echar = '\b';
                break;
              case 'f':
                echar = '\f';
                break;
              case 'n':
                echar = '\n';
                break;
              case 'r':
                echar = '\r';
                break;
              case 't':
                echar = '\t';
                break;
              case '"':
                echar = '"';
                break;
              case '\'':
                echar = '\'';
                break;
              case '\\':
                echar = '\\';
                break;
              default:
                break;
            }
            if (echar != ' ') {
              currentSpelling.append(echar);
              accept();
              accept();
              currentSpelling.deleteCharAt(currentSpelling.length() - 1);
              currentSpelling.deleteCharAt(currentSpelling.length() - 1);
            } else {
              errorReporter.reportError("%: illegal escape character", '\\' + "" + inspectChar(1),
                  new SourcePosition(stringLine, startColumn, currentColumn));
              accept();
            }
          } else
            accept();
        }
        if (currentChar != '"' || currentLine != stringLine)
          errorReporter.reportError("%: unterminated string", currentSpelling.toString(),
              new SourcePosition(stringLine, startColumn, startColumn));
        else if (currentChar == '"') {
          accept();
          currentSpelling.deleteCharAt(currentSpelling.length() - 1);
        }
        return Token.STRINGLITERAL;

      case '.':
        // attempting to recognise a float
        int dotLine = currentLine;
        accept();
        if (Character.isDigit(currentChar)) {
          while (Character.isDigit(currentChar) && currentLine == dotLine)
            accept();
          if ((currentChar == 'e' || currentChar == 'E') && currentLine == dotLine
              && (Character.isDigit(inspectChar(1))
                  || ((inspectChar(1) == '+' || inspectChar(1) == '-') && Character.isDigit(inspectChar(2))))) {
            accept();
            accept();
            while (Character.isDigit(currentChar) && currentLine == dotLine)
              accept();
          }
          return Token.FLOATLITERAL;
        } else
          return Token.ERROR;

      case SourceFile.eof:
        currentSpelling.append(Token.spell(Token.EOF));
        oldColumn = currentColumn;
        return Token.EOF;
      default:
        // reserved words and identifiers

        if (Character.isLetter(currentChar) || currentChar == '_') {
          int idLine = currentLine;
          accept();
          while ((Character.isLetterOrDigit(currentChar) || currentChar == '_') && currentLine == idLine)
            accept();
          if ("true".compareTo(currentSpelling.toString()) == 0 || "false".compareTo(currentSpelling.toString()) == 0)
            return Token.BOOLEANLITERAL;
          return Token.ID;
        }

        if (Character.isDigit(currentChar)) {
          int numLine = currentLine;
          accept();
          while (Character.isDigit(currentChar) && currentLine == numLine)
            accept();
          if (currentChar == '.' && currentLine == numLine) {
            accept();
            while (Character.isDigit(currentChar) && currentLine == numLine)
              accept();
            if ((currentChar == 'e' || currentChar == 'E') && currentLine == numLine
                && (Character.isDigit(inspectChar(1))
                    || ((inspectChar(1) == '+' || inspectChar(1) == '-') && Character.isDigit(inspectChar(2))))) {
              accept();
              accept();
              while (Character.isDigit(currentChar) && currentLine == numLine)
                accept();
            }
            return Token.FLOATLITERAL;
          } else if ((currentChar == 'e' || currentChar == 'E') && currentLine == numLine
              && (Character.isDigit(inspectChar(1))
                  || ((inspectChar(1) == '+' || inspectChar(1) == '-') && Character.isDigit(inspectChar(2))))) {
            accept();
            accept();
            while (Character.isDigit(currentChar) && currentLine == numLine)
              accept();
            return Token.FLOATLITERAL;
          } else
            return Token.INTLITERAL;
        }

        break;
    }

    accept();
    return Token.ERROR;
  }

  void skipSpaceAndComments() {
    int inLineCommentLine = 0;
    boolean inInLineComment = false;
    boolean inBlockComment = false;
    boolean stuck = false;
    while (!stuck) {
      stuck = true;
      if (inBlockComment || inInLineComment || Character.isWhitespace(currentChar)) {
        if (currentChar == SourceFile.eof) {
          errorReporter.reportError("%: unterminated comment", "",
              new SourcePosition(currentLine - 1, startColumn, startColumn));
          return;
        }
        switch (currentChar) {
          case '\t':
            currentColumn = (currentColumn / 8 + 1) * 8;
            break;
        }
        accept();
        stuck = false;
      }
      if (currentChar == '/') {
        if (inspectChar(1) == '/' && !inBlockComment && !inInLineComment) {
          inInLineComment = true;
          inLineCommentLine = currentLine;
          startColumn = currentColumn;
          accept();
          stuck = false;
        } else if (inspectChar(1) == '*' && !inBlockComment && !inInLineComment) {
          inBlockComment = true;
          startColumn = currentColumn;
          accept();
          stuck = false;
        }
      }
      if (inInLineComment && currentLine > inLineCommentLine) {
        inInLineComment = false;
        stuck = false;
        continue;
      }
      if (inBlockComment && currentChar == '*' && inspectChar(1) == '/') {
        inBlockComment = false;
        accept();
        accept();
        stuck = false;
      }
    }
  }

  public Token getToken() {
    Token tok;
    int kind;

    // skip white space and comments

    skipSpaceAndComments();

    currentSpelling = new StringBuffer("");

    startColumn = currentColumn;
    int lineStart = currentLine;
    // You must record the position of the current token somehow

    kind = nextToken();

    sourcePos = new SourcePosition(lineStart, startColumn, oldColumn);
    tok = new Token(kind, currentSpelling.toString(), sourcePos);

    // * do not remove these three lines
    if (debug)
      System.out.println(tok);
    return tok;
  }
}
