.class public test27
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
.method f()V
L0:
.var 0 is this Ltest27; from L0 to L1
	iconst_1
	invokestatic VC/lang/System/putIntLn(I)V
L1:
	
	; return may not be present in a VC function returning void
	; The following return inserted by the VC compiler
	return
	
	; set limits used by this method
.limit locals 1
.limit stack 1
.end method
.method g()I
L0:
.var 0 is this Ltest27; from L0 to L1
	iconst_2
	invokestatic VC/lang/System/putIntLn(I)V
	iconst_1
	ireturn
L1:
	nop
	
	; set limits used by this method
.limit locals 1
.limit stack 1
.end method
.method public static main([Ljava/lang/String;)V
L0:
.var 0 is argv [Ljava/lang/String; from L0 to L1
.var 1 is vc$ Ltest27; from L0 to L1
	new test27
	dup
	invokenonvirtual test27/<init>()V
	astore_1
.var 2 is i I from L0 to L1
	iconst_1
	istore_2
	iload_2
	iconst_1
	if_icmpeq L4
	iconst_0
	goto L5
L4:
	iconst_1
L5:
	ifeq L2
	aload_1
	invokevirtual test27/f()V
	goto L3
L2:
	aload_1
	invokevirtual test27/g()I
	pop
L3:
	return
L1:
	return
	
	; set limits used by this method
.limit locals 3
.limit stack 2
.end method