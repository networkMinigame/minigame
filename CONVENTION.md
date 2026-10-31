# 개발 컨벤션

## 작업 흐름

1. 작업 내용을 이슈로 먼저 등록한다.
2. 최신 `main`에서 작업 브랜치를 만들고 해당 브랜치에서 수정·커밋한다.
3. 작업 브랜치를 푸시한 뒤 `main`을 대상으로 PR을 만든다. PR 본문에 `Closes #이슈번호`로 이슈를 연결한다.
4. 변경 내용을 검증하고 리뷰를 확인한 뒤 squash merge한다. 작업 브랜치의 커밋들을 하나로 합쳐 `main`에 반영한다.

## Java 주석

Javadoc은 `/** ... */`로 작성하는 문서 주석이다. 클래스나 메서드 선언 바로 위에 두면 IDE에서 설명을 확인할 수 있다.

- 함수나 복잡한 비즈니스 로직에는 목적과 중요한 조건을 설명한다. 이름만으로 의미가 분명한 단순 조회 메서드에 같은 내용을 반복하지 않는다.
- 첫 문장에는 무엇을 하는지 적는다. 이어서 호출 전 필요한 상태, 호출 뒤 바뀌는 상태, 중복 호출 시 동작처럼 사용자가 알아야 할 조건을 적는다.
- 내부 구현에서 특정 방식을 택한 이유는 해당 코드 옆에 `//`로 적는다. 반복문이나 대입문을 한 줄씩 해설하지 않는다.

Javadoc의 태그는 설명할 내용이 있을 때 사용한다.

| 태그 | 적을 내용 |
| --- | --- |
| `@param 이름` | 인자의 의미, 허용 범위, 단위 |
| `@return` | 반환값의 의미와 실패하거나 결과가 없을 때의 값. `void` 메서드에는 쓰지 않는다. |
| `@throws 예외명` | 해당 예외가 발생하는 조건 |

`LobbyRoster.join(int characterIndex)`에 붙이는 예시다. 입장 성공 시 조작 대상이 바뀌는 점과 중복·정원 초과 처리를 설명한다.

```java
/**
 * 캐릭터를 로비에 입장시키고 새 입장자를 조작 대상으로 선택한다.
 * 이미 입장한 캐릭터이거나 정원이 찼으면 참가자와 조작 대상을 바꾸지 않는다.
 *
 * @param characterIndex 0 이상, 선택 가능한 캐릭터 수 미만의 인덱스
 * @return 입장 성공은 JOINED, 중복 입장은 ALREADY_PRESENT, 정원 초과는 FULL
 * @throws IllegalArgumentException 캐릭터 인덱스가 허용 범위를 벗어난 경우
 */
```

## 테스트

테스트는 **given / when / then**으로 나눈다. 각각 준비, 실행, 검증 단계이며 `// given`, `// when`, `// then` 주석으로 구분한다.

| 단계 | 작성할 내용 |
| --- | --- |
| given | 검사할 동작을 실행하기 전의 상태를 만든다. 객체 생성, 입력값 설정, 선행 동작이 여기에 들어간다. |
| when | 이번 테스트에서 확인할 동작을 실행한다. 반환값이 있으면 변수에 저장한다. |
| then | 기대한 반환값이나 외부에서 확인할 수 있는 상태를 검증한다. |

중복 입장을 검사한다면 첫 입장은 준비이므로 given에, 두 번째 입장은 검사할 동작이므로 when에 둔다. 아래 예시는 JUnit의 `@Test`로 테스트를 선언하고, AssertJ의 `assertThat`으로 결과를 검증한다.

```java
@Test
@DisplayName("이미 입장한 캐릭터가 다시 입장하면 참가자가 늘지 않는다")
void rejectsDuplicateCharacters() {
	// given
	LobbyRoster roster = new LobbyRoster(5);
	roster.join(2);

	// when
	LobbyRoster.JoinResult result = roster.join(2);

	// then
	assertThat(result).isEqualTo(LobbyRoster.JoinResult.ALREADY_PRESENT);
	assertThat(roster.size()).isEqualTo(1);
}
```

- 테스트 하나에는 동작 하나를 둔다. 같은 동작의 반환값과 상태를 함께 검증해도 된다.
- 입력값만 달라지는 같은 검증은 `@ParameterizedTest`로 반복한다. 준비 상태나 기대 동작이 다르면 테스트를 나눈다.
- 게임 로직의 판정, 점수, 탈락은 네트워크 연결 없이 검사한다.
- 동시성 테스트는 `Thread.sleep`으로 실행 순서를 기대하지 않는다. `CountDownLatch`처럼 다른 스레드의 특정 단계 도달을 기다리는 도구로 순서를 맞춘다.
- `@DisplayName`에는 테스트 실행 결과에 표시할 이름을 적는다. 구현 용어 대신 어떤 상황에서 어떤 결과가 나오는지 쓴다.

## 커밋 컨벤션

- `git push --force`는 금지한다.
- `git push --force-with-lease`는 작업 브랜치에서만 허용한다. `main`·`master` 같은 기본 브랜치에서는 금지한다.
- `feat:` 기능 추가
- `fix:` 버그 수정
- `refactor:` 기능 변경 없는 코드 구조 개선
- `docs:` 문서 수정
- `chore:` 설정/빌드 관련
- `test:` 테스트 작성, 수정
