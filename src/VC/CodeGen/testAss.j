.class public testAss
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
.var 1 is vc$ LtestAss; from L0 to L1
	new testAss
	dup
	invokenonvirtual testAss/<init>()V
	astore_1
.var 2 is a [I from L0 to L1
	iconst_2
	newarray int
	astore_2
.var 3 is b I from L0 to L1
	aload_2
	iconst_1
	aload_2
	iconst_0
	iconst_1
	dup_x2
	iastore
	dup_x2
	iastore
	istore_3
	iload_3
	invokestatic VC/lang/System.putInt(I)V
	invokestatic VC/lang/System/putLn()V
	aload_2
	iconst_0
	iaload
	invokestatic VC/lang/System.putInt(I)V
	invokestatic VC/lang/System/putLn()V
	aload_2
	iconst_1
	iaload
	invokestatic VC/lang/System.putInt(I)V
	invokestatic VC/lang/System/putLn()V
L1:
	return
	
	; set limits used by this method
.limit locals 4
.limit stack 6
.end method
