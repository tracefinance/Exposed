.PHONY: build

build:
	./gradlew clean
	./gradlew build -x test

test:
	./gradlew clean
	./gradlew build
	./gradlew test

publish:
	./gradlew clean
	./gradlew build -x test
	./gradlew artifactoryPublish
