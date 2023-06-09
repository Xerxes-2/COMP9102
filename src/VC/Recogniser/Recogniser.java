/***
 ***
 *** Recogniser.java            
 ***
 ***/

/* At this stage, this parser accepts a subset of VC defined	by
 * the following grammar. 
 *
 * You need to modify the supplied parsing methods (if necessary) and 
 * add the missing ones to obtain a parser for the VC language.
 *
 * 20-Feb-2023

program       -> (func-decl | var-decl)*

--declaration
func-decl           -> type identifier para-list compound-stmt
var-decl            -> type init-declarator-list ";"
init-declarator-list-> init-declarator ( "," init-declarator )*
init-declarator     -> declarator ( "=" initialiser )? 
declarator          -> identifier ( identifier "[" INTLITERAL? "]" )?
initialiser         -> expr 
                    |  "{" expr ( "," expr )* "}"

--statements 
compound-stmt       -> "{" var-decl* stmt* "}" 
stmt                -> compound-stmt
                    |  if-stmt 
                    |  for-stmt
                    |  while-stmt 
                    |  break-stmt
                    |  continue-stmt
                    |  return-stmt
                    |  expr-stmt
if-stmt             -> if "(" expr ")" stmt ( else stmt )?
for-stmt            -> for "(" expr? ";" expr? ";" expr? ")" stmt
while-stmt          -> while "(" expr ")" stmt
break-stmt          -> break ";"
continue-stmt       -> continue ";"
return-stmt         -> return expr? ";"
expr-stmt           -> expr? ";"

--expressions 
expr                -> assignment-expr
assignment-expr     -> ( cond-or-expr "=" )* cond-or-expr
cond-or-expr        -> cond-and-expr ( "||" cond-and-expr )*
cond-and-expr       -> equality-expr ( "&&" equality-expr )*
equality-expr       -> rel-expr ( ( "==" | "!=" ) rel-expr )*
rel-expr            -> additive-expr ( ( "<" | "<=" | ">" | ">=" ) additive-expr )*
additive-expr       -> multiplicative-expr ( ( "+" | "-" ) multiplicative-expr )*
multiplicative-expr -> unary-expr ( ( "*" | "/" ) unary-expr )*
unary-expr          -> ( "+" | "-" | "!" )* primary-expr
primary-expr        -> identifier arg-list?
                    | identifier "[" expr "]"
                    | "(" expr ")"
                    | INTLITERAL
                    | FLOATLITERAL
                    | BOOLLITERAL
                    | STRINGLITERAL

--parameters
para-list           -> "(" proper-para-list? ")"
proper-para-list    -> para-decl ( "," para-decl )*
para-decl           -> type declarator
arg-list            -> "(" proper-arg-list? ")"
proper-arg-list     -> arg ( "," arg )*
arg                 -> expr
*/

package VC.Recogniser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;

public class Recogniser {

  private Scanner scanner;
  private ErrorReporter errorReporter;
  private Token currentToken;

  public Recogniser(Scanner lexer, ErrorReporter reporter) {
    scanner = lexer;
    errorReporter = reporter;

    currentToken = scanner.getToken();
  }

  // match checks to see f the current token matches tokenExpected.
  // If so, fetches the next token.
  // If not, reports a syntactic error.

