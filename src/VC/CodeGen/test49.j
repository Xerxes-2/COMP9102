.class public test49
.super java/lang/Object
	
	
	; standard class static initializer 
.method static <clinit>()V
	
	
	; set limits used by this method
.limit locals 0
.limit stack 0
	return
.end method
	
	; standard constructor initializer 
.method public <init>()V
.limit stack 1
.limit locals 1
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
.method public static main([Ljava/lang/String;)V
L0:
.var 0 is argv [Ljava/lang/String; from L0 to L1
.var 1 is vc$ Ltest49; from L0 to L1
	new test49
	dup
	invokenonvirtual test49/<init>()V
	astore_1
.var 2 is i I from L0 to L1
	iconst_1
	istore_2
	iload_2
	invokestatic VC/lang/System/putIntLn(I)V
L2:
.var 3 is i I from L2 to L3
	iconst_2
	istore_3
	iload_3
	invokestatic VC/lang/System/putIntLn(I)V
L3:
	iload_2
	invokestatic VC/lang/System/putIntLn(I)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 4
.limit stack 2
.end method
