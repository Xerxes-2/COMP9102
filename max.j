.class public max
.super java/lang/Object
	
	
	; standard class static initializer 
.method static <clinit>()V
	
	
	; set limits used by this method
.limit locals 0
.limit stack 50
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
.var 1 is vc$ Lmax; from L0 to L1
	new max
	dup
	invokenonvirtual max/<init>()V
	astore_1
.var 2 is i I from L0 to L1
	invokestatic VC/lang/System.getInt()I
	istore_2
.var 3 is j I from L0 to L1
	invokestatic VC/lang/System.getInt()I
	istore_3
	iload_2
	iload_3
	if_icmpge L2
	iconst_0
	goto L3
L2:
	iconst_1
L3:
	ifeq L4
	iload_2
	invokestatic VC/lang/System/putIntLn(I)V
	goto L5
L4:
	iload_3
	invokestatic VC/lang/System/putIntLn(I)V
L5:
	return
L1:
	return
	
	; set limits used by this method
.limit locals 4
.limit stack 50
.end method
