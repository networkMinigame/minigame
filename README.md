# minigame

2~4인이 함께하는 실시간 미니게임 파티 게임

서버 PC 한 대에 친구들이 접속해, 미니게임을 연달아 하며 왕관을 모읍니다. 정해진 라운드가 끝나면 왕관이 가장 많은 사람이 승리합니다.

## 기술 스택

- Java 25 / Gradle
- Swing + FlatLaf (클라이언트 화면, 테마·스타일)
- MigLayout (화면 배치)
- TCP 소켓 (`\n`으로 끝나는 한 줄 = 메시지 하나)
- Jackson (JSON 직렬화)

## 시작하기

JDK를 따로 맞출 필요는 없습니다. JDK 25가 없으면 첫 빌드 때 Gradle이 자동으로 받습니다.

```bash
./gradlew run           # 실행 (Windows: gradlew run)
./gradlew test          # 테스트
./gradlew build         # 빌드 (build/libs/*.jar)
```

같은 빌드로 여러 클라이언트를 실행할 때는 첫 실행 인자로 서로 다른 이름을 지정하세요.

```bash
./gradlew run --args="모카"
./gradlew run --args="구름"
```

IntelliJ에서는 ClientMain 실행 설정의 Program arguments에 이름을 입력합니다.
이름을 생략하거나 공백으로 주면 ClientMain의 기본 이름을 사용합니다.
동일 이름으로 접속하면 서버가 입장을 거절합니다.

## 배포 서버

클라우드에 띄운 서버의 공인 IP는 `129.203.160.45`입니다. 포트는 `ServerMain`의 `PORT`를 따릅니다.

클라이언트는 기본으로 `localhost`에 접속합니다. 배포 서버에 붙으려면 `ClientMain`의 `HOST`를 위 IP로 바꿔 실행하세요.

## 문서

- [설계 기준안](docs/architecture.md): 스레드 구조, 라운드 흐름, 메시지 처리 방식
- [스레드 구조도](docs/thread-diagram/): 서버·클라이언트 스레드와 큐 그림, 메시지가 지나가는 순서 (버전별)
- [미니게임 목록](docs/minigames.md): 메인/후보 게임, 형식(개인전·2:2·1:나머지), 동시성 포인트
- [라이브러리 사용법](docs/libraries.md): FlatLaf, MigLayout, Jackson 사용법
- [개발 컨벤션](CONTRIBUTING.md): Java 주석, 테스트 및 Git 규칙
