class_flag=-cp "target/classes/:src/main/resources:target/lib/json-20200518.jar"
compile=javac $(class_flag) -d target/classes/
run=java $(class_flag)


# *** Main API ***
run: compile_driver compile_paxos compile_eserver compile_eclient
	@$(run) main.driver.PaxosDriver $(shell pwd)/config.json


# *** Testing ***
test_class_flag=-cp "target/classes/:target/test-classes:src/test/resources:target/testlib/junit-4.13.jar:target/testlib/hamcrest-core-1.3.jar:target/testlib/mockito-core-3.5.13.jar:target/testlib/byte-buddy-1.10.15.jar:target/testlib/byte-buddy-agent-1.10.15.jar:target/testlib/objenesis-3.1.jar"
compile_test=javac $(test_class_flag) -d target/test-classes/
run_test=java $(test_class_flag) org.junit.runner.JUnitCore

test_eserver: compile_test_eserver
	@$(run_test) test.eserver.EmailServerTest
	@$(run_test) test.eserver.EmailConnectionTest

test_eclient: compile_test_eclient
	@$(run_test) test.eclient.EmailClientTest
	@$(run_test) test.eclient.EmailIntegrationTest

test_paxos: compile_test_paxos
	@$(run_test) test.paxos.MemberRunnableTest
	@$(run_test) test.paxos.TimerTest
	@$(run_test) test.paxos.ProposerRunnableTest
	@$(run_test) test.paxos.AcceptorRunnableTest
	@$(run_test) test.paxos.MessageCodesTest
	@$(run_test) test.paxos.MemberTest
	@$(run_test) test.paxos.DelayedMessageExecutorTest

test_slow_paxos: compile_test_paxos
	@$(run_test) test.paxos.ProposerRunnableSlowTest


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

compile_driver: src/main/driver/*.java
	@$(compile) $?

zip: clean
	zip -r project.zip Makefile README.md designs src target config.json .git

clean:
	rm -r --force target/classes/*
	rm -r --force target/test-classes/*
