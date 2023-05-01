.class public test3
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
.var 1 is vc$ Ltest3; from L0 to L1
	new test3
	dup
	invokenonvirtual test3/<init>()V
	astore_1
.var 2 is i I from L0 to L1
	bipush 10
	istore_2
.var 3 is f F from L0 to L1
	bipush 100
	dup
	istore_2
	pop
	iload_2
	iconst_1
	iadd
	dup
	istore_2
	pop
	iload_2
	invokestatic VC/lang/System/putIntLn(I)V
	fconst_1
	dup
	fstore_3
	pop
	iload_2
	i2f
	fload_3
	fadd
	dup
	fstore_3
	pop
	fload_3
	invokestatic VC/lang/System/putFloatLn(F)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 4
.limit stack 2
.end method
