"""전체도를 SVG로 그리는 도구. 각 버전의 overview.py가 이걸로 overview.svg를 만든다.

색 규칙
- 박스: 파랑 = 스레드, 노랑 = 큐(또는 큐를 가진 스레드), 초록 = GameLoop·상태 소유자, 회색 = 프로세스
- 선: 회색 = TCP, 파랑 = 받는 길(Reader -> receiveQueue), 초록 = 큐 -> GameLoop, 노랑 = 보내는 길(GameLoop -> sendQueue -> Writer)
- v2부터: 청록 = main 스레드, 빨강 점선 = 종료 경로, 연회색 = 생성·시작
"""

from pathlib import Path

FONT = "Pretendard, 'Malgun Gothic', 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif"

BOX = {
    "process": ("#f6f8fa", "#8c959f"),
    "thread": ("#ffffff", "#0969da"),
    "queue": ("#fff8c5", "#bf8700"),
    "loop": ("#dafbe1", "#1a7f37"),
    "title": ("#ffffff", "#57606a"),
    "main": ("#e6f6f3", "#1a7f6e"),
    "edt": ("#eef4ff", "#0969da"),
    "socket": ("#d8dee4", "#57606a"),
    "close": ("#ffebe9", "#cf222e"),
    "note": ("#ffffff", "#d0d7de"),
}

LINE = {
    "tcp": "#57606a",
    "recv": "#0969da",
    "loop": "#1a7f37",
    "send": "#bf8700",
    "make": "#8c959f",
    "close": "#cf222e",
}

DASHED = {"close"}


class Diagram:
    def __init__(self, width, height):
        self.w, self.h = width, height
        self.out = [f'<rect width="{width}" height="{height}" rx="12" fill="#ffffff"/>']
        markers = "".join(
            f'<marker id="arrow-{name}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" '
            f'orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="{color}"/></marker>'
            for name, color in LINE.items())
        self.out.append(f"<defs>{markers}</defs>")

    def rect(self, x, y, w, h, kind, r=8):
        fill, stroke = BOX[kind]
        self.out.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" '
                        f'fill="{fill}" stroke="{stroke}" stroke-width="1.6"/>')

    def panel(self, x, y, w, h, kind, title, note=None, r=12):
        """안에 다른 박스를 담는 큰 박스. 제목은 왼쪽 위, note는 오른쪽 위에 작게."""
        self.rect(x, y, w, h, kind, r=r)
        self.text(x + 20, y + 26, title, size=17, weight=700, anchor="start")
        if note:
            self.text(x + w - 18, y + 26, note, size=12, weight=400, color="#57606a", anchor="end")

    def lines(self, x, y, rows, size=13, gap=21, color="#1f2328"):
        """왼쪽 정렬 여러 줄."""
        for i, row in enumerate(rows):
            self.text(x, y + i * gap, row, size=size, weight=400, color=color, anchor="start")

    def text(self, x, y, s, size=15, weight=500, color="#1f2328", anchor="middle"):
        self.out.append(f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" text-anchor="{anchor}" '
                        f'dominant-baseline="middle" fill="{color}">{s}</text>')

    def node(self, x, y, w, h, kind, lines):
        """박스 + 가운데 라벨. 첫 줄은 굵게, 나머지는 작은 회색 글씨."""
        self.rect(x, y, w, h, kind)
        top = y + h / 2 - (len(lines) - 1) * 10
        for i, line in enumerate(lines):
            if i == 0:
                self.text(x + w / 2, top, line, weight=600)
            else:
                self.text(x + w / 2, top + i * 20, line, size=13, weight=400, color="#57606a")

    def line(self, x1, y1, x2, y2, color="#8c959f"):
        self.out.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{color}" stroke-width="1.6"/>')

    def arrow(self, points, kind, label=None, label_at=None):
        """points를 잇는 꺾은선 화살표. label은 선 위에 흰 바탕으로 얹는다."""
        d = "M " + " L ".join(f"{x} {y}" for x, y in points)
        dash = ' stroke-dasharray="6 4"' if kind in DASHED else ""
        self.out.append(f'<path d="{d}" fill="none" stroke="{LINE[kind]}" stroke-width="1.8"{dash} '
                        f'marker-end="url(#arrow-{kind})"/>')
        if label:
            lx, ly = label_at
            # 한글은 영문보다 넓어서 글자 종류별로 폭을 어림한다. 짧은 라벨은 예전처럼 36px.
            width = max(36, sum(11 if ord(c) > 0x2000 else 6.5 for c in label) + 12)
            self.out.append(f'<rect x="{lx - width / 2:g}" y="{ly - 10}" width="{width:g}" height="18" rx="4" fill="#ffffff"/>')
            self.text(lx, ly, label, size=11, weight=600, color=LINE[kind] if kind != "tcp" else "#57606a")

    def save(self, path):
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {self.w} {self.h}" '
               f'width="{self.w}" height="{self.h}" font-family="{FONT}">\n'
               + "\n".join(self.out) + "\n</svg>\n")
        Path(path).write_text(svg, encoding="utf-8", newline="\n")
