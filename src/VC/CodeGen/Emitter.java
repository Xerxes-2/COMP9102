/*
 *** Emitter.java 
 *** Mon 03 Apr 2023 12:42:25 AEST
 */

// A new frame object is created for every function just before the
// function is being translated in visitFuncDecl.
//
// All the information about the translation of a function should be
// placed in this Frame object and passed across the AST nodes as the
// 2nd argument of every visitor method in Emitter.java.

package VC.CodeGen;

import java.util.LinkedList;
import java.util.Enumeration;
import java.util.ListIterator;

import VC.ASTs.*;
import VC.ErrorReporter;
import VC.StdEnvironment;

public final class Emitter implements Visitor {

    private ErrorReporter errorReporter;
    private String inputFilename;
    private String classname;
    private String outputFilename;
    private int arrayIndex = 0;

    public Emitter(String inputFilename, ErrorReporter reporter) {
        this.inputFilename = inputFilename;
        errorReporter = reporter;

        int i = this.inputFilename.lastIndexOf('.');
        if (i > 0)
            classname = this.inputFilename.substring(0, i);
        else
            classname = this.inputFilename;

        int j = classname.lastIndexOf(java.io.File.separatorChar);
        if (j > 0)
            classname = classname.substring(j + 1);
    }
    // PRE: ast must be a Program node

    public final void gen(AST ast) {
        ast.visit(this, null);
        JVM.dump(classname + ".j");
    }

    // Programs
    public Object visitProgram(Program ast, Object o) {
        /**
         * This method works for scalar variables only. You need to modify
         * it to handle all array-related declarations and initialisations.
         **/

        // Generates the default constructor initialiser
        emit(JVM.CLASS, "public", classname);
        emit(JVM.SUPER, "java/lang/Object");

        emit("");

        // Three subpasses:

        // (1) Generate .field definition statements since
        // these are required to appear before method definitions
        List list = ast.FL;
        while (!list.isEmpty()) {
            DeclList dlAST = (DeclList) list;
            if (dlAST.D instanceof GlobalVarDecl) {
                GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
                emit(JVM.STATIC_FIELD, vAST.I.spelling, VCtoJavaType(vAST.T));
            }
            list = dlAST.DL;
        }

        emit("");

        // (2) Generate <clinit> for global variables (assumed to be static)

        emit("; standard class static initializer ");
        emit(JVM.METHOD_START, "static <clinit>()V");
        emit("");

        // create a Frame for <clinit>

        Frame frame = new Frame(false);

        list = ast.FL;
        while (!list.isEmpty()) {
            DeclList dlAST = (DeclList) list;
            if (dlAST.D instanceof GlobalVarDecl) {
                GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
                if (!vAST.E.isEmptyExpr()) {
                    vAST.E.visit(this, frame);
                } else {
                    emitDefaultInit(vAST, frame);
                    frame.push();
                }
                emitPUTSTATIC(VCtoJavaType(vAST.T), vAST.I.spelling);
                frame.pop();
            }
            list = dlAST.DL;
        }

        emit("");
        emit("; set limits used by this method");
        emit(JVM.LIMIT, "locals", frame.getNewIndex());

        emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
        // changed by the marker
        // emit(JVM.LIMIT, "stack", 50);

        emit(JVM.RETURN);
        emit(JVM.METHOD_END, "method");

        emit("");

        // (3) Generate Java bytecode for the VC program

        emit("; standard constructor initializer ");
        emit(JVM.METHOD_START, "public <init>()V");
        emit(JVM.LIMIT, "stack 1");
        emit(JVM.LIMIT, "locals 1");
        emit(JVM.ALOAD_0);
        emit(JVM.INVOKESPECIAL, "java/lang/Object/<init>()V");
        emit(JVM.RETURN);
        emit(JVM.METHOD_END, "method");

        return ast.FL.visit(this, o);
    }

    // Statements

    public Object visitStmtList(StmtList ast, Object o) {
        ast.S.visit(this, o);
        ast.SL.visit(this, o);
        return null;
    }

