.class public test47
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
.method g(I)F
L0:
.var 0 is this Ltest47; from L0 to L1
.var 1 is i I from L0 to L1
	iload_1
	i2f
	freturn
L1:
	nop
	
	; set limits used by this method
.limit locals 2
.limit stack 1
.end method
.method f(IF)F
L0:
.var 0 is this Ltest47; from L0 to L1
.var 1 is i I from L0 to L1
.var 2 is f F from L0 to L1
	iload_1
	i2f
	aload_0
	iload_1
	invokevirtual test47/g(I)F
	fadd
	freturn
L1:
	nop
	
	; set limits used by this method
.limit locals 3
.limit stack 3
.end method
.method public static main([Ljava/lang/String;)V
L0:
.var 0 is argv [Ljava/lang/String; from L0 to L1
.var 1 is vc$ Ltest47; from L0 to L1
	new test47
	dup
	invokenonvirtual test47/<init>()V
	astore_1
	aload_1
	iconst_1
	iconst_2
	i2f
	invokevirtual test47/f(IF)F
	invokestatic VC/lang/System/putFloatLn(F)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 2
.limit stack 3
.end method
