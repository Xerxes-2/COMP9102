.class public test51
.super java/lang/Object
	
.field static i I
	
	; standard class static initializer 
.method static <clinit>()V
	
	iconst_1
	putstatic test51/i I
	
	; set limits used by this method
.limit locals 0
.limit stack 1
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
.method f()Z
L0:
.var 0 is this Ltest51; from L0 to L1
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
.var 1 is vc$ Ltest51; from L0 to L1
	new test51
	dup
	invokenonvirtual test51/<init>()V
	astore_1
.var 2 is j I from L0 to L1
	getstatic test51/i I
	istore_2
.var 3 is b1 Z from L0 to L1
	getstatic test51/i I
	bipush 10
	if_icmple L2
	iconst_0
	goto L3
L2:
	iconst_1
L3:
	istore_3
.var 4 is b2 Z from L0 to L1
	aload_1
	invokevirtual test51/f()Z
	istore 4
.var 5 is b3 [Z from L0 to L1
	iconst_2
	newarray boolean
	dup
	iconst_0
	iload_3
	bastore
	dup
	iconst_1
	iload 4
	bastore
	astore 5
.var 6 is f F from L0 to L1
	iload_2
	bipush 100
	iadd
	i2f
	fstore 6
	getstatic test51/i I
	i2f
	dup
	fstore 6
	dup
	fstore 6
	dup
	fstore 6
	pop
	getstatic test51/i I
	invokestatic VC/lang/System/putIntLn(I)V
	iload_2
	invokestatic VC/lang/System/putIntLn(I)V
	iload_3
	ifne L4
	iconst_0
	goto L5
L4:
	iload 4
L5:
	ifne L6
	iconst_0
	goto L7
L6:
	aload 5
	iconst_0
	baload
L7:
	ifeq L8
	iconst_1
	goto L9
L8:
	aload 5
	iconst_1
	baload
L9:
	invokestatic VC/lang/System/putBoolLn(Z)V
	fload 6
	invokestatic VC/lang/System/putFloatLn(F)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 7
.limit stack 4
.end method
