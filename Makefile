SCANNER_VC := $(wildcard src/VC/Scanner/*.vc)
RECOGNISER_VC := $(wildcard src/VC/Recogniser/*.vc)
PARSER_VC := $(wildcard src/VC/Parser/*.vc)
CHECKER_VC := $(wildcard src/VC/Checker/*.vc)
EMMITER_VC := $(wildcard src/VC/CodeGen/*.vc)
SHELL=/usr/bin/bash

# sources
JAVA_SOURCES := $(shell find src -name "*.java")

compile: clean
	javac -d target ${JAVA_SOURCES}


clean:
	find target -iname "*.class" -delete
	find src -iname "*.out" -delete
	find src -iname "*.j" -delete

# run
scanner: compile
	for file in $(patsubst %.vc,%,$(SCANNER_VC)); do \
		echo "Checking $$file"; \
		touch $$file.out; \
		java -cp target VC.vc $$file.vc > $$file.out; \
		diff $$file.sol $$file.out; \
	done

recogniser: compile
	for i in ${RECOGNISER_VC}; do \
		echo "============Checking $$i============"; \
		b=$${i%.vc};  \
		java -cp target VC.vc $$i > $$b.out; \
		diff $$b.out $$b.sol; \
	done

parser: compile
	t=0; \
	p=0; \
	for i in ${PARSER_VC}; do \
		echo "============Checking $$i============"; \
		t=$$((t+2)); \
		b=$${i%.vc};  \
		java -cp target VC.vc $$i; \
		java -cp target VC.vc -u $${i}uu $${i}u; \
		diff $${i}u $${i}uu; \
		diff $${i}u $$b.sol; \
		if [ $$? -eq 0 ]; then \
			p=$$((p+1)); \
		fi; \
		if [ $$? -eq 0 ]; then \
			p=$$((p+1)); \
		fi; \
	done; \
	echo "Total: $$t"; \
	echo "Pass: $$p"; \

checker: compile
	for i in ${CHECKER_VC}; do \
		echo "============Checking $$i============"; \
		b=$${i%.vc};  \
		java -cp target VC.vc $$i > $$b.out; \
		diff $$b.out $$b.sol; \
	done

emmiter: compile
	p=src/VC/CodeGen/; \
	cd $$p; \
	for i in test[0-9]*.vc; do \
		echo "============Checking $$i============"; \
		b=$${i%.vc};  \
		n=$${b#$$p}; \
		echo "=== test program === " > $$n.out; \
		cat $$n.vc >> $$n.out; \
		printf "\n" >> $$n.out; \
		java -cp ~/cs3131/target VC.vc $$n.vc >> $$n.out; \
		jasmin -d ~/cs3131/target $$n.j; \
		printf "\n=== The output of the test program === \n" >> $$n.out; \
		java -cp ~/cs3131/target $$n >> $$n.out; \
		diff $$n.out $$n.sol; \
	done

.PHONY: compile
