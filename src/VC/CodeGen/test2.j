.class public test2
.super java/lang/Object
	
.field static f [F
	
	; standard class static initializer 
.method static <clinit>()V
	
	iconst_2
	newarray float
	putstatic test2/f [F
	
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
.method public static main([Ljava/lang/String;)V
L0:
.var 0 is argv [Ljava/lang/String; from L0 to L1
.var 1 is vc$ Ltest2; from L0 to L1
	new test2
	dup
	invokenonvirtual test2/<init>()V
	astore_1
	getstatic test2/f [F
	iconst_0
	faload
	invokestatic VC/lang/System/putFloatLn(F)V
	getstatic test2/f [F
	iconst_1
	faload
	invokestatic VC/lang/System/putFloatLn(F)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 2
.limit stack 2
.end method