  void match(int tokenExpected) throws SyntaxError {
    if (currentToken.kind == tokenExpected) {
      currentToken = scanner.getToken();
    } else {
      syntacticError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

  // accepts the current token and fetches the next
  void accept() {
    currentToken = scanner.getToken();
  }

  void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
    SourcePosition pos = currentToken.position;
    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
    throw (new SyntaxError());
  }

  // ========================== PROGRAMS ========================

  public void parseProgram() {
    try {
      while (currentToken.kind != Token.EOF) {
        parseFuncOrVarDecl();
      }
      if (currentToken.kind != Token.EOF) {
        syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
      }
    } catch (SyntaxError s) {
    }
  }

  // ========================== DECLARATIONS ========================

  void parseFuncOrVarDecl() throws SyntaxError {
    parseType();
    parseIdent();
    if (currentToken.kind == Token.LPAREN) {
      parseParaList();
      parseCompoundStmt();
    } else {
      if (currentToken.kind == Token.LBRACKET) {
        accept();
        if (currentToken.kind == Token.INTLITERAL) {
          accept();
        }
        match(Token.RBRACKET);
      }
      if (currentToken.kind == Token.EQ) {
        accept();
        parseInitialiser();
      }
      while (currentToken.kind == Token.COMMA) {
        accept();
        parseInitDeclarator();
      }
      match(Token.SEMICOLON);
    }
  }

  void parseVarDecl() throws SyntaxError {
    parseType();
    parseInitDeclaratorList();
    match(Token.SEMICOLON);
  }

  void parseInitDeclaratorList() throws SyntaxError {
    parseInitDeclarator();
    while (currentToken.kind == Token.COMMA) {
      accept();
      parseInitDeclarator();
    }
  }

  void parseInitDeclarator() throws SyntaxError {
    parseDeclarator();
    if (currentToken.kind == Token.EQ) {
      accept();
      parseInitialiser();
    }
  }

  void parseDeclarator() throws SyntaxError {
    parseIdent();
    if (currentToken.kind == Token.LBRACKET) {
      accept();
      if (currentToken.kind == Token.INTLITERAL) {
        accept();
      }
      match(Token.RBRACKET);
    }
  }

  void parseInitialiser() throws SyntaxError {
    if (currentToken.kind == Token.LCURLY) {
      accept();
      parseExpr();
      while (currentToken.kind == Token.COMMA) {
        accept();
        parseExpr();
      }
      match(Token.RCURLY);
    } else {
      parseExpr();
    }

  }

  void parseType() throws SyntaxError {
    switch (currentToken.kind) {
      case Token.VOID:
        accept();
        break;
      case Token.INT:
        accept();
        break;
      case Token.FLOAT:
        accept();
        break;
      case Token.BOOLEAN:
        accept();
        break;
      default:
        syntacticError("\"%\": Unknown type", currentToken.spelling);
        break;
    }
  }

  boolean isFirstType() {
    return currentToken.kind == Token.VOID || currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT
        || currentToken.kind == Token.BOOLEAN;
  }
  // ======================= STATEMENTS ==============================

  void parseCompoundStmt() throws SyntaxError {
    match(Token.LCURLY);
    while (!isFirstStmt() && currentToken.kind != Token.RCURLY)
      parseVarDecl();
    parseStmtList();
    match(Token.RCURLY);
  }

  // Here, a new nontermial has been introduced to define { stmt } *
  void parseStmtList() throws SyntaxError {
    while (currentToken.kind != Token.RCURLY)
      parseStmt();
  }

  void parseStmt() throws SyntaxError {

    switch (currentToken.kind) {

      case Token.LCURLY:
        parseCompoundStmt();
        break;

      case Token.IF:
        parseIfStmt();
        break;

      case Token.FOR:
        parseForStmt();
        break;

      case Token.WHILE:
        parseWhileStmt();
        break;

      case Token.BREAK:
        parseBreakStmt();
        break;

      case Token.CONTINUE:
        parseContinueStmt();
        break;

      case Token.RETURN:
        parseReturnStmt();
        break;

      default:
        parseExprStmt();
        break;

    }
  }

  void parseIfStmt() throws SyntaxError {
    match(Token.IF);
    match(Token.LPAREN);
    parseExpr();
    match(Token.RPAREN);
    parseStmt();
    if (currentToken.kind == Token.ELSE) {
      accept();
      parseStmt();
    }
  }

  void parseForStmt() throws SyntaxError {
    match(Token.FOR);
    match(Token.LPAREN);
    if (isFirstExpr()) {
      parseExpr();
    }
    for (int i = 0; i < 2; i++) {
      match(Token.SEMICOLON);
      if (isFirstExpr()) {
        parseExpr();
      }
    }
    match(Token.RPAREN);
    parseStmt();
  }

  void parseWhileStmt() throws SyntaxError {
    match(Token.WHILE);
    match(Token.LPAREN);
    parseExpr();
    match(Token.RPAREN);
    parseStmt();
  }

  void parseBreakStmt() throws SyntaxError {
    match(Token.BREAK);
    match(Token.SEMICOLON);
  }

  void parseContinueStmt() throws SyntaxError {
    match(Token.CONTINUE);
    match(Token.SEMICOLON);
  }

  void parseReturnStmt() throws SyntaxError {
    match(Token.RETURN);
    if (isFirstExpr()) {
      parseExpr();
    }
    match(Token.SEMICOLON);
  }

  void parseExprStmt() throws SyntaxError {
    if (isFirstExpr()) {
      parseExpr();
    }
    match(Token.SEMICOLON);
  }

  boolean isFirstStmt() {
    return currentToken.kind == Token.LCURLY
        || currentToken.kind == Token.IF
        || currentToken.kind == Token.FOR
        || currentToken.kind == Token.WHILE
        || currentToken.kind == Token.BREAK
        || currentToken.kind == Token.CONTINUE
        || currentToken.kind == Token.RETURN
        || isFirstExpr()
        || currentToken.kind == Token.SEMICOLON;
  }

  boolean isFirstExpr() {
    return currentToken.kind == Token.ID
        || currentToken.kind == Token.INTLITERAL
        || currentToken.kind == Token.FLOATLITERAL
        || currentToken.kind == Token.BOOLEANLITERAL
        || currentToken.kind == Token.STRINGLITERAL
        || currentToken.kind == Token.MINUS
        || currentToken.kind == Token.NOT
        || currentToken.kind == Token.PLUS
        || currentToken.kind == Token.LPAREN;
  }
  // ======================= IDENTIFIERS ======================

  // Call parseIdent rather than match(Token.ID).
  // In Assignment 3, an Identifier node will be constructed in here.

  void parseIdent() throws SyntaxError {
    if (currentToken.kind == Token.ID) {
      accept();
    } else
      syntacticError("identifier expected here", "");
  }

  // ======================= OPERATORS ======================

  // Call acceptOperator rather than accept().
  // In Assignment 3, an Operator Node will be constructed in here.

  void acceptOperator() throws SyntaxError {
    currentToken = scanner.getToken();
  }

  // ======================= EXPRESSIONS ======================

  void parseExpr() throws SyntaxError {
    parseAssignExpr();
  }

  void parseAssignExpr() throws SyntaxError {
    if (!isFirstExpr()) {
      syntacticError("\"%\" cannot start an expression", currentToken.spelling);
    }
    while (isFirstExpr()) {
      parseCondOrExpr();
      if (currentToken.kind == Token.EQ) {
        acceptOperator();
      } else {
        break;
      }
    }
  }

  void parseCondOrExpr() throws SyntaxError {
    parseCondAndExpr();
    while (currentToken.kind == Token.OROR) {
      acceptOperator();
      parseCondAndExpr();
    }
  }

  void parseCondAndExpr() throws SyntaxError {
    parseEqualityExpr();
    while (currentToken.kind == Token.ANDAND) {
      acceptOperator();
      parseEqualityExpr();
    }
  }

  void parseEqualityExpr() throws SyntaxError {
    parseRelExpr();
    while (currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
      acceptOperator();
      parseRelExpr();
    }
  }

  void parseRelExpr() throws SyntaxError {
    parseAdditiveExpr();
    while (currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ
        || currentToken.kind == Token.GT || currentToken.kind == Token.GTEQ) {
      acceptOperator();
      parseAdditiveExpr();
    }
  }

  void parseAdditiveExpr() throws SyntaxError {
    parseMultiplicativeExpr();
    while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS) {
      acceptOperator();
      parseMultiplicativeExpr();
    }
  }

