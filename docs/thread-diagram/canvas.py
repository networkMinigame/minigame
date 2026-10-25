"""ASCII 다이어그램을 좌표로 찍는 캔버스. 손으로 공백을 맞추지 않으려고 쓴다.

라벨은 ASCII만 쓴다. 한글은 고정폭 글꼴에서 폭이 2칸이라 정렬이 깨진다.
"""


class Canvas:
    def __init__(self, width, height):
        self.g = [[" "] * width for _ in range(height)]

    def put(self, x, y, text):
        for i, ch in enumerate(text):
            self.g[y][x + i] = ch

    def box(self, x1, y1, x2, y2, labels=(), lx=None):
        """테두리 + 라벨. lx가 없으면 가운데 정렬."""
        for x in range(x1, x2 + 1):
            self.g[y1][x] = self.g[y2][x] = "-"
        for y in range(y1, y2 + 1):
            self.g[y][x1] = self.g[y][x2] = "|"
        for x, y in ((x1, y1), (x2, y1), (x1, y2), (x2, y2)):
            self.g[y][x] = "+"
        for y in range(y1 + 1, y2):
            for x in range(x1 + 1, x2):
                self.g[y][x] = " "
        for i, s in enumerate(labels):
            start = lx if lx is not None else x1 + 1 + (x2 - x1 - 1 - len(s)) // 2
            self.put(start, y1 + 1 + i, s)

    def h(self, y, x1, x2, ch="-"):
        for x in range(x1, x2 + 1):
            self.g[y][x] = ch

    def v(self, x, y1, y2, ch="|"):
        for y in range(y1, y2 + 1):
            self.g[y][x] = ch

    def render(self):
        return "\n".join("".join(r).rstrip() for r in self.g)