    public Object visitCompoundStmt(CompoundStmt ast, Object o) {
        Frame frame = (Frame) o;

        String scopeStart = frame.getNewLabel();
        String scopeEnd = frame.getNewLabel();
        frame.scopeStart.push(scopeStart);
        frame.scopeEnd.push(scopeEnd);

        emit(scopeStart + ":");
        if (ast.parent instanceof FuncDecl) {
            if (((FuncDecl) ast.parent).I.spelling.equals("main")) {
                emit(JVM.VAR, "0 is argv [Ljava/lang/String; from " + frame.scopeStart.peek() + " to "
                        + frame.scopeEnd.peek());
                emit(JVM.VAR, "1 is vc$ L" + classname + "; from " + frame.scopeStart.peek() + " to "
                        + frame.scopeEnd.peek());
                // Generate code for the initialiser vc$ = new classname();
                emit(JVM.NEW, classname);
                emit(JVM.DUP);
                frame.push(2);
                emit("invokenonvirtual", classname + "/<init>()V");
                frame.pop();
                emit(JVM.ASTORE_1);
                frame.pop();
            } else {
                emit(JVM.VAR, "0 is this L" + classname + "; from " + frame.scopeStart.peek() + " to "
                        + frame.scopeEnd.peek());
                ((FuncDecl) ast.parent).PL.visit(this, o);
            }
        }
        ast.DL.visit(this, o);
        ast.SL.visit(this, o);
        emit(scopeEnd + ":");

        frame.scopeStart.pop();
        frame.scopeEnd.pop();
        return null;
    }

    public Object visitReturnStmt(ReturnStmt ast, Object o) {
        Frame frame = (Frame) o;

        /*
         * int main() { return 0; } must be interpretted as
         * public static void main(String[] args) { return ; }
         * Therefore, "return expr", if present in the main of a VC program
         * must be translated into a RETURN rather than IRETURN instruction.
         */

        if (frame.isMain()) {
            emit(JVM.RETURN);
        } else {
            AST parent = ast.parent;
            while (!(parent instanceof FuncDecl)) {
                parent = parent.parent;
            }
            FuncDecl fd = (FuncDecl) parent;
            ast.E.visit(this, o);
            if (fd.T.equals(StdEnvironment.intType) || fd.T.equals(StdEnvironment.booleanType)) {
                emit(JVM.IRETURN);
                frame.pop();
            } else if (fd.T.equals(StdEnvironment.floatType)) {
                emit(JVM.FRETURN);
                frame.pop();
            } else if (fd.T.equals(StdEnvironment.voidType)) {
                emit(JVM.RETURN);
            }
        }
        return null;
    }

    public Object visitWhileStmt(WhileStmt ast, Object o) {
        Frame frame = (Frame) o;

        String startLabel = frame.getNewLabel();
        String endLabel = frame.getNewLabel();

        emit(startLabel + ":");
        ast.E.visit(this, o);
        emit(JVM.IFEQ, endLabel);
        frame.pop();

        frame.conStack.push(startLabel);
        frame.brkStack.push(endLabel);

        ast.S.visit(this, o);
        emit(JVM.GOTO, startLabel);
        emit(endLabel + ":");

        frame.conStack.pop();
        frame.brkStack.pop();

        return null;
    }

    public Object visitIfStmt(IfStmt ast, Object o) {
        Frame frame = (Frame) o;

        String elseLabel = frame.getNewLabel();
        String endLabel = frame.getNewLabel();

        ast.E.visit(this, o);

        emit(JVM.IFEQ, elseLabel);
        frame.pop();
        ast.S1.visit(this, o);
        emit(JVM.GOTO, endLabel);
        emit(elseLabel + ":");
        ast.S2.visit(this, o);
        emit(endLabel + ":");

        return null;
    }

    public Object visitExprStmt(ExprStmt ast, Object o) {
        Frame frame = (Frame) o;
        int stackSize1 = frame.getCurStackSize();
        ast.E.visit(this, o);
        emitMultiPOP(stackSize1, frame);
        return null;
    }

    public Object visitBreakStmt(BreakStmt ast, Object o) {
        Frame frame = (Frame) o;
        emit(JVM.GOTO, frame.brkStack.peek());
        return null;
    }

    public Object visitContinueStmt(ContinueStmt ast, Object o) {
        Frame frame = (Frame) o;
        emit(JVM.GOTO, frame.conStack.peek());
        return null;
    }

