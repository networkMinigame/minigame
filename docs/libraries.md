# 라이브러리 사용법

기본 자바/Swing으로 짤 때와 비교해서 정리. 버전은 `build.gradle.kts`가 단일 출처.

예제는 Java 25 + 현재 의존성으로 실제 실행해서 확인함 (2026-10-25).

## 1. FlatLaf: 화면 테마

L&F(Look and Feel)는 Swing 컴포넌트 전체의 생김새를 정하는 설정. 기본 L&F는 오래된 자바 느낌이라 FlatLaf로 바꾼다.

**기존**

```java
UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // OS 기본 모양
```

**FlatLaf**

```java
FlatLightLaf.setup();   // 밝은 테마. 어두운 테마는 FlatDarkLaf.setup()
```

- 컴포넌트를 만들기 **전에** 한 번만 호출한다 (`main` 맨 앞)
- 테마 모음(`flatlaf-intellij-themes`)에서 고를 수도 있다: `FlatArcOrangeIJTheme.setup();`

## 2. FlatLaf style: CSS처럼 꾸미기

**기존**: 속성마다 setter를 부르고, 둥근 모서리는 `paintComponent`를 직접 오버라이드해야 함

```java
button.setBackground(new Color(0x4A90E2));
button.setForeground(Color.WHITE);
button.setFont(button.getFont().deriveFont(Font.BOLD, button.getFont().getSize2D() + 2));
// 둥근 모서리: JButton을 상속해서 paintComponent에서 fillRoundRect로 직접 그리기
```

**FlatLaf style**: 문자열 한 줄

```java
button.putClientProperty(FlatClientProperties.STYLE,
    "arc: 20; background: #4A90E2; foreground: #FFFFFF; font: bold +2");
```

- **미리 정의된 글자 크기**: `label.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");` (h00, h0, h1~h4, large, medium, small 등)
- **앱 전체 기본값**: `setup()` 전에 `UIManager.put("Button.arc", 20);` → 모든 버튼이 둥글어짐. 컴포넌트마다 style을 주는 것보다 이쪽을 먼저 쓴다

## 3. flatlaf-extras: 아이콘, 개발용 검사 도구

**SVG 아이콘**

```java
// 기존: PNG. 화면 배율(125%, 150%)에서 흐려짐
new ImageIcon(getClass().getResource("/icons/crown.png"));

// FlatSVGIcon: 벡터라 배율이 달라도 선명함. 경로는 src/main/resources 기준
new FlatSVGIcon("icons/crown.svg", 24, 24);
```

**FlatInspector**: 실행 중인 창에서 단축키를 누르고 마우스를 올리면 컴포넌트 종류·크기·여백이 보임. 레이아웃이 왜 틀어졌는지 찾을 때 씀

```java
FlatInspector.install("ctrl shift alt X");   // 개발 중에만
```

## 4. MigLayout: 화면 배치

로비, 결과 화면처럼 라벨·입력칸·버튼을 배치하는 화면에 쓴다. 게임 화면은 `paintComponent`로 직접 그리므로 해당 없음.

예: 닉네임/서버 주소 입력칸 2줄 + 가운데 접속 버튼

**기존 (GridBagLayout)**: 칸마다 좌표·정렬·늘어남을 지정

```java
JPanel panel = new JPanel(new GridBagLayout());
GridBagConstraints c = new GridBagConstraints();
c.insets = new Insets(5, 5, 5, 5);

c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.LINE_END;
panel.add(new JLabel("닉네임"), c);
c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
panel.add(nameField, c);

c.gridx = 0; c.gridy = 1; c.fill = GridBagConstraints.NONE; c.weightx = 0;
panel.add(new JLabel("서버 주소"), c);
c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
panel.add(addressField, c);

c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
c.anchor = GridBagConstraints.CENTER;
panel.add(joinButton, c);
```

**MigLayout**: 표 규칙을 문자열로 한 번 적고 순서대로 add

