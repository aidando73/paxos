class_flag=-cp "target/classes/:src/main/resources:target/lib/json-20200518.jar"
compile=javac $(class_flag) -d target/classes/
run=java $(class_flag)


# *** Main API ***











# *** Testing ***
test_class_flag=-cp "target/classes/:target/test-classes:src/test/resources:target/testlib/junit-4.13.jar:target/testlib/hamcrest-core-1.3.jar:target/testlib/mockito-all-1.10.19.jar"
compile_test=javac $(test_class_flag) -d target/test-classes/
run_test=java $(test_class_flag) org.junit.runner.JUnitCore

test_eserver: compile_test_eserver
	@$(run_test) test.eserver.EmailServerTest
	@$(run_test) test.eserver.EmailConnectionTest

test_eclient: compile_test_eclient
	@$(run_test) test.eclient.EmailClientTest
	@$(run_test) test.eclient.EmailIntegrationTest

test_paxos: compile_test_paxos
	@$(run_test) test.paxos.MessageCodesTest
	@$(run_test) test.paxos.MemberTest


# *** Compilation ***
compile_test_eserver: compile_eserver src/test/eserver/*.java
	@$(compile_test) src/test/eserver/*.java

compile_test_eclient: compile_eclient src/test/eclient/*.java
	@$(compile_test) src/test/eclient/*.java

compile_test_paxos: compile_paxos src/test/paxos/*.java
	@$(compile_test) src/test/paxos/*.java


compile_paxos: src/main/paxos/*.java
	@$(compile) $?

compile_eserver: src/main/eserver/*.java
	@$(compile) $?

compile_eclient: src/main/eclient/*.java
	@$(compile) $?
