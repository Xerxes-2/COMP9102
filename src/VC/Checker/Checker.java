/***
 * Checker.java   
 *
 * Thu 09 Mar 2023 11:37:23 AEDT
 *
 ****/

package VC.Checker;

import VC.ASTs.*;
import VC.Scanner.SourcePosition;
import VC.ErrorReporter;
import VC.StdEnvironment;

public final class Checker implements Visitor {

  private String errMesg[] = {
      "*0: main function is missing",
      "*1: return type of main is not int",

      // defined occurrences of identifiers
      // for global, local and parameters
      "*2: identifier redeclared", // : id
      "*3: identifier declared void", // : id
      "*4: identifier declared void[]", // : id

      // applied occurrences of identifiers
      "*5: identifier undeclared", // : id

      // assignments
      "*6: incompatible type for =",
      "*7: invalid lvalue in assignment",

      // types for expressions
      "*8: incompatible type for return",
      "*9: incompatible type for this binary operator", // : op
      "*10: incompatible type for this unary operator", // : op

      // scalars
      "*11: attempt to use an array/function as a scalar", // : id

      // arrays
      "*12: attempt to use a scalar/function as an array",
      "*13: wrong type for element in array initialiser", // : at position x
      "*14: invalid initialiser: array initialiser for scalar", // : id
      "*15: invalid initialiser: scalar initialiser for array", // : id
      "*16: excess elements in array initialiser", // : id
      "*17: array subscript is not an integer",
      "*18: array size missing", // : id

      // functions
      "*19: attempt to reference a scalar/array as a function", // : id

      // conditional expressions in if, for and while
      "*20: if conditional is not boolean", // (found type)
      "*21: for conditional is not boolean", // (found type)
      "*22: while conditional is not boolean", // (found type)

      // break and continue
      "*23: break must be in a while/for",
      "*24: continue must be in a while/for",

      // parameters
      "*25: too many actual parameters",
      "*26: too few actual parameters",
      "*27: wrong type for actual parameter", // : id

      // reserved for errors that I may have missed (J. Xue)
      "*28: misc 1",
      "*29: misc 2",

      // the following two checks are optional
      "*30: statement(s) not reached",
      "*31: missing return statement",
  };

  private SymbolTable idTable;
  private static SourcePosition dummyPos = new SourcePosition();
  private ErrorReporter reporter;
  private Type currentFunctionType;
  private int currentLoopDepth;
  private boolean isMainPresent;
  private boolean inGlobalScope;
  private int arrayIndex;

  // Checks whether the source program, represented by its AST,
  // satisfies the language's scope rules and type rules.
  // Also decorates the AST as follows:
  // (1) Each applied occurrence of an identifier is linked to
  // the corresponding declaration of that identifier.
  // (2) Each expression and variable is decorated by its type.

  public Checker(ErrorReporter reporter) {
    this.reporter = reporter;
    this.idTable = new SymbolTable();
    this.currentFunctionType = null;
    this.currentLoopDepth = 0;
    this.isMainPresent = false;
    this.inGlobalScope = true;
    this.arrayIndex = 0;
    establishStdEnvironment();
  }

  public void check(AST ast) {
    ast.visit(this, null);
  }

  // auxiliary methods

  private void declareVariable(Ident ident, Decl decl) {
    IdEntry entry = idTable.retrieveOneLevel(ident.spelling);

    if (entry == null) {
      ; // no problem
    } else
      reporter.reportError(errMesg[2] + ": %", ident.spelling, ident.position);

    if (decl.T.isVoidType())
      reporter.reportError(errMesg[3] + ": %", ident.spelling, ident.position);
    else if (decl.T.isArrayType() && ((ArrayType) decl.T).T.isVoidType())
      reporter.reportError(errMesg[4] + ": %", ident.spelling, ident.position);

    idTable.insert(ident.spelling, decl);
    // if a declaration, say, "int i = 1" has also an initialiser (i.e.,
    // "1" here, then i is both a defined occurrence (i.e., definition)
    // of i and an applied occurrence (i.e., use) of i. So do the
    // identification here for i just in case (even it is not used)
    ident.visit(this, null);
  }

