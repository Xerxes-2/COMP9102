/*
 +--------------+
 + Parser.java  +
 +--------------+
 *
 * PLEASE COMPARE Recogniser.java PROVIDED IN ASSIGNMENT 2 AND Parser.java
 * PROVIDED BELOW TO UNDERSTAND HOW THE FORMER IS MODIFIED TO OBTAIN THE LATTER.
 *
 * This parser for a subset of the VC language is intended to 
 *  demonstrate how to create the AST nodes, including (among others): 
 *  (1) a list (of statements)
 *  (2) a function
 *  (3) a statement (which is an expression statement), 
 *  (4) a unary expression
 *  (5) a binary expression
 *  (6) terminals (identifiers, integer literals and operators)
 *
 * In addition, it also demonstrates how to use the two methods start 
 * and finish to determine the position information for the start and 
 * end of a construct (known as a phrase) corresponding an AST node.
 *
 * NOTE THAT THE POSITION INFORMATION WILL NOT BE MARKED. HOWEVER, IT CAN BE
 * USEFUL TO DEBUG YOUR IMPLEMENTATION.
 *
 *
 * --- 3/3/2023 --- 


program       -> func-decl
func-decl     -> type identifier "(" ")" compound-stmt
type          -> void
identifier    -> ID
// statements
compound-stmt -> "{" stmt* "}" 
stmt          -> expr-stmt
expr-stmt     -> expr? ";"
// expressions 
expr                -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
                    |  additive-expr "-" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
	            |  multiplicative-expr "/" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr

primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
 */

package VC.Parser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;

import VC.ErrorReporter;
import VC.ASTs.*;

public class Parser {

    private Scanner scanner;
    private ErrorReporter errorReporter;
    private Token currentToken;
    private SourcePosition previousTokenPosition;
    private SourcePosition dummyPos = new SourcePosition();
    private int depth = 0;

    public Parser(Scanner lexer, ErrorReporter reporter) {
        scanner = lexer;
        errorReporter = reporter;

        previousTokenPosition = new SourcePosition();

        currentToken = scanner.getToken();

    }

    // match checks to see f the current token matches tokenExpected.
    // If so, fetches the next token.
    // If not, reports a syntactic error.

    void match(int tokenExpected) throws SyntaxError {
        if (currentToken.kind == tokenExpected) {
            previousTokenPosition = currentToken.position;
            currentToken = scanner.getToken();
        } else {
            syntacticError("\"%\" expected here", Token.spell(tokenExpected));
        }
    }

    void accept() {
        previousTokenPosition = currentToken.position;
        currentToken = scanner.getToken();
    }

