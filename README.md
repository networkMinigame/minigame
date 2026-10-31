# minigame

2~4인이 함께하는 실시간 미니게임 파티 게임

서버 PC 한 대에 친구들이 접속해, 미니게임을 연달아 하며 왕관을 모읍니다. 정해진 라운드가 끝나면 왕관이 가장 많은 사람이 승리합니다.

## 기술 스택

- Java 25 / Gradle
- Swing + FlatLaf (클라이언트 화면, 테마·스타일)
- MigLayout (화면 배치)
- TCP 소켓 (`\n`으로 끝나는 한 줄 = 메시지 하나)
- Gson / Jackson (메시지 직렬화, 둘 중 하나로 정할 예정)

## 시작하기

JDK를 따로 맞출 필요는 없습니다. JDK 25가 없으면 첫 빌드 때 Gradle이 자동으로 받습니다.

```bash
./gradlew run           # 실행 (Windows: gradlew run)
./gradlew test          # 테스트
./gradlew build         # 빌드 (build/libs/*.jar)
```

## 문서

- [설계 기준안](docs/architecture.md): 스레드 구조, 라운드 흐름, 메시지 처리 방식
- [스레드 구조도](docs/thread-diagram/): 서버·클라이언트 스레드와 큐 그림, 메시지가 지나가는 순서 (버전별)
- [미니게임 목록](docs/minigames.md): 메인/후보 게임, 형식(개인전·2:2·1:나머지), 동시성 포인트
- [라이브러리 사용법](docs/libraries.md): FlatLaf, MigLayout, Gson/Jackson 등을 기본 Swing·자바 코드와 비교
- [개발 컨벤션](CONVENTION.md): 작업 흐름, Java 주석, 테스트 및 Git 규칙