  private UnaryExpr typeCoercionsI2F(Expr intExpr) {
    Operator op = new Operator("i2f", dummyPos);
    UnaryExpr eAST = new UnaryExpr(op, intExpr, dummyPos);
    eAST.type = StdEnvironment.floatType;
    return eAST;
  }

  // Programs
  public Object visitProgram(Program ast, Object o) {
    ast.FL.visit(this, ast.FL);

    return null;
  }

  // Empty lists, should always return null. Does not use the given object.
  public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
    if (!isMainPresent && inGlobalScope)
      reporter.reportError(errMesg[0], "", ((AST) o).position);
    return null;
  }

  public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
    return false;
  }

  public Object visitEmptyArrayExprList(EmptyArrayExprList ast, Object o) {
    return null;
  }

  public Object visitEmptyParaList(EmptyParaList ast, Object o) {
    return null;
  }

  public Object visitEmptyArgList(EmptyArgList ast, Object o) {
    List formalParaList = (List) o;
    if (formalParaList != null && !formalParaList.isEmpty())
      reporter.reportError(errMesg[26], "", ast.position);
    return null;
  }

  // Declarations
  // Always returns null. Does not use the given object.
  public Object visitFuncDecl(FuncDecl ast, Object o) {
    if (idTable.retrieveOneLevel(ast.I.spelling) != null)
      reporter.reportError(errMesg[2] + ": %", ast.I.spelling, ast.I.position);

    idTable.insert(ast.I.spelling, ast);

    // Your code goes here

    // HINT
    // Pass ast as the 2nd argument (as done below) so that the
    // formal parameters of the function can be extracted from ast when the
    // function body is later visited
    ast.T.visit(this, null);
    ast.I.visit(this, null);
    if (ast.I.spelling.equals("main")) {
      isMainPresent = true;
      if (!ast.T.isIntType())
        reporter.reportError(errMesg[1], "", ast.T.position);
    }
    inGlobalScope = false;
    currentFunctionType = ast.T;
    ast.S.visit(this, ast);
    currentFunctionType = null;
    inGlobalScope = true;
    return null;
  }

  public Object visitDeclList(DeclList ast, Object o) {
    ast.D.visit(this, null);
    ast.DL.visit(this, o);
    return null;
  }

  public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
    declareVariable(ast.I, ast);

    // fill the rest
    ast.T.visit(this, null);
    ast.I.visit(this, null);
    ast.E.visit(this, ast);
    if (!ast.E.isEmptyExpr()) {
      if (ast.T.isArrayType()) {
        if (ast.E.type != null)
          reporter.reportError(errMesg[15] + ": %", ast.I.spelling, ast.E.position);
      } else if (!ast.T.assignable(ast.E.type) && ast.E.type != null)
        reporter.reportError(errMesg[6], "", ast.position);
    } else if (ast.T.isArrayType() && ((ArrayType) ast.T).E.isEmptyExpr()) {
      reporter.reportError(errMesg[18] + ": %", ast.I.spelling, ast.position);
    }
    return null;
  }

  public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
    declareVariable(ast.I, ast);

    // fill the rest
    ast.T.visit(this, null);
    ast.I.visit(this, null);
    ast.E.visit(this, ast);
    if (!ast.E.isEmptyExpr()) {
      if (ast.T.isArrayType()) {
        if (ast.E.type != null)
          reporter.reportError(errMesg[15] + ": %", ast.I.spelling, ast.E.position);
      } else if (!ast.T.assignable(ast.E.type) && ast.E.type != null)
        reporter.reportError(errMesg[6], "", ast.position);
    } else if (ast.T.isArrayType() && ((ArrayType) ast.T).E.isEmptyExpr()) {
      reporter.reportError(errMesg[18] + ": %", ast.I.spelling, ast.I.position);
    }
    return null;
  }

  // Statements

  public Object visitCompoundStmt(CompoundStmt ast, Object o) {
    idTable.openScope();

    if (o instanceof FuncDecl) {
      FuncDecl funcDecl = (FuncDecl) o;
      funcDecl.PL.visit(this, ((FuncDecl) o).PL);
    }

    ast.DL.visit(this, null);
    boolean r = (boolean) ast.SL.visit(this, null);

    idTable.closeScope();

    if (o instanceof FuncDecl && !((FuncDecl) o).T.isVoidType() && !r)
      reporter.reportError(errMesg[31], "", ast.position);

    return r;
  }

  public Object visitStmtList(StmtList ast, Object o) {
    boolean r1 = (boolean) ast.S.visit(this, o);
    if (ast.S instanceof ReturnStmt && ast.SL instanceof StmtList)
      reporter.reportError(errMesg[30], "", ast.SL.position);
    boolean r2 = (boolean) ast.SL.visit(this, o);
    return r1 || r2;
  }

  static final String FOUND_STRING = " (found: %)";

  public Object visitIfStmt(IfStmt ast, Object o) {
    ast.E.visit(this, null);
    if (!ast.E.type.isBooleanType() && !ast.E.type.isErrorType())
      reporter.reportError(errMesg[20] + FOUND_STRING, ast.E.type.toString(), ast.E.position);
    boolean r1 = (boolean) ast.S1.visit(this, o);
    boolean r2 = (boolean) ast.S2.visit(this, o);
    return r1 && r2;
  }

  public Object visitWhileStmt(WhileStmt ast, Object o) {
    ast.E.visit(this, null);
    if (!ast.E.type.isBooleanType())
      reporter.reportError(errMesg[22] + FOUND_STRING, ast.E.type.toString(), ast.E.position);
    currentLoopDepth++;
    ast.S.visit(this, o);
    currentLoopDepth--;
    return false;
  }

  public Object visitForStmt(ForStmt ast, Object o) {
    idTable.openScope();
    ast.E1.visit(this, null);
    ast.E2.visit(this, null);
    if (ast.E2.isEmptyExpr())
      ast.E2 = new BooleanExpr(new BooleanLiteral("true", ast.E2.position), ast.E2.position);
    else if (!ast.E2.type.isBooleanType())
      reporter.reportError(errMesg[21] + FOUND_STRING, ast.E2.type.toString(), ast.E2.position);
    ast.E3.visit(this, null);
    currentLoopDepth++;
    ast.S.visit(this, ast);
    currentLoopDepth--;
    idTable.closeScope();
    return false;
  }

  public Object visitBreakStmt(BreakStmt ast, Object o) {
    // check if break is in a loop
    if (currentLoopDepth <= 0)
      reporter.reportError(errMesg[23], "", ast.position);
    return false;
  }

  public Object visitContinueStmt(ContinueStmt ast, Object o) {
    // check if continue is in a loop
    if (currentLoopDepth <= 0)
      reporter.reportError(errMesg[24], "", ast.position);
    return false;
  }

  public Object visitReturnStmt(ReturnStmt ast, Object o) {
    ast.E.visit(this, null);
    // check type of return value
    if (currentFunctionType != null) {
      if (currentFunctionType.isVoidType() ^ ast.E.isEmptyExpr())
        reporter.reportError(errMesg[8], "", ast.position);
      else if (currentFunctionType.isFloatType() && ast.E.type.isIntType())
        ast.E = typeCoercionsI2F(ast.E);
      else if (!currentFunctionType.assignable(ast.E.type))
        reporter.reportError(errMesg[8], "", ast.position);
    } else {
      // should not happen, but just in case
      reporter.reportError(errMesg[8], "", ast.position);
    }
    return true;
  }

  public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
    if (o instanceof FuncDecl && !((FuncDecl) o).T.isVoidType())
      reporter.reportError(errMesg[31], "", ast.position);

    return false;
  }

  public Object visitExprStmt(ExprStmt ast, Object o) {
    ast.E.visit(this, null);
    return false;
  }

  public Object visitEmptyStmt(EmptyStmt ast, Object o) {
    return false;
  }

  // Expressions

  // Returns the Type denoting the type of the expression. Does
  // not use the given object.

  public Object visitEmptyExpr(EmptyExpr ast, Object o) {
    ast.type = StdEnvironment.errorType;
    return ast.type;
  }

  public Object visitBooleanExpr(BooleanExpr ast, Object o) {
    ast.type = StdEnvironment.booleanType;
    return ast.type;
  }

  public Object visitIntExpr(IntExpr ast, Object o) {
    ast.type = StdEnvironment.intType;
    return ast.type;
  }

  public Object visitFloatExpr(FloatExpr ast, Object o) {
    ast.type = StdEnvironment.floatType;
    return ast.type;
  }

  public Object visitStringExpr(StringExpr ast, Object o) {
    ast.type = StdEnvironment.stringType;
    return ast.type;
  }

  public Object visitVarExpr(VarExpr ast, Object o) {
    ast.type = (Type) ast.V.visit(this, null);
    if (!ast.type.isErrorType()) {
      SimpleVar simpleVar = (SimpleVar) ast.V;
      if (ast.type.isArrayType() && !(o instanceof Arg))
        reporter.reportError(errMesg[11] + ": %", simpleVar.I.spelling, ast.position);
      else {
        Decl binding = idTable.retrieve(simpleVar.I.spelling);
        if (binding.isFuncDecl())
          reporter.reportError(errMesg[11] + ": %", simpleVar.I.spelling, ast.position);
      }
    }
    return ast.type;
  }

  public Object visitUnaryExpr(UnaryExpr ast, Object o) {
    ast.O.visit(this, null);
    ast.type = (Type) ast.E.visit(this, null);
    if (!ast.E.type.isErrorType())
      switch (ast.O.spelling.charAt(0)) {
        case '!':
          if (!ast.type.isBooleanType())
            reporter.reportError(errMesg[10] + ": %", ast.O.spelling, ast.position);
          ast.O.spelling = "i" + ast.O.spelling;
          break;
        case '+':
        case '-':
          if (ast.type.isFloatType())
            ast.O.spelling = "f" + ast.O.spelling;
          else if (ast.type.isIntType())
            ast.O.spelling = "i" + ast.O.spelling;
          else
            reporter.reportError(errMesg[10] + ": %", ast.O.spelling, ast.position);
          break;
        default:
          // should not happen
          reporter.reportError(errMesg[10] + ": %", ast.O.spelling, ast.position);
      }
    return ast.type;
  }

  public Object visitBinaryExpr(BinaryExpr ast, Object o) {
    ast.E1.visit(this, null);
    ast.O.visit(this, null);
    ast.E2.visit(this, null);
    if (ast.E1.type.isErrorType() || ast.E2.type.isErrorType()) {
      ast.type = StdEnvironment.errorType;
      return ast.type;
    }
    switch (ast.O.spelling.charAt(0)) {
      case '&':
      case '|':
        ast.type = StdEnvironment.booleanType;
        if (!ast.E1.type.isBooleanType() || !ast.E2.type.isBooleanType()) {
          reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
          ast.type = StdEnvironment.errorType;
        }
        ast.O.spelling = "i" + ast.O.spelling;
        break;
      case '+':
      case '-':
      case '*':
      case '/':
        ast.type = StdEnvironment.floatType;
        if (ast.E1.type.isFloatType() && ast.E2.type.isFloatType()) {
          ast.O.spelling = "f" + ast.O.spelling;
        } else if (ast.E1.type.isIntType() && ast.E2.type.isIntType()) {
          ast.type = StdEnvironment.intType;
          ast.O.spelling = "i" + ast.O.spelling;
        } else if (ast.E1.type.isIntType() && ast.E2.type.isFloatType()) {
          ast.E1 = typeCoercionsI2F(ast.E1);
          ast.O.spelling = "f" + ast.O.spelling;
        } else if (ast.E1.type.isFloatType() && ast.E2.type.isIntType()) {
          ast.E2 = typeCoercionsI2F(ast.E2);
          ast.O.spelling = "f" + ast.O.spelling;
        } else {
          reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
          ast.type = StdEnvironment.errorType;
        }
        break;
      case '<':
      case '>':
        ast.type = StdEnvironment.booleanType;
        if (ast.E1.type.isFloatType() && ast.E2.type.isFloatType()) {
          ast.O.spelling = "f" + ast.O.spelling;
        } else if (ast.E1.type.isIntType() && ast.E2.type.isIntType()) {
          ast.O.spelling = "i" + ast.O.spelling;
        } else if (ast.E1.type.isIntType() && ast.E2.type.isFloatType()) {
          ast.E1 = typeCoercionsI2F(ast.E1);
          ast.O.spelling = "f" + ast.O.spelling;
        } else if (ast.E1.type.isFloatType() && ast.E2.type.isIntType()) {
          ast.E2 = typeCoercionsI2F(ast.E2);
          ast.O.spelling = "f" + ast.O.spelling;
        } else {
          reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
          ast.type = StdEnvironment.errorType;
        }
        break;
      case '=':
      case '!':
        ast.type = StdEnvironment.booleanType;
        if ((ast.E1.type.isBooleanType() && ast.E2.type.isBooleanType())
            || (ast.E1.type.isIntType() && ast.E2.type.isIntType()))
          ast.O.spelling = "i" + ast.O.spelling;
        else if (ast.E1.type.isFloatType() && ast.E2.type.isFloatType())
          ast.O.spelling = "f" + ast.O.spelling;
        else if (ast.E1.type.isIntType() && ast.E2.type.isFloatType()) {
          ast.E1 = typeCoercionsI2F(ast.E1);
          ast.O.spelling = "f" + ast.O.spelling;
        } else if (ast.E1.type.isFloatType() && ast.E2.type.isIntType()) {
          ast.E2 = typeCoercionsI2F(ast.E2);
          ast.O.spelling = "f" + ast.O.spelling;
        } else {
          reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
          ast.type = StdEnvironment.errorType;
        }
        break;
      default:
        // should not happen
        reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
        ast.type = StdEnvironment.errorType;
    }
    return ast.type;
  }

  public Object visitArrayInitExpr(ArrayInitExpr ast, Object o) {
    Decl arrayDecl = (Decl) o;
    if (arrayDecl.T.isArrayType()) {
      ArrayType arrayType = (ArrayType) arrayDecl.T;
      if (arrayType.E.isEmptyExpr() || arrayType.E instanceof IntExpr) {
        arrayIndex = 0;
        ast.IL.visit(this, arrayDecl);
      } else {
        reporter.reportError(errMesg[17], "", ast.position);
      }
    } else {
      reporter.reportError(errMesg[14], "", ast.position);
    }
    return null;
  }

  public Object visitArrayExprList(ArrayExprList ast, Object o) {
    Decl arrayDecl = (Decl) o;
    ArrayType arrayType = (ArrayType) arrayDecl.T;
    Type elemType = arrayType.T;
    boolean sized = !arrayType.E.isEmptyExpr();
    int size = 0;
    if (sized) {
      String sizeString = ((IntExpr) arrayType.E).IL.spelling;
      size = Integer.parseInt(sizeString);
    }
    Type actualType = (Type) ast.E.visit(this, null);
    if (elemType.assignable(actualType)) {
      if (elemType.isFloatType() && actualType.isIntType())
        ast.E = typeCoercionsI2F(ast.E);
    } else
      reporter.reportError(errMesg[13] + ": at position %", Integer.toString(arrayIndex), ast.E.position);
    if (sized && arrayIndex == size)
      reporter.reportError(errMesg[16] + ": %", arrayDecl.I.spelling, ast.E.position);
    arrayIndex++;
    ast.EL.visit(this, o);
    if (ast.EL.isEmptyArrayExprList() && !sized)
      arrayType.E = new IntExpr(new IntLiteral(Integer.toString(arrayIndex), ast.position), ast.position);
    return null;
  }

  public Object visitArrayExpr(ArrayExpr ast, Object o) {
    Type arrayType = (Type) ast.V.visit(this, null);
    Type indexType = (Type) ast.E.visit(this, null);
    if (arrayType.isErrorType() || indexType.isErrorType())
      ast.type = StdEnvironment.errorType;
    else if (arrayType.isArrayType()) {
      ArrayType array = (ArrayType) arrayType;
      ast.type = array.T;
      if (!indexType.isIntType()) {
        ast.type = StdEnvironment.errorType;
        reporter.reportError(errMesg[17], "", ast.E.position);
      }
    } else {
      ast.type = StdEnvironment.errorType;
      reporter.reportError(errMesg[12], "", ast.position);
    }
    return ast.type;
  }

  public Object visitCallExpr(CallExpr ast, Object o) {
    Decl binding = (Decl) ast.I.visit(this, null);
    if (binding != null) {
      if (binding.isFuncDecl()) {
        FuncDecl f = (FuncDecl) binding;
        ast.type = f.T;
        ast.AL.visit(this, f.PL);
      } else {
        reporter.reportError(errMesg[19] + ": %", ast.I.spelling, ast.position);
        ast.type = StdEnvironment.errorType;
      }
    }
    return ast.type;
  }

  public Object visitAssignExpr(AssignExpr ast, Object o) {
    Type lhsType = (Type) ast.E1.visit(this, null);
    Type rhsType = (Type) ast.E2.visit(this, null);
    ast.type = rhsType;

    if (lhsType.assignable(rhsType)) {
      if (lhsType.isFloatType() && rhsType.isIntType())
        ast.E2 = typeCoercionsI2F(ast.E2);
    } else {
      reporter.reportError(errMesg[6], "", ast.position);
      ast.type = StdEnvironment.errorType;
    }
    if ((!(ast.E1 instanceof VarExpr) && !(ast.E1 instanceof ArrayExpr)) || lhsType.isErrorType()
        || rhsType.isErrorType()) {
      reporter.reportError(errMesg[7], "", ast.position);
      ast.type = StdEnvironment.errorType;
    }

    return ast.type;
  }

  // Literals, Identifiers and Operators
  public Object visitIdent(Ident I, Object o) {
    Decl binding = idTable.retrieve(I.spelling);
    if (binding != null)
      I.decl = binding;
    else
      reporter.reportError(errMesg[5] + ": %", I.spelling, I.position);
    return binding;
  }

  public Object visitBooleanLiteral(BooleanLiteral SL, Object o) {
    return StdEnvironment.booleanType;
  }

  public Object visitIntLiteral(IntLiteral IL, Object o) {
    return StdEnvironment.intType;
  }

  public Object visitFloatLiteral(FloatLiteral IL, Object o) {
    return StdEnvironment.floatType;
  }

  public Object visitStringLiteral(StringLiteral IL, Object o) {
    return StdEnvironment.stringType;
  }

  public Object visitOperator(Operator O, Object o) {
    return null;
  }

  // Parameters

  // Always returns null. Does not use the given object.

  public Object visitParaList(ParaList ast, Object o) {
    ast.P.visit(this, null);
    ast.PL.visit(this, null);
    return null;
  }

  public Object visitParaDecl(ParaDecl ast, Object o) {
    declareVariable(ast.I, ast);
    return null;
  }

  // Arguments

  public Object visitArgList(ArgList ast, Object o) {
    List formalParams = (List) o;
    if (formalParams.isEmptyParaList())
      reporter.reportError(errMesg[25], "", ast.A.position);
    else {
      ast.A.visit(this, ((ParaList) formalParams).P);
      ast.AL.visit(this, ((ParaList) formalParams).PL);
    }
    return null;
  }

  public Object visitArg(Arg ast, Object o) {
    Type argType = (Type) ast.E.visit(this, ast);
    ParaDecl formalParam = (ParaDecl) o;
    if (!argType.isErrorType() && formalParam.T.assignable(argType)) {
      if (formalParam.T.isFloatType() && argType.isIntType())
        ast.E = typeCoercionsI2F(ast.E);
    } else {
      if (argType.isArrayType() && formalParam.T.isArrayType()) {
        ArrayType argArrayType = (ArrayType) argType;
        ArrayType formalArrayType = (ArrayType) formalParam.T;
        if (!argArrayType.T.equals(formalArrayType.T)
            && !(argArrayType.T.isIntType() && formalArrayType.T.isFloatType()))
          reporter.reportError(errMesg[27] + ": %", formalParam.I.spelling, ast.E.position);
      } else
        reporter.reportError(errMesg[27] + ": %", formalParam.I.spelling, ast.E.position);
    }
    return null;
  }

  // Types

  // Returns the type predefined in the standard environment.

  public Object visitErrorType(ErrorType ast, Object o) {
    return StdEnvironment.errorType;
  }

  public Object visitBooleanType(BooleanType ast, Object o) {
    return StdEnvironment.booleanType;
  }

  public Object visitIntType(IntType ast, Object o) {
    return StdEnvironment.intType;
  }

  public Object visitFloatType(FloatType ast, Object o) {
    return StdEnvironment.floatType;
  }

  public Object visitStringType(StringType ast, Object o) {
    return StdEnvironment.stringType;
  }

  public Object visitVoidType(VoidType ast, Object o) {
    return StdEnvironment.voidType;
  }

  public Object visitArrayType(ArrayType ast, Object o) {
    ast.T.visit(this, null);
    ast.E.visit(this, null);
    return null;
  }

  // Variables
  public Object visitSimpleVar(SimpleVar ast, Object o) {
    Decl binding = (Decl) ast.I.visit(this, null);
    return binding == null ? StdEnvironment.errorType : binding.T;
  }

  // Creates a small AST to represent the "declaration" of each built-in
  // function, and enters it in the symbol table.

  private FuncDecl declareStdFunc(Type resultType, String id, List pl) {

    FuncDecl binding;

    binding = new FuncDecl(resultType, new Ident(id, dummyPos), pl,
        new EmptyStmt(dummyPos), dummyPos);
    idTable.insert(id, binding);
    return binding;
  }

  // Creates small ASTs to represent "declarations" of all
  // build-in functions.
  // Inserts these "declarations" into the symbol table.

  private final static Ident dummyI = new Ident("x", dummyPos);

  private void establishStdEnvironment() {

    // Define four primitive types
    // errorType is assigned to ill-typed expressions

    StdEnvironment.booleanType = new BooleanType(dummyPos);
    StdEnvironment.intType = new IntType(dummyPos);
    StdEnvironment.floatType = new FloatType(dummyPos);
    StdEnvironment.stringType = new StringType(dummyPos);
    StdEnvironment.voidType = new VoidType(dummyPos);
    StdEnvironment.errorType = new ErrorType(dummyPos);

    // enter into the declarations for built-in functions into the table

    StdEnvironment.getIntDecl = declareStdFunc(StdEnvironment.intType,
        "getInt", new EmptyParaList(dummyPos));
    StdEnvironment.putIntDecl = declareStdFunc(StdEnvironment.voidType,
        "putInt", new ParaList(
            new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
            new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putIntLnDecl = declareStdFunc(StdEnvironment.voidType,
        "putIntLn", new ParaList(
            new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
            new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.getFloatDecl = declareStdFunc(StdEnvironment.floatType,
        "getFloat", new EmptyParaList(dummyPos));
    StdEnvironment.putFloatDecl = declareStdFunc(StdEnvironment.voidType,
        "putFloat", new ParaList(
            new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
            new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putFloatLnDecl = declareStdFunc(StdEnvironment.voidType,
        "putFloatLn", new ParaList(
            new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
            new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putBoolDecl = declareStdFunc(StdEnvironment.voidType,
        "putBool", new ParaList(
            new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
            new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putBoolLnDecl = declareStdFunc(StdEnvironment.voidType,
        "putBoolLn", new ParaList(
            new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
            new EmptyParaList(dummyPos), dummyPos));

    StdEnvironment.putStringLnDecl = declareStdFunc(StdEnvironment.voidType,
        "putStringLn", new ParaList(
            new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
            new EmptyParaList(dummyPos), dummyPos));

    StdEnvironment.putStringDecl = declareStdFunc(StdEnvironment.voidType,
        "putString", new ParaList(
            new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
            new EmptyParaList(dummyPos), dummyPos));

    StdEnvironment.putLnDecl = declareStdFunc(StdEnvironment.voidType,
        "putLn", new EmptyParaList(dummyPos));

  }

}