    public Object visitForStmt(ForStmt ast, Object o) {
        Frame frame = (Frame) o;

        int stackSize1 = frame.getCurStackSize();
        ast.E1.visit(this, o);
        emitMultiPOP(stackSize1, frame);

        String loopStart = frame.getNewLabel();
        String loopEnd = frame.getNewLabel();
        String loopContinue = frame.getNewLabel();

        emit(loopStart + ":");
        ast.E2.visit(this, o);
        if (ast.E2.isEmptyExpr()) {
            emit(JVM.ICONST_1);
            frame.push();
        }
        emit(JVM.IFEQ, loopEnd);
        frame.pop();

        frame.conStack.push(loopContinue);
        frame.brkStack.push(loopEnd);

        ast.S.visit(this, o);

        emit(loopContinue + ":");
        int stackSize3 = frame.getCurStackSize();
        ast.E3.visit(this, o);
        emitMultiPOP(stackSize3, frame);

        emit(JVM.GOTO, loopStart);
        emit(loopEnd + ":");

        frame.conStack.pop();
        frame.brkStack.pop();

        return null;
    }

    public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
        return null;
    }

    public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
        return null;
    }

    public Object visitEmptyStmt(EmptyStmt ast, Object o) {
        return null;
    }

    // #region Expressions

    public Object visitAssignExpr(AssignExpr ast, Object o) {
        Frame frame = (Frame) o;

        if (ast.E1 instanceof ArrayExpr) {
            ArrayExpr ae = (ArrayExpr) ast.E1;
            ae.V.visit(this, o);
            ae.E.visit(this, o);
        }

        ast.E2.visit(this, o);

        if (ast.E1 instanceof ArrayExpr) {
            emit(JVM.DUP_X2);
        } else {
            emit(JVM.DUP);
        }
        frame.push();

        if (ast.E1 instanceof ArrayExpr) {
            emitArraySTORE(ast.E2, frame);
        } else {
            VarExpr varExpr = (VarExpr) ast.E1;
            SimpleVar v = (SimpleVar) varExpr.V;
            if (v.I.decl instanceof GlobalVarDecl) {
                GlobalVarDecl g = (GlobalVarDecl) v.I.decl;
                String type = VCtoJavaType(g.T);
                emitPUTSTATIC(type, v.I.spelling);
                frame.pop();
            } else if (ast.E1.type.isFloatType()) {
                emitFSTORE(v.I);
                frame.pop();
            } else {
                emitISTORE(v.I);
                frame.pop();
            }
        }
        return null;
    }

    public Object visitCallExpr(CallExpr ast, Object o) {
        Frame frame = (Frame) o;
        String fname = ast.I.spelling;

        switch (fname) {
            case "getInt":
                ast.AL.visit(this, o); // push args (if any) into the op stack
                emit("invokestatic VC/lang/System.getInt()I");
                frame.push();
                break;
            case "putInt":
                ast.AL.visit(this, o); // push args (if any) into the op stack
                emit("invokestatic VC/lang/System.putInt(I)V");
                frame.pop();
                break;
            case "putIntLn":
                ast.AL.visit(this, o); // push args (if any) into the op stack
                emit("invokestatic VC/lang/System/putIntLn(I)V");
                frame.pop();
                break;
            case "getFloat":
                ast.AL.visit(this, o); // push args (if any) into the op stack
                emit("invokestatic VC/lang/System/getFloat()F");
                frame.push();
                break;
            case "putFloat":
                ast.AL.visit(this, o); // push args (if any) into the op stack
                emit("invokestatic VC/lang/System/putFloat(F)V");
                frame.pop();
                break;
            case "putFloatLn":
                ast.AL.visit(this, o); // push args (if any) into the op stack
                emit("invokestatic VC/lang/System/putFloatLn(F)V");
                frame.pop();
                break;
            case "putBool":
                ast.AL.visit(this, o); // push args (if any) into the op stack
                emit("invokestatic VC/lang/System/putBool(Z)V");
                frame.pop();
                break;
            case "putBoolLn":
                ast.AL.visit(this, o); // push args (if any) into the op stack
                emit("invokestatic VC/lang/System/putBoolLn(Z)V");
                frame.pop();
                break;
            case "putString":
                ast.AL.visit(this, o);
                emit(JVM.INVOKESTATIC, "VC/lang/System/putString(Ljava/lang/String;)V");
                frame.pop();
                break;
            case "putStringLn":
                ast.AL.visit(this, o);
                emit(JVM.INVOKESTATIC, "VC/lang/System/putStringLn(Ljava/lang/String;)V");
                frame.pop();
                break;
            case "putLn":
                ast.AL.visit(this, o); // push args (if any) into the op stack
                emit("invokestatic VC/lang/System/putLn()V");
                break;
            default: // programmer-defined functions
                FuncDecl fAST = (FuncDecl) ast.I.decl;

                // all functions except main are assumed to be instance methods
                if (frame.isMain())
                    emit("aload_1"); // vc.funcname(...)
                else
                    emit("aload_0"); // this.funcname(...)
                frame.push();

                ast.AL.visit(this, o);

                String retType = VCtoJavaType(fAST.T);

                // The types of the parameters of the called function are not
                // directly available in the FuncDecl node but can be gathered
                // by traversing its field PL.

                StringBuffer argsTypes = new StringBuffer("");
                List fpl = fAST.PL;
                while (!fpl.isEmpty()) {
                    argsTypes.append(VCtoJavaType(((ParaList) fpl).P.T));
                    fpl = ((ParaList) fpl).PL;
                }

                emit("invokevirtual", classname + "/" + fname + "(" + argsTypes + ")" + retType);

                // find and delete all '[' in String argsTypes
                argsTypes = new StringBuffer(argsTypes.toString().replace("[", ""));
                frame.pop(argsTypes.length() + 1);

                if (!retType.equals("V"))
                    frame.push();
        }
        return null;
    }

    public Object visitBinaryExpr(BinaryExpr ast, Object o) {
        Frame frame = (Frame) o;
        ast.E1.visit(this, o);
        switch (ast.O.spelling) {
            case "i&&":
                String labelCalc = frame.getNewLabel();
                String labelEnd = frame.getNewLabel();

                emit(JVM.IFNE, labelCalc);
                frame.pop();
                emit(JVM.ICONST_0);
                emit(JVM.GOTO, labelEnd);

                emit(labelCalc + ":");
                ast.E2.visit(this, o);
                emit(labelEnd + ":");
                break;
            case "i*":
                ast.E2.visit(this, o);
                emit(JVM.IMUL);
                frame.pop(2);
                frame.push();
                break;
            case "i||":
                String labelCalc2 = frame.getNewLabel();
                String labelEnd2 = frame.getNewLabel();

                emit(JVM.IFEQ, labelCalc2);
                frame.pop();
                emit(JVM.ICONST_1);
                emit(JVM.GOTO, labelEnd2);

                emit(labelCalc2 + ":");
                ast.E2.visit(this, o);
                emit(labelEnd2 + ":");
                break;
            case "i+":
                ast.E2.visit(this, o);
                emit(JVM.IADD);
                frame.pop(2);
                frame.push();
                break;
            case "i-":
                ast.E2.visit(this, o);
                emit(JVM.ISUB);
                frame.pop(2);
                frame.push();
                break;
            case "i/":
                ast.E2.visit(this, o);
                emit(JVM.IDIV);
                frame.pop(2);
                frame.push();
                break;
            case "f+":
                ast.E2.visit(this, o);
                emit(JVM.FADD);
                frame.pop(2);
                frame.push();
                break;
            case "f-":
                ast.E2.visit(this, o);
                emit(JVM.FSUB);
                frame.pop(2);
                frame.push();
                break;
            case "f*":
                ast.E2.visit(this, o);
                emit(JVM.FMUL);
                frame.pop(2);
                frame.push();
                break;
            case "f/":
                ast.E2.visit(this, o);
                emit(JVM.FDIV);
                frame.pop(2);
                frame.push();
                break;
            case "i<":
            case "i<=":
            case "i>":
            case "i>=":
            case "i==":
            case "i!=":
                ast.E2.visit(this, o);
                emitICMP(ast.O.spelling, frame);
                break;
            case "f<":
            case "f<=":
            case "f>":
            case "f>=":
            case "f==":
            case "f!=":
                ast.E2.visit(this, o);
                emitFCMP(ast.O.spelling, frame);
                break;
            default:
                errorReporter.reportError("*** Error: Invalid binary operator", "", ast.position);
                break;
        }
        return null;
    }

    public Object visitUnaryExpr(UnaryExpr ast, Object o) {
        Frame frame = (Frame) o;
        ast.E.visit(this, o);
        switch (ast.O.spelling) {
            case "i2f":
                emit(JVM.I2F);
                break;
            case "i-":
                emit(JVM.INEG);
                break;
            case "f-":
                emit(JVM.FNEG);
                break;
            case "i!":
                String falseLabel = frame.getNewLabel();
                String exitLabel = frame.getNewLabel();
                emit(JVM.IFNE, falseLabel);
                emitICONST(1);
                emit("goto", exitLabel);
                emit(falseLabel + ":");
                emitICONST(0);
                emit(exitLabel + ":");
                break;
            case "f+":
            case "i+":
                break;
            default:
                errorReporter.reportError("*** Error: invalid unary operator", "", ast.position);
                break;
        }
        return null;
    }

    public Object visitVarExpr(VarExpr ast, Object o) {
        ast.V.visit(this, o);
        return null;
    }

    public Object visitArrayInitExpr(ArrayInitExpr ast, Object o) {
        Frame frame = (Frame) o;
        Decl decl = (Decl) ast.parent;
        ArrayType at = (ArrayType) decl.T;
        emitNEWARRAY(at, frame);
        frame.push();
        arrayIndex = 0;
        ast.IL.visit(this, o);
        return null;
    }

    public Object visitArrayExpr(ArrayExpr ast, Object o) {
        Frame frame = (Frame) o;
        ArrayType at = (ArrayType) ast.V.visit(this, o);
        String it = VCtoJavaType(at.T);
        ast.E.visit(this, o);
        String inst = "";
        switch (it) {
            case "I":
                inst = JVM.IALOAD;
                break;
            case "F":
                inst = JVM.FALOAD;
                break;
            case "Z":
                inst = JVM.BALOAD;
                break;
            default:
                errorReporter.reportError("ArrayExpr: invalid array type", "", ast.position);
        }
        if (!inst.equals("")) {
            emit(inst);
            frame.pop(2);
            frame.push();
        }
        return null;
    }

    public Object visitArrayExprList(ArrayExprList ast, Object o) {
        Frame frame = (Frame) o;

        emit(JVM.DUP);
        frame.push();

        emitICONST(arrayIndex);
        frame.push();

        ast.E.visit(this, o);

        emitArraySTORE(ast.E, frame);

        arrayIndex++;
        ast.EL.visit(this, o);
        return null;
    }

    public Object visitEmptyArrayExprList(EmptyArrayExprList ast, Object o) {
        return null;
    }

    public Object visitEmptyExpr(EmptyExpr ast, Object o) {
        return null;
    }

    public Object visitIntExpr(IntExpr ast, Object o) {
        ast.IL.visit(this, o);
        return null;
    }

    public Object visitFloatExpr(FloatExpr ast, Object o) {
        ast.FL.visit(this, o);
        return null;
    }

    public Object visitBooleanExpr(BooleanExpr ast, Object o) {
        ast.BL.visit(this, o);
        return null;
    }

    public Object visitStringExpr(StringExpr ast, Object o) {
        ast.SL.visit(this, o);
        return null;
    }
    // #endregion
    // Declarations

    public Object visitDeclList(DeclList ast, Object o) {
        ast.D.visit(this, o);
        ast.DL.visit(this, o);
        return null;
    }

    public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
        return null;
    }

    public Object visitFuncDecl(FuncDecl ast, Object o) {

        Frame frame;

        if (ast.I.spelling.equals("main")) {

            frame = new Frame(true);

            // Assume that main has one String parameter and reserve 0 for it
            frame.getNewIndex();

            emit(JVM.METHOD_START, "public static main([Ljava/lang/String;)V");
            // Assume implicitly that
            // classname vc$;
            // appears before all local variable declarations.
            // (1) Reserve 1 for this object reference.

            frame.getNewIndex();

        } else {

            frame = new Frame(false);

            // all other programmer-defined functions are treated as if
            // they were instance methods
            frame.getNewIndex(); // reserve 0 for "this"

            String retType = VCtoJavaType(ast.T);

            // The types of the parameters of the called function are not
            // directly available in the FuncDecl node but can be gathered
            // by traversing its field PL.

            StringBuffer argsTypes = new StringBuffer("");
            List fpl = ast.PL;
            while (!fpl.isEmpty()) {
                argsTypes.append(VCtoJavaType(((ParaList) fpl).P.T));
                fpl = ((ParaList) fpl).PL;
            }

            emit(JVM.METHOD_START, ast.I.spelling + "(" + argsTypes + ")" + retType);
        }

        ast.S.visit(this, frame);

        // JVM requires an explicit return in every method.
        // In VC, a function returning void may not contain a return, and
        // a function returning int or float is not guaranteed to contain
        // a return. Therefore, we add one at the end just to be sure.

        if (ast.T.equals(StdEnvironment.voidType)) {
            emit("");
            emit("; return may not be present in a VC function returning void");
            emit("; The following return inserted by the VC compiler");
            emit(JVM.RETURN);
        } else if (ast.I.spelling.equals("main")) {
            // In case VC's main does not have a return itself
            emit(JVM.RETURN);
        } else
            emit(JVM.NOP);

        emit("");
        emit("; set limits used by this method");
        emit(JVM.LIMIT, "locals", frame.getNewIndex());

        emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
        // changed by the marker
        // emit(JVM.LIMIT, "stack", 50);
        emit(".end method");

        return null;
    }

    public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
        // nothing to be done
        return null;
    }

    public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
        Frame frame = (Frame) o;
        ast.index = frame.getNewIndex();
        String T = VCtoJavaType(ast.T);

        emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + T + " from " + frame.scopeStart.peek()
                + " to " + frame.scopeEnd.peek());

        if (!ast.E.isEmptyExpr()) {
            ast.E.visit(this, o);

            if (ast.T.equals(StdEnvironment.floatType)) {
                emitFSTORE(ast.I);
                frame.pop();
            } else if (ast.T.equals(StdEnvironment.booleanType) || ast.T.equals(StdEnvironment.intType)) {
                emitISTORE(ast.I);
                frame.pop();
            } else {
                emitASTORE(ast.I);
                frame.pop();
            }
        } else if (ast.T.isArrayType()) {
            ArrayType at = (ArrayType) ast.T;
            emitNEWARRAY(at, frame);
            frame.push();
            emitASTORE(ast.I);
            frame.pop();
        }

        return null;
    }

    // Parameters

    public Object visitParaList(ParaList ast, Object o) {
        ast.P.visit(this, o);
        ast.PL.visit(this, o);
        return null;
    }

    public Object visitParaDecl(ParaDecl ast, Object o) {
        Frame frame = (Frame) o;
        ast.index = frame.getNewIndex();
        String type = VCtoJavaType(ast.T);

        emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + type + " from " + frame.scopeStart.peek()
                + " to " + frame.scopeEnd.peek());
        return null;
    }

    public Object visitEmptyParaList(EmptyParaList ast, Object o) {
        return null;
    }

    // Arguments

    public Object visitArgList(ArgList ast, Object o) {
        ast.A.visit(this, o);
        ast.AL.visit(this, o);
        return null;
    }

    public Object visitArg(Arg ast, Object o) {
        ast.E.visit(this, o);
        return null;
    }

    public Object visitEmptyArgList(EmptyArgList ast, Object o) {
        return null;
    }

    // Types

    public Object visitArrayType(ArrayType ast, Object o) {
        return null;
    }

    public Object visitStringType(StringType ast, Object o) {
        return null;
    }

    public Object visitIntType(IntType ast, Object o) {
        return null;
    }

    public Object visitFloatType(FloatType ast, Object o) {
        return null;
    }

    public Object visitBooleanType(BooleanType ast, Object o) {
        return null;
    }

    public Object visitVoidType(VoidType ast, Object o) {
        return null;
    }

    public Object visitErrorType(ErrorType ast, Object o) {
        return null;
    }

    // Literals, Identifiers and Operators

    public Object visitIdent(Ident ast, Object o) {
        return null;
    }

    public Object visitIntLiteral(IntLiteral ast, Object o) {
        Frame frame = (Frame) o;
        emitICONST(Integer.parseInt(ast.spelling));
        frame.push();
        return null;
    }

    public Object visitFloatLiteral(FloatLiteral ast, Object o) {
        Frame frame = (Frame) o;
        emitFCONST(Float.parseFloat(ast.spelling));
        frame.push();
        return null;
    }

    public Object visitBooleanLiteral(BooleanLiteral ast, Object o) {
        Frame frame = (Frame) o;
        emitBCONST(ast.spelling.equals("true"));
        frame.push();
        return null;
    }

    public Object visitStringLiteral(StringLiteral ast, Object o) {
        Frame frame = (Frame) o;
        emit(JVM.LDC, "\"" + ast.spelling + "\"");
        frame.push();
        return null;
    }

    public Object visitOperator(Operator ast, Object o) {
        return null;
    }

    // Variables

    public Object visitSimpleVar(SimpleVar ast, Object o) {
        Frame frame = (Frame) o;
        Decl binding = (Decl) ast.I.decl;
        String type = VCtoJavaType(binding.T);
        int index;
        if (binding instanceof GlobalVarDecl) {
            emitGETSTATIC(type, binding.I.spelling);
        } else {
            if (binding instanceof LocalVarDecl) {
                LocalVarDecl local = (LocalVarDecl) binding;
                index = local.index;
            } else {
                ParaDecl para = (ParaDecl) binding;
                index = para.index;
            }
            switch (type) {
                case "I":
                case "Z":
                    emitILOAD(index);
                    break;
                case "F":
                    emitFLOAD(index);
                    break;
                default:
                    emitALOAD(index);
                    break;
            }
        }
        frame.push();
        return binding.T;
    }

    // Auxiliary methods for byte code generation

    // The following method appends an instruction directly into the JVM
    // Code Store. It is called by all other overloaded emit methods.

    private void emit(String s) {
        JVM.append(new Instruction(s));
    }

    private void emit(String s1, String s2) {
        emit(s1 + " " + s2);
    }

    private void emit(String s1, int i) {
        emit(s1 + " " + i);
    }

    private void emit(String s1, float f) {
        emit(s1 + " " + f);
    }

    private void emit(String s1, String s2, int i) {
        emit(s1 + " " + s2 + " " + i);
    }

    private void emit(String s1, String s2, String s3) {
        emit(s1 + " " + s2 + " " + s3);
    }

    private void emitDefaultInit(GlobalVarDecl gVar, Frame frame) {
        if (gVar.T.equals(StdEnvironment.floatType))
            emit(JVM.FCONST_0);
        else if (gVar.T.equals(StdEnvironment.intType))
            emit(JVM.ICONST_0);
        else if (gVar.T.equals(StdEnvironment.booleanType))
            emitBCONST(false);
        else {
            ArrayType at = (ArrayType) gVar.T;
            emitNEWARRAY(at, frame);
        }
    }

    private void emitArraySTORE(Expr e, Frame frame) {
        String inst = "";
        if (e.type.equals(StdEnvironment.floatType))
            inst = JVM.FASTORE;
        else if (e.type.equals(StdEnvironment.intType))
            inst = JVM.IASTORE;
        else if (e.type.equals(StdEnvironment.booleanType))
            inst = JVM.BASTORE;
        emit(inst);
        frame.pop(3);
    }

    private void emitNEWARRAY(ArrayType at, Frame frame) {
        String it = VCtoJavaType(at.T);
        at.E.visit(this, frame);
        String instIT = "";
        switch (it) {
            case "I":
                instIT = "int";
                break;
            case "F":
                instIT = "float";
                break;
            case "Z":
                instIT = "boolean";
                break;
            default:
                errorReporter.reportError("*** Error: invalid array type", "", at.position);
        }
        if (!instIT.equals("")) {
            emit(JVM.NEWARRAY, instIT);
            frame.pop();
        }
    }

    private void emitMultiPOP(int oldStackSize, Frame frame) {
        int newStackSize = frame.getCurStackSize();
        for (int i = newStackSize; i > oldStackSize; i--)
            emit(JVM.POP);
        if (newStackSize > oldStackSize)
            frame.pop(newStackSize - oldStackSize);
    }

    private void emitICMP(String op, Frame frame) {
        String opcode;

        if (op.equals("i!="))
            opcode = JVM.IF_ICMPNE;
        else if (op.equals("i=="))
            opcode = JVM.IF_ICMPEQ;
        else if (op.equals("i<"))
            opcode = JVM.IF_ICMPLT;
        else if (op.equals("i<="))
            opcode = JVM.IF_ICMPLE;
        else if (op.equals("i>"))
            opcode = JVM.IF_ICMPGT;
        else // if (op.equals("i>="))
            opcode = JVM.IF_ICMPGE;

        String falseLabel = frame.getNewLabel();
        String nextLabel = frame.getNewLabel();

        emit(opcode, falseLabel);
        frame.pop(2);
        emit("iconst_0");
        emit("goto", nextLabel);
        emit(falseLabel + ":");
        emit(JVM.ICONST_1);
        frame.push();
        emit(nextLabel + ":");
    }

    private void emitFCMP(String op, Frame frame) {
        String opcode;

        if (op.equals("f!="))
            opcode = JVM.IFNE;
        else if (op.equals("f=="))
            opcode = JVM.IFEQ;
        else if (op.equals("f<"))
            opcode = JVM.IFLT;
        else if (op.equals("f<="))
            opcode = JVM.IFLE;
        else if (op.equals("f>"))
            opcode = JVM.IFGT;
        else // if (op.equals("f>="))
            opcode = JVM.IFGE;

        String falseLabel = frame.getNewLabel();
        String nextLabel = frame.getNewLabel();

        emit(JVM.FCMPG);
        frame.pop(2);
        emit(opcode, falseLabel);
        emit(JVM.ICONST_0);
        emit("goto", nextLabel);
        emit(falseLabel + ":");
        emit(JVM.ICONST_1);
        frame.push();
        emit(nextLabel + ":");

    }

    private void emitILOAD(int index) {
        if (index >= 0 && index <= 3)
            emit(JVM.ILOAD + "_" + index);
        else
            emit(JVM.ILOAD, index);
    }

    private void emitFLOAD(int index) {
        if (index >= 0 && index <= 3)
            emit(JVM.FLOAD + "_" + index);
        else
            emit(JVM.FLOAD, index);
    }

    private void emitALOAD(int index) {
        if (index >= 0 && index <= 3)
            emit(JVM.ALOAD + "_" + index);
        else
            emit(JVM.ALOAD, index);
    }

    private void emitGETSTATIC(String T, String I) {
        emit(JVM.GETSTATIC, classname + "/" + I, T);
    }

    private void emitISTORE(Ident ast) {
        int index;
        if (ast.decl instanceof ParaDecl)
            index = ((ParaDecl) ast.decl).index;
        else
            index = ((LocalVarDecl) ast.decl).index;

        if (index >= 0 && index <= 3)
            emit(JVM.ISTORE + "_" + index);
        else
            emit(JVM.ISTORE, index);
    }

    private void emitFSTORE(Ident ast) {
        int index;
        if (ast.decl instanceof ParaDecl)
            index = ((ParaDecl) ast.decl).index;
        else
            index = ((LocalVarDecl) ast.decl).index;
        if (index >= 0 && index <= 3)
            emit(JVM.FSTORE + "_" + index);
        else
            emit(JVM.FSTORE, index);
    }

    private void emitASTORE(Ident ast) {
        int index;
        if (ast.decl instanceof ParaDecl)
            index = ((ParaDecl) ast.decl).index;
        else
            index = ((LocalVarDecl) ast.decl).index;
        if (index >= 0 && index <= 3)
            emit(JVM.ASTORE + "_" + index);
        else
            emit(JVM.ASTORE, index);
    }

    private void emitPUTSTATIC(String T, String I) {
        emit(JVM.PUTSTATIC, classname + "/" + I, T);
    }

    private void emitICONST(int value) {
        if (value == -1)
            emit(JVM.ICONST_M1);
        else if (value >= 0 && value <= 5)
            emit(JVM.ICONST + "_" + value);
        else if (value >= -128 && value <= 127)
            emit(JVM.BIPUSH, value);
        else if (value >= -32768 && value <= 32767)
            emit(JVM.SIPUSH, value);
        else
            emit(JVM.LDC, value);
    }

    private void emitFCONST(float value) {
        if (value == 0.0)
            emit(JVM.FCONST_0);
        else if (value == 1.0)
            emit(JVM.FCONST_1);
        else if (value == 2.0)
            emit(JVM.FCONST_2);
        else
            emit(JVM.LDC, value);
    }

    private void emitBCONST(boolean value) {
        if (value)
            emit(JVM.ICONST_1);
        else
            emit(JVM.ICONST_0);
    }

    private String VCtoJavaType(Type t) {
        if (t.equals(StdEnvironment.booleanType))
            return "Z";
        else if (t.equals(StdEnvironment.intType))
            return "I";
        else if (t.equals(StdEnvironment.floatType))
            return "F";
        else if (t.equals(StdEnvironment.voidType))
            return "V";
        else {
            ArrayType at = (ArrayType) t;
            return "[" + VCtoJavaType(at.T);
        }
    }

}
