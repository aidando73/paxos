class_flag=-cp "target/classes/:src/main/resources:target/lib/json-20200518.jar"
compile=javac $(class_flag) -d target/classes/
run=java $(class_flag)


# *** Main API ***











# *** Testing ***
test_class_flag=-cp "target/classes/:target/test-classes:src/test/resources:target/junit-4.13.jar:target/testlib/hamcrest-core-1.3.jar:target/testlib/mockito-all-1.10.19.jar"
compile_test=javac $(test_class_flag) -d target/test-classes/
run_test=java $(test_class_flag) org.junit.runner.JUnitCore

test_eserver: compile_test_eserver



# *** Compilation ***
compile_test_eserver: compile_eserver src/test/eserver/*.java
	@$(compile_test) $?

compile_eserver: src/main/eserver/*.java
	@$(compile) $?
