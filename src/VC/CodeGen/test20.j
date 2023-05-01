.class public test20
.super java/lang/Object
	
.field static b [Z
	
	; standard class static initializer 
.method static <clinit>()V
	
	iconst_2
	newarray boolean
	putstatic test20/b [Z
	
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
.method f()I
L0:
.var 0 is this Ltest20; from L0 to L1
	iconst_1
	ireturn
L1:
	nop
	
	; set limits used by this method
.limit locals 1
.limit stack 1
.end method
.method g()I
L0:
.var 0 is this Ltest20; from L0 to L1
	iconst_2
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
.var 1 is vc$ Ltest20; from L0 to L1
	new test20
	dup
	invokenonvirtual test20/<init>()V
	astore_1
.var 2 is a [I from L0 to L1
	iconst_2
	newarray int
	dup
	iconst_0
	aload_1
	invokevirtual test20/f()I
	iastore
	dup
	iconst_1
	aload_1
	invokevirtual test20/g()I
	iastore
	astore_2
.var 3 is f [F from L0 to L1
	iconst_2
	newarray float
	dup
	iconst_0
	aload_2
	iconst_0
	iaload
	i2f
	fastore
	dup
	iconst_1
	fconst_2
	fastore
	astore_3
	getstatic test20/b [Z
	iconst_0
	aload_2
	iconst_0
	iaload
	i2f
	aload_3
	iconst_0
	faload
	fcmpg
	ifle L2
	iconst_0
	goto L3
L2:
	iconst_1
L3:
	ifne L4
	iconst_0
	goto L5
L4:
	getstatic test20/b [Z
	iconst_1
	baload
L5:
	ifeq L6
	iconst_1
	goto L7
L6:
	aload_3
	iconst_1
	faload
	aload_2
	iconst_1
	iaload
	i2f
	fadd
	bipush 100
	i2f
	fcmpg
	ifge L8
	iconst_0
	goto L9
L8:
	iconst_1
L9:
L7:
	dup_x2
	bastore
	pop
	getstatic test20/b [Z
	iconst_0
	baload
	invokestatic VC/lang/System/putBoolLn(Z)V
	getstatic test20/b [Z
	iconst_1
	baload
	invokestatic VC/lang/System/putBoolLn(Z)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 4
.limit stack 5
.end method