```java
JPanel panel = new JPanel(new MigLayout(
    "wrap 2, insets 20, gap 10",   // 2칸마다 줄바꿈, 바깥 여백 20, 간격 10
    "[right][grow, fill]"));        // 1열 오른쪽 정렬, 2열은 남는 폭을 채움
panel.add(new JLabel("닉네임"));
panel.add(nameField);
panel.add(new JLabel("서버 주소"));
panel.add(addressField);
panel.add(joinButton, "span 2, align center");   // 두 칸 합쳐서 가운데
```

## 5. Gson / Jackson: 메시지 직렬화

메시지 형식(JSON vs 문자열)은 아직 정하지 않음 ([architecture.md](architecture.md) 열린 질문). 아래는 고를 때 비교용. **둘 중 하나로 정하면 나머지는 의존성에서 뺀다.**

공통 주의: 한 줄 = 메시지 하나이므로 **pretty print(줄바꿈 넣는 출력)를 켜지 않는다.** 둘 다 기본값은 한 줄 출력.

**기존 (문자열 프로토콜)**

```java
String line = "MOVE|1|0";
String[] parts = line.split("\\|");
ClientMsg msg = switch (parts[0]) {
    case "MOVE" -> new Move(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    // 필드가 늘거나 순서가 바뀌면 보내는 쪽·받는 쪽 인덱스를 같이 고쳐야 함
    default -> throw new IllegalArgumentException("알 수 없는 메시지: " + parts[0]);
};
```

**Gson**: sealed interface를 자동으로 구분하지 못해서 `type` + `data` 봉투로 감싼다

```java
record Envelope(String type, JsonElement data) {}

Gson gson = new Gson();
String line = gson.toJson(new Envelope("MOVE", gson.toJsonTree(new Move(1, 0))));
// {"type":"MOVE","data":{"dx":1,"dy":0}}

Envelope env = gson.fromJson(line, Envelope.class);
ClientMsg msg = switch (env.type()) {
    case "MOVE" -> gson.fromJson(env.data(), Move.class);
    case "USE_ITEM" -> gson.fromJson(env.data(), UseItem.class);
    default -> throw new IllegalArgumentException("알 수 없는 메시지: " + env.type());
};
```

**Jackson**: 인터페이스에 어노테이션을 달면 `type` 필드로 알아서 구분

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Move.class, name = "MOVE"),
    @JsonSubTypes.Type(value = UseItem.class, name = "USE_ITEM")
})
sealed interface ClientMsg permits Move, UseItem {}

JsonMapper mapper = JsonMapper.builder().build();
String line = mapper.writeValueAsString(new UseItem("bomb", 2));
// {"type":"USE_ITEM","item":"bomb","targetId":2}

ClientMsg msg = mapper.readValue(line, ClientMsg.class);
switch (msg) {
    case Move m -> ...
    case UseItem u -> ...
}
```

- Jackson 3은 패키지가 `tools.jackson.*`. 검색해서 나오는 예제 대부분은 Jackson 2(`com.fasterxml.jackson.databind.*`)라 import가 다름. 어노테이션만 `com.fasterxml.jackson.annotation.*` 그대로
- Jackson 3은 예외가 unchecked(`JacksonException`)라 `throws`를 안 붙여도 됨

| | Gson | Jackson |
|---|---|---|
| 메시지 종류 구분 | 봉투 + switch를 직접 작성 | 어노테이션만 달면 자동 |
| 새 메시지 추가 시 | switch에 case 추가 | `@JsonSubTypes`에 한 줄 추가 |
| 크기·설정 | 작고 단순 | 기능이 많음 |

## 6. JUnit + AssertJ: 테스트

작성 규칙은 [개발 컨벤션](../CONTRIBUTING.md#테스트)가 단일 출처. 여기는 검증 문법만 비교.

```java
// JUnit 기본: 기대값/실제값 순서를 헷갈리기 쉬움
assertEquals(2, player.lives());

// AssertJ: 실제값부터 읽히는 순서
assertThat(player.lives()).isEqualTo(2);
assertThat(players).hasSize(4).extracting(Player::id).containsExactly(0, 1, 2, 3);
```