    void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
        SourcePosition pos = currentToken.position;
        errorReporter.reportError(messageTemplate, tokenQuoted, pos);
        throw (new SyntaxError());
    }

    // start records the position of the start of a phrase.
    // This is defined to be the position of the first
    // character of the first token of the phrase.

    void start(SourcePosition position) {
        position.lineStart = currentToken.position.lineStart;
        position.charStart = currentToken.position.charStart;
    }

    // finish records the position of the end of a phrase.
    // This is defined to be the position of the last
    // character of the last token of the phrase.

    void finish(SourcePosition position) {
        position.lineFinish = previousTokenPosition.lineFinish;
        position.charFinish = previousTokenPosition.charFinish;
    }

    void copyStart(SourcePosition from, SourcePosition to) {
        to.lineStart = from.lineStart;
        to.charStart = from.charStart;
    }

    void copyFinish(SourcePosition from, SourcePosition to) {
        to.lineFinish = from.lineFinish;
        to.charFinish = from.charFinish;
    }

    // ========================== PROGRAMS ========================

    public Program parseProgram() {

        Program programAST = null;

        SourcePosition programPos = new SourcePosition();
        start(programPos);

        try {
            List dlAST = parseDeclList();
            finish(programPos);
            programAST = new Program(dlAST, programPos);
            if (currentToken.kind != Token.EOF) {
                syntacticError("\"%\" unknown type", currentToken.spelling);
            }
        } catch (SyntaxError s) {
            return null;
        }
        return programAST;
    }

    // ========================== DECLARATIONS ========================

    List parseDeclList() throws SyntaxError {
        List dlAST = null;
        Decl funcDeclAST = null;
        List varDeclListAST = null;
        boolean funcOrVar;

        SourcePosition declPos = new SourcePosition();
        start(declPos);

        if (isFirstType()) {
            Type tAST = parseType();
            Ident iAST = parseIdent();
            if (currentToken.kind == Token.LPAREN) {
                // function declaration
                funcOrVar = true;
                funcDeclAST = parseFuncDeclAlt(tAST, iAST);
            } else {
                // variable declaration
                funcOrVar = false;
                varDeclListAST = parseVarDeclListAlt(tAST, iAST);
            }
        } else {
            finish(declPos);
            return new EmptyDeclList(dummyPos);
        }

        List remainList;
        if (isFirstType()) {
            remainList = parseDeclList();
            finish(declPos);
        } else {
            finish(declPos);
            remainList = new EmptyDeclList(dummyPos);
        }
        if (funcOrVar) {
            dlAST = new DeclList(funcDeclAST, remainList, declPos);
        } else {
            dlAST = linkDeclLists(varDeclListAST, remainList);
        }

        return dlAST;
    }

    List parseVarDeclList(List prevAST) throws SyntaxError {
        List dAST;
        if (isFirstType()) {
            dAST = parseVarDecl();
            prevAST = linkDeclLists(prevAST, dAST);
            return parseVarDeclList(prevAST);
        } else
            return prevAST;
    }

    List parseVarDeclListAlt(Type tAST, Ident iAST) throws SyntaxError {
        List declListAST, remainInlineAST;
        Type t2AST = tAST;
        Decl dAST;
        Expr arraySizeAST;
        Expr initAST = new EmptyExpr(dummyPos);
        SourcePosition declPos = new SourcePosition();
        SourcePosition inlinePos = new SourcePosition();
        copyStart(tAST.position, declPos);
        copyStart(tAST.position, inlinePos);

        if (currentToken.kind == Token.LBRACKET) {
            accept();
            if (currentToken.kind != Token.RBRACKET)
                arraySizeAST = parseExpr();
            else
                arraySizeAST = new EmptyExpr(dummyPos);
            match(Token.RBRACKET);
            t2AST = new ArrayType(tAST, arraySizeAST, tAST.position);
        }
        if (currentToken.kind == Token.EQ) {
            accept();
            initAST = parseInit();
        }
        finish(declPos);

        dAST = new GlobalVarDecl(t2AST, iAST, initAST, declPos);

        if (currentToken.kind == Token.COMMA) {
            accept();
            remainInlineAST = parseInitDeclList(tAST);
            match(Token.SEMICOLON);
            finish(inlinePos);
            copyFinish(inlinePos, remainInlineAST.position);
            declListAST = new DeclList(dAST, remainInlineAST, inlinePos);
        } else {
            match(Token.SEMICOLON);
            finish(inlinePos);
            declListAST = new DeclList(dAST, new EmptyDeclList(dummyPos), inlinePos);
        }
        return declListAST;
    }

    List linkDeclLists(List prevAST, List newAST) {
        if (prevAST.isEmptyDeclList())
            return newAST;
        if (newAST.isEmptyDeclList())
            return prevAST;
        DeclList last = (DeclList) prevAST;
        while (!last.DL.isEmptyDeclList()) {
            if (newAST.position != dummyPos) {
                copyFinish(newAST.position, last.position);
            }
            last = (DeclList) last.DL;
        }
        last.DL = newAST;
        return prevAST;

    }

    Decl parseFuncDecl() throws SyntaxError {

        Decl fAST = null;

        SourcePosition funcPos = new SourcePosition();
        start(funcPos);
        depth += 1;

        Type tAST = parseType();
        Ident iAST = parseIdent();
        List fplAST = parseParaList();
        Stmt cAST = parseCompoundStmt();

        finish(funcPos);
        depth -= 1;
        fAST = new FuncDecl(tAST, iAST, fplAST, cAST, funcPos);
        return fAST;
    }

    Decl parseFuncDeclAlt(Type tAST, Ident iAST) throws SyntaxError {
        Decl fAST = null;

        SourcePosition funcPos = new SourcePosition();
        copyStart(tAST.position, funcPos);
        depth += 1;

        List fplAST = parseParaList();
        Stmt cAST = parseCompoundStmt();

        finish(funcPos);
        depth -= 1;
        fAST = new FuncDecl(tAST, iAST, fplAST, cAST, funcPos);
        return fAST;
    }

    List parseVarDecl() throws SyntaxError {
        List vAST = null;

        SourcePosition varPos = new SourcePosition();
        start(varPos);

        Type tAST = parseType();
        vAST = parseInitDeclList(tAST);
        match(Token.SEMICOLON);

        return vAST;
    }

    List parseInitDeclList(Type tAST) throws SyntaxError {
        List iAST = null;

        SourcePosition initPos = new SourcePosition();
        start(initPos);
        Decl dAST = parseInitDecl(tAST);
        if (currentToken.kind == Token.COMMA) {
            accept();
            iAST = parseInitDeclList(tAST);
            finish(initPos);
            iAST = new DeclList(dAST, iAST, initPos);
        } else {
            finish(initPos);
            iAST = new DeclList(dAST, new EmptyDeclList(dummyPos), initPos);
        }

        return iAST;
    }

    Decl parseInitDecl(Type tAST) throws SyntaxError {

        SourcePosition initPos = new SourcePosition();
        start(initPos);

        DeclParseResult declAST = parseDecl(tAST);

        Expr initAST = new EmptyExpr(dummyPos);
        if (currentToken.kind == Token.EQ) {
            accept();
            initAST = parseInit();
        }

        finish(initPos);

        if (depth == 0) {
            return new GlobalVarDecl(declAST.getType(), declAST.getIdent(), initAST, initPos);
        } else {
            return new LocalVarDecl(declAST.getType(), declAST.getIdent(), initAST, initPos);
        }
    }

    class DeclParseResult {
        private Type type;
        private Ident ident;

        public DeclParseResult(Type type, Ident ident) {
            this.type = type;
            this.ident = ident;
        }

        public Type getType() {
            return type;
        }

        public Ident getIdent() {
            return ident;
        }
    }

    DeclParseResult parseDecl(Type primaryType) throws SyntaxError {
        Type declType = primaryType;

        SourcePosition declPos = new SourcePosition(); // Position of AST
        start(declPos);

        Ident identAST = parseIdent(); // Identifier of variable

        if (currentToken.kind == Token.LBRACKET) {
            // It's an array.
            accept();
            if (currentToken.kind != Token.RBRACKET) {
                // This array has a init size
                declType = new ArrayType(primaryType, parsePrimaryExpr(), declPos);
            } else {
                declType = new ArrayType(primaryType, new EmptyExpr(dummyPos), declPos);
            }
            match(Token.RBRACKET);
        }
        finish(declPos);

        return new DeclParseResult(declType, identAST);
    }

    Expr parseInit() throws SyntaxError {
        Expr iAST = null;

        SourcePosition initPos = new SourcePosition();
        start(initPos);
        if (currentToken.kind == Token.LCURLY) {
            accept();
            List lAST = parseArrExprList();
            match(Token.RCURLY);
            finish(initPos);
            iAST = new ArrayInitExpr(lAST, initPos);
        } else {
            iAST = parseExpr();
            finish(initPos);
        }

        return iAST;
    }

    // ======================== TYPES ==========================

    Type parseType() throws SyntaxError {
        Type typeAST = null;

        SourcePosition typePos = new SourcePosition();
        start(typePos);

        switch (currentToken.kind) {
            case Token.VOID:
                accept();
                finish(typePos);
                typeAST = new VoidType(typePos);
                break;
            case Token.INT:
                accept();
                finish(typePos);
                typeAST = new IntType(typePos);
                break;
            case Token.FLOAT:
                accept();
                finish(typePos);
                typeAST = new FloatType(typePos);
                break;
            case Token.BOOLEAN:
                accept();
                finish(typePos);
                typeAST = new BooleanType(typePos);
                break;
            default:
                syntacticError("\"%\": Unknown type", currentToken.spelling);
                break;
        }

        return typeAST;
    }

    boolean isFirstType() {
        return currentToken.kind == Token.VOID || currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT
                || currentToken.kind == Token.BOOLEAN;
    }

    // ======================= STATEMENTS ==============================

    Stmt parseCompoundStmt() throws SyntaxError {
        Stmt cAST = null;

        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);
        depth += 1;

        match(Token.LCURLY);

        List dlAST = parseVarDeclList(new EmptyDeclList(dummyPos));

        List slAST = parseStmtList();
        match(Token.RCURLY);
        depth -= 1;
        finish(stmtPos);

        /*
         * In the subset of the VC grammar, no variable declarations are
         * allowed. Therefore, a block is empty iff it has no statements.
         */
        if (slAST.isEmptyStmtList() && dlAST.isEmptyDeclList())
            cAST = new EmptyCompStmt(stmtPos);
        else
            cAST = new CompoundStmt(dlAST, slAST, stmtPos);
        return cAST;
    }

    List parseStmtList() throws SyntaxError {
        List slAST = null;

        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);

        if (currentToken.kind != Token.RCURLY) {
            Stmt sAST = parseStmt();
            {
                if (currentToken.kind != Token.RCURLY) {
                    slAST = parseStmtList();
                    finish(stmtPos);
                    slAST = new StmtList(sAST, slAST, stmtPos);
                } else {
                    finish(stmtPos);
                    slAST = new StmtList(sAST, new EmptyStmtList(dummyPos), stmtPos);
                }
            }
        } else
            slAST = new EmptyStmtList(dummyPos);

        return slAST;
    }

    Stmt parseStmt() throws SyntaxError {
        Stmt sAST = null;

        switch (currentToken.kind) {
            case Token.LCURLY:
                sAST = parseCompoundStmt();
                break;
            case Token.IF:
                sAST = parseIfStmt();
                break;
            case Token.WHILE:
                sAST = parseWhileStmt();
                break;
            case Token.FOR:
                sAST = parseForStmt();
                break;
            case Token.RETURN:
                sAST = parseReturnStmt();
                break;
            case Token.BREAK:
                sAST = parseBreakStmt();
                break;
            case Token.CONTINUE:
                sAST = parseContinueStmt();
                break;
            default:
                sAST = parseExprStmt();
                break;
        }

        return sAST;
    }

    Stmt parseIfStmt() throws SyntaxError {
        Stmt sAST = null;

        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);

        match(Token.IF);
        match(Token.LPAREN);
        Expr eAST = parseExpr();
        match(Token.RPAREN);
        Stmt s1AST = parseStmt();
        Stmt s2AST = null;
        if (currentToken.kind == Token.ELSE) {
            accept();
            s2AST = parseStmt();
        }

        finish(stmtPos);

        if (s2AST == null)
            sAST = new IfStmt(eAST, s1AST, stmtPos);
        else
            sAST = new IfStmt(eAST, s1AST, s2AST, stmtPos);
        return sAST;
    }

    Stmt parseWhileStmt() throws SyntaxError {
        Stmt sAST = null;

        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);

        match(Token.WHILE);
        match(Token.LPAREN);
        Expr eAST = parseExpr();
        match(Token.RPAREN);
        Stmt wAST = parseStmt();

        finish(stmtPos);
        sAST = new WhileStmt(eAST, wAST, stmtPos);
        return sAST;
    }

    Stmt parseForStmt() throws SyntaxError {
        Stmt sAST = null;

        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);

        match(Token.FOR);
        match(Token.LPAREN);
        Expr e1AST, e2AST, e3AST;
        if (isFirstExpr()) {
            e1AST = parseExpr();
        } else {
            e1AST = new EmptyExpr(dummyPos);
        }
        match(Token.SEMICOLON);
        if (isFirstExpr()) {
            e2AST = parseExpr();
        } else {
            e2AST = new EmptyExpr(dummyPos);
        }
        match(Token.SEMICOLON);
        if (isFirstExpr()) {
            e3AST = parseExpr();
        } else {
            e3AST = new EmptyExpr(dummyPos);
        }
        match(Token.RPAREN);
        Stmt fAST = parseStmt();

        finish(stmtPos);
        sAST = new ForStmt(e1AST, e2AST, e3AST, fAST, stmtPos);
        return sAST;
    }

    Stmt parseReturnStmt() throws SyntaxError {
        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);
        match(Token.RETURN);
        Expr eAST = null;
        if (isFirstExpr()) {
            eAST = parseExpr();
        } else {
            eAST = new EmptyExpr(dummyPos);
        }
        match(Token.SEMICOLON);
        finish(stmtPos);
        return new ReturnStmt(eAST, stmtPos);
    }

    Stmt parseBreakStmt() throws SyntaxError {
        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);
        match(Token.BREAK);
        match(Token.SEMICOLON);
        finish(stmtPos);
        return new BreakStmt(stmtPos);
    }

    Stmt parseContinueStmt() throws SyntaxError {
        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);
        match(Token.CONTINUE);
        match(Token.SEMICOLON);
        finish(stmtPos);
        return new ContinueStmt(stmtPos);
    }

    Stmt parseExprStmt() throws SyntaxError {
        Stmt sAST = null;

        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);

        Expr eAST = null;
        if (isFirstExpr()) {
            eAST = parseExpr();
        } else {
            eAST = new EmptyExpr(dummyPos);
        }
        match(Token.SEMICOLON);
        finish(stmtPos);
        sAST = new ExprStmt(eAST, stmtPos);
        return sAST;
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

    // ======================= PARAMETERS =======================

    List parseParaList() throws SyntaxError {
        List formalsAST = null;

        SourcePosition formalsPos = new SourcePosition();

        match(Token.LPAREN);
        start(formalsPos);
        if (isFirstType())
            formalsAST = parseProperParaList();
        finish(formalsPos);
        match(Token.RPAREN);

        if (formalsAST == null)
            formalsAST = new EmptyParaList(formalsPos);

        return formalsAST;
    }

    List parseProperParaList() throws SyntaxError {
        List formalsAST = null;

        SourcePosition formalsPos = new SourcePosition();
        start(formalsPos);

        ParaDecl dAST = parseParaDecl();
        List lAST = null;
        if (currentToken.kind == Token.COMMA) {
            accept();
            lAST = parseProperParaList();
        } else {
            lAST = new EmptyParaList(dummyPos);
        }

        finish(formalsPos);
        formalsAST = new ParaList(dAST, lAST, formalsPos);
        return formalsAST;
    }

    ParaDecl parseParaDecl() throws SyntaxError {
        ParaDecl pAST = null;

        SourcePosition paraPos = new SourcePosition();
        start(paraPos);

        Type tAST = parseType();
        DeclParseResult result = parseDecl(tAST);
        tAST = result.type;
        Ident iAST = result.ident;
        if (currentToken.kind == Token.LBRACKET) {
            while (currentToken.kind != Token.RBRACKET) {
                accept();
            }
            accept();
        }

        finish(paraPos);
        pAST = new ParaDecl(tAST, iAST, paraPos);
        return pAST;
    }

    List parseArgList() throws SyntaxError {
        List actualsAST = null;

        SourcePosition actualsPos = new SourcePosition();
        match(Token.LPAREN);
        start(actualsPos);

        if (isFirstExpr())
            actualsAST = parseProperArgList();

        finish(actualsPos);
        match(Token.RPAREN);

        if (actualsAST == null)
            actualsAST = new EmptyArgList(actualsPos);

        return actualsAST;
    }

    List parseProperArgList() throws SyntaxError {
        List actualsAST = null;

        SourcePosition actualsPos = new SourcePosition();
        start(actualsPos);

        Arg aAST = parseArg();
        List lAST = null;
        if (currentToken.kind == Token.COMMA) {
            accept();
            lAST = parseProperArgList();
        } else
            lAST = new EmptyArgList(dummyPos);

        finish(actualsPos);
        actualsAST = new ArgList(aAST, lAST, actualsPos);

        return actualsAST;
    }

    Arg parseArg() throws SyntaxError {
        Expr eAST = null;

        SourcePosition argPos = new SourcePosition();
        start(argPos);

        eAST = parseExpr();

        finish(argPos);

        return new Arg(eAST, argPos);
    }

    // ======================= EXPRESSIONS ======================

    Expr parseExpr() throws SyntaxError {
        Expr exprAST = null;
        exprAST = parseAssignExpr(0);
        return exprAST;
    }

    Expr parseAssignExpr(int depth) throws SyntaxError {
        Expr lAST = null;

        SourcePosition assignPos = new SourcePosition();
        start(assignPos);

        Expr eAST = parseCondOrExpr();
        Expr aAST = null;
        if (currentToken.kind == Token.EQ) {
            accept();
            aAST = parseAssignExpr(depth + 1);
        } else {
            return eAST;
        }

        finish(assignPos);
        lAST = new AssignExpr(eAST, aAST, assignPos);
        return lAST;
    }

    Expr parseCondOrExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition orStartPos = new SourcePosition();
        start(orStartPos);

        exprAST = parseCondAndExpr();
        while (currentToken.kind == Token.OROR) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseCondAndExpr();

            SourcePosition orPos = new SourcePosition();
            copyStart(orStartPos, orPos);
            finish(orPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, orPos);
        }

        return exprAST;
    }

    Expr parseCondAndExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition andStartPos = new SourcePosition();
        start(andStartPos);

        exprAST = parseEqualityExpr();
        while (currentToken.kind == Token.ANDAND) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseEqualityExpr();

            SourcePosition andPos = new SourcePosition();
            copyStart(andStartPos, andPos);
            finish(andPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, andPos);
        }

        return exprAST;
    }

    Expr parseEqualityExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition eqStartPos = new SourcePosition();
        start(eqStartPos);

        exprAST = parseRelExpr();
        while (currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseRelExpr();

            SourcePosition eqPos = new SourcePosition();
            copyStart(eqStartPos, eqPos);
            finish(eqPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, eqPos);
        }

        return exprAST;
    }

    Expr parseRelExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition relStartPos = new SourcePosition();
        start(relStartPos);

        exprAST = parseAdditiveExpr();
        while (currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ
                || currentToken.kind == Token.GT || currentToken.kind == Token.GTEQ) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseAdditiveExpr();

            SourcePosition relPos = new SourcePosition();
            copyStart(relStartPos, relPos);
            finish(relPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, relPos);
        }

        return exprAST;
    }

    Expr parseAdditiveExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition addStartPos = new SourcePosition();
        start(addStartPos);

        exprAST = parseMultiplicativeExpr();
        while (currentToken.kind == Token.PLUS
                || currentToken.kind == Token.MINUS) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseMultiplicativeExpr();

            SourcePosition addPos = new SourcePosition();
            copyStart(addStartPos, addPos);
            finish(addPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
        }
        return exprAST;
    }

    Expr parseMultiplicativeExpr() throws SyntaxError {

        Expr exprAST = null;

        SourcePosition multStartPos = new SourcePosition();
        start(multStartPos);

        exprAST = parseUnaryExpr();
        while (currentToken.kind == Token.MULT
                || currentToken.kind == Token.DIV) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseUnaryExpr();
            SourcePosition multPos = new SourcePosition();
            copyStart(multStartPos, multPos);
            finish(multPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, multPos);
        }
        return exprAST;
    }

    Expr parseUnaryExpr() throws SyntaxError {

        Expr exprAST = null;

        SourcePosition unaryPos = new SourcePosition();
        start(unaryPos);

        if (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS
                || currentToken.kind == Token.NOT) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseUnaryExpr();
            finish(unaryPos);
            exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
        } else
            exprAST = parsePrimaryExpr();

        return exprAST;
    }

    Expr parsePrimaryExpr() throws SyntaxError {

        Expr exprAST = null;

        SourcePosition primPos = new SourcePosition();
        start(primPos);

        switch (currentToken.kind) {

            case Token.ID:
                Ident iAST = parseIdent();
                switch (currentToken.kind) {
                    case Token.LPAREN:
                        List lAST = parseArgList();
                        finish(primPos);
                        exprAST = new CallExpr(iAST, lAST, primPos);
                        break;
                    case Token.LBRACKET:
                        SourcePosition varPos = primPos;
                        finish(varPos);
                        accept();
                        Var vAST = new SimpleVar(iAST, varPos);
                        Expr eAST = parseExpr();
                        match(Token.RBRACKET);
                        finish(primPos);
                        exprAST = new ArrayExpr(vAST, eAST, primPos);
                        break;
                    default:
                        finish(primPos);
                        Var simVAST = new SimpleVar(iAST, primPos);
                        exprAST = new VarExpr(simVAST, primPos);
                        break;
                }
                break;

            case Token.LPAREN:
                accept();
                exprAST = parseExpr();
                match(Token.RPAREN);
                break;

            case Token.INTLITERAL:
                IntLiteral ilAST = parseIntLiteral();
                finish(primPos);
                exprAST = new IntExpr(ilAST, primPos);
                break;

            case Token.BOOLEANLITERAL:
                BooleanLiteral blAST = parseBooleanLiteral();
                finish(primPos);
                exprAST = new BooleanExpr(blAST, primPos);
                break;

            case Token.FLOATLITERAL:
                FloatLiteral flAST = parseFloatLiteral();
                finish(primPos);
                exprAST = new FloatExpr(flAST, primPos);
                break;

            case Token.STRINGLITERAL:
                StringLiteral slAST = parseStringLiteral();
                finish(primPos);
                exprAST = new StringExpr(slAST, primPos);
                break;

            default:
                syntacticError("illegal primary expression", currentToken.spelling);

        }
        return exprAST;
    }

    List parseArrExprList() throws SyntaxError {
        List listAST = null;

        SourcePosition arrExprListPos = new SourcePosition();
        start(arrExprListPos);

        Expr eAST = parseExpr();
        List lAST = null;
        if (currentToken.kind == Token.COMMA) {
            accept();
            lAST = parseArrExprList();
        } else {
            lAST = new EmptyArrayExprList(dummyPos);
        }

        finish(arrExprListPos);
        listAST = new ArrayExprList(eAST, lAST, arrExprListPos);
        return listAST;
    }

    // ========================== ID, OPERATOR and LITERALS ========================

    Ident parseIdent() throws SyntaxError {

        Ident I = null;

        if (currentToken.kind == Token.ID) {
            previousTokenPosition = currentToken.position;
            String spelling = currentToken.spelling;
            I = new Ident(spelling, previousTokenPosition);
            currentToken = scanner.getToken();
        } else
            syntacticError("identifier expected here", "");
        return I;
    }

    // acceptOperator parses an operator, and constructs a leaf AST for it

    Operator acceptOperator() throws SyntaxError {
        Operator O = null;

        previousTokenPosition = currentToken.position;
        String spelling = currentToken.spelling;
        O = new Operator(spelling, previousTokenPosition);
        currentToken = scanner.getToken();
        return O;
    }

    IntLiteral parseIntLiteral() throws SyntaxError {
        IntLiteral IL = null;

        if (currentToken.kind == Token.INTLITERAL) {
            String spelling = currentToken.spelling;
            accept();
            IL = new IntLiteral(spelling, previousTokenPosition);
        } else
            syntacticError("integer literal expected here", "");
        return IL;
    }

    FloatLiteral parseFloatLiteral() throws SyntaxError {
        FloatLiteral FL = null;

        if (currentToken.kind == Token.FLOATLITERAL) {
            String spelling = currentToken.spelling;
            accept();
            FL = new FloatLiteral(spelling, previousTokenPosition);
        } else
            syntacticError("float literal expected here", "");
        return FL;
    }

    BooleanLiteral parseBooleanLiteral() throws SyntaxError {
        BooleanLiteral BL = null;

        if (currentToken.kind == Token.BOOLEANLITERAL) {
            String spelling = currentToken.spelling;
            accept();
            BL = new BooleanLiteral(spelling, previousTokenPosition);
        } else
            syntacticError("boolean literal expected here", "");
        return BL;
    }

    StringLiteral parseStringLiteral() throws SyntaxError {
        StringLiteral SL = null;

        if (currentToken.kind == Token.STRINGLITERAL) {
            String spelling = currentToken.spelling;
            accept();
            SL = new StringLiteral(spelling, previousTokenPosition);
        } else
            syntacticError("string literal expected here", "");
        return SL;
    }

}