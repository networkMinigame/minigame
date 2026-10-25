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
- [미니게임 목록](docs/minigames.md): 메인/후보 게임, 형식(개인전·2:2·1:나머지), 동시성 포인트
- [라이브러리 사용법](docs/libraries.md): FlatLaf, MigLayout, Gson/Jackson 등을 기본 Swing·자바 코드와 비교

## 테스트

테스트는 **given / when / then** 으로 작성합니다.

- 주석 `// given`, `// when`, `// then` 으로 준비 · 실행 · 검증을 나눕니다.
- 테스트 하나에는 동작 하나만 둡니다. 여러 경우는 테스트를 나누거나 `@ParameterizedTest` 로 씁니다.
- 게임 로직(판정, 점수, 탈락)은 네트워크 없이 단위 테스트합니다.
- 동시성 테스트는 `Thread.sleep` 으로 순서를 기대하지 않고 `CountDownLatch` 등으로 순서를 맞춥니다.
- `@DisplayName` 은 구현 용어 대신 "어떤 상황이면 → 어떤 결과" 로 씁니다.

```java
@Test
@DisplayName("무적 시간 중에 다시 맞으면 목숨이 줄지 않는다")
void hitDuringInvincible() {
	// given
	Player player = new Player(0, 3);

	// when
	player.hit();
	player.hit();

	// then
	assertThat(player.lives()).isEqualTo(2);
}
```

## 커밋 컨벤션

- `feat:` 기능 추가
- `fix:` 버그 수정
- `refactor:` 기능 변경 없는 코드 구조 개선
- `docs:` 문서 수정
- `chore:` 설정/빌드 관련
- `test:` 테스트 작성, 수정
