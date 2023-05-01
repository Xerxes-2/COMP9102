.class public test29
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
.method f([IF)V
L0:
.var 0 is this Ltest29; from L0 to L1
.var 1 is a [I from L0 to L1
.var 2 is f F from L0 to L1
	aload_1
	iconst_0
	iaload
	invokestatic VC/lang/System/putIntLn(I)V
	aload_1
	iconst_0
	bipush 100
	dup_x2
	iastore
	pop
	fload_2
	invokestatic VC/lang/System/putFloatLn(F)V
L1:
	
	; return may not be present in a VC function returning void
	; The following return inserted by the VC compiler
	return
	
	; set limits used by this method
.limit locals 3
.limit stack 4
.end method
.method public static main([Ljava/lang/String;)V
L0:
.var 0 is argv [Ljava/lang/String; from L0 to L1
.var 1 is vc$ Ltest29; from L0 to L1
	new test29
	dup
	invokenonvirtual test29/<init>()V
	astore_1
.var 2 is a [I from L0 to L1
	iconst_1
	newarray int
	dup
	iconst_0
	iconst_1
	iastore
	astore_2
	aload_1
	aload_2
	iconst_2
	i2f
	invokevirtual test29/f([IF)V
	aload_2
	iconst_0
	iaload
	invokestatic VC/lang/System/putIntLn(I)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 3
.limit stack 4
.end method
