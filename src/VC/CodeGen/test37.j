.class public test37
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
.var 1 is vc$ Ltest37; from L0 to L1
	new test37
	dup
	invokenonvirtual test37/<init>()V
	astore_1
.var 2 is i I from L0 to L1
.var 3 is j I from L0 to L1
	iconst_1
	istore_3
.var 4 is k I from L0 to L1
	iload_3
	dup
	istore_2
	iconst_1
	if_icmpeq L4
	iconst_0
	goto L5
L4:
	iconst_1
L5:
	ifeq L2
	iload_3
	invokestatic VC/lang/System/putIntLn(I)V
	goto L3
L2:
L3:
	return
L1:
	return
	
	; set limits used by this method
.limit locals 5
.limit stack 2
.end method