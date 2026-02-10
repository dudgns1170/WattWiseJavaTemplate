SHELL := /bin/bash
 
.PHONY: install install-dev up down test format lint

install:
	@echo "Gradle를 사용해 프로덕션 빌드를 수행합니다..."
	./gradlew clean build

install-dev:
	@echo "개발용 빌드를 수행합니다..."
	./gradlew build

up:
	@echo "도커 컴포즈를 시작합니다..."
	docker-compose up --build

down:
	@echo "도커 컴포즈를 중지합니다..."
	docker-compose down

test:
	./gradlew test

format:
	@echo "정적 분석 및 기본 코드 검사를 실행합니다..."
	./gradlew check

lint:
	@echo "정적 분석 및 기본 코드 검사를 실행합니다..."
	./gradlew check