  void parseMultiplicativeExpr() throws SyntaxError {
    parseUnaryExpr();
    while (currentToken.kind == Token.MULT || currentToken.kind == Token.DIV) {
      acceptOperator();
      parseUnaryExpr();
    }
  }

  void parseUnaryExpr() throws SyntaxError {
    while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS
        || currentToken.kind == Token.NOT) {
      acceptOperator();
    }
    parsePrimaryExpr();
  }

  void parsePrimaryExpr() throws SyntaxError {

    switch (currentToken.kind) {
      case Token.ID:
        parseIdent();
        switch (currentToken.kind) {
          case Token.LPAREN:
            parseArgList();
            break;
          case Token.LBRACKET:
            accept();
            parseExpr();
            match(Token.RBRACKET);
            break;
          default:
            break;
        }
        break;

      case Token.LPAREN:
        accept();
        parseExpr();
        match(Token.RPAREN);
        break;

      case Token.INTLITERAL:
        parseIntLiteral();
        break;

      case Token.FLOATLITERAL:
        parseFloatLiteral();
        break;

      case Token.BOOLEANLITERAL:
        parseBooleanLiteral();
        break;

      case Token.STRINGLITERAL:
        parseStringLiteral();
        break;

      default:
        syntacticError("illegal primary expression", currentToken.spelling);

    }
  }

  // ========================== LITERALS ========================

  // Call these methods rather than accept(). In Assignment 3,
  // literal AST nodes will be constructed inside these methods.

  void parseIntLiteral() throws SyntaxError {
    if (currentToken.kind == Token.INTLITERAL) {
      accept();
    } else
      syntacticError("integer literal expected here", "");
  }

  void parseFloatLiteral() throws SyntaxError {
    if (currentToken.kind == Token.FLOATLITERAL) {
      accept();
    } else
      syntacticError("float literal expected here", "");
  }

  void parseBooleanLiteral() throws SyntaxError {
    if (currentToken.kind == Token.BOOLEANLITERAL) {
      accept();
    } else
      syntacticError("boolean literal expected here", "");
  }

  void parseStringLiteral() throws SyntaxError {
    if (currentToken.kind == Token.STRINGLITERAL) {
      accept();
    } else
      syntacticError("string literal expected here", "");
  }

  // ========================== PARAMETERS ========================

  void parseParaList() throws SyntaxError {
    match(Token.LPAREN);
    if (isFirstType()) {
      parseProperParaList();
    }
    match(Token.RPAREN);
  }

  void parseProperParaList() throws SyntaxError {
    parseParaDecl();
    while (currentToken.kind == Token.COMMA) {
      accept();
      parseParaDecl();
    }
  }

  void parseParaDecl() throws SyntaxError {
    parseType();
    parseDeclarator();
  }

  void parseArgList() throws SyntaxError {
    match(Token.LPAREN);
    if (isFirstExpr()) {
      parseProperArgList();
    }
    match(Token.RPAREN);
  }

  void parseProperArgList() throws SyntaxError {
    parseArg();
    while (currentToken.kind == Token.COMMA) {
      accept();
      parseArg();
    }
  }

  void parseArg() throws SyntaxError {
    parseExpr();
  }
}
