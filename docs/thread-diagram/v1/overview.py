"""v1 전체도. 실행: python docs/thread-diagram/v1/overview.py"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))  # canvas.py 위치
from canvas import Canvas

c = Canvas(92, 34)

# 제목
c.box(4, 0, 17, 2, ["Client A"])
c.box(52, 0, 77, 2, ["Server 100.x.x.x:5000"])

# 클라이언트 A
c.box(0, 4, 21, 23)
c.box(3, 6, 18, 8, ["Input (main)"])
c.v(10, 9, 9); c.put(10, 10, "v")
c.box(3, 11, 18, 13, ["sendQueue"])
c.v(10, 14, 14); c.put(10, 15, "v")
c.box(3, 16, 18, 18, ["Send"])
c.box(3, 19, 18, 21, ["Recv -> print"])

# 클라이언트 B
c.box(2, 25, 19, 30)
c.put(4, 26, "Recv")
c.put(4, 27, "Client B")
c.put(4, 28, "(same as A)")
c.put(4, 29, "Send")

# 서버
c.box(34, 4, 90, 33)
c.box(70, 6, 88, 8, ["Accept"])
c.put(66, 9, "new client ->")
c.put(66, 10, "Recv + Send")

c.box(56, 12, 73, 14, ["inputQueue"])
c.v(64, 15, 16); c.put(64, 17, "v")
c.box(56, 18, 73, 22, ["GameLoop", "1 thread", "20~30Hz"])

# 서버 테두리(x=34)에 걸친 세션 스레드.
# 받는 길(Recv -> inputQueue)과 보내는 길(GameLoop -> Send)이 교차하지 않게 A는 Recv/Send, B는 Send/Recv 순서
for y1, label in ((16, "Recv A"), (19, "Send A [Q]"), (25, "Send B [Q]"), (28, "Recv B")):
    c.box(28, y1, 47, y1 + 2, [label], lx=36)
    c.g[y1][34] = "+"; c.g[y1 + 1][34] = "|"; c.g[y1 + 2][34] = "+"

# TCP
c.h(17, 19, 26); c.put(22, 17, "TCP"); c.put(27, 17, ">")
c.put(19, 20, "<"); c.h(20, 20, 27); c.put(22, 20, "TCP")
c.put(20, 26, "<"); c.h(26, 21, 27); c.put(22, 26, "TCP")
c.h(29, 20, 26); c.put(22, 29, "TCP"); c.put(27, 29, ">")

# Recv A -> inputQueue
c.h(17, 48, 49); c.g[17][50] = "+"; c.v(50, 14, 16); c.g[13][50] = "+"; c.h(13, 51, 54); c.put(55, 13, ">")
# Recv B -> 아래로 돌아 오른쪽에서 inputQueue
c.h(29, 48, 85); c.g[29][86] = "+"; c.v(86, 14, 28); c.g[13][86] = "+"; c.h(13, 75, 85); c.put(74, 13, "<")
# GameLoop -> Send A
c.put(48, 20, "<"); c.h(20, 49, 55)
# GameLoop -> Send B
c.h(21, 52, 55); c.g[21][51] = "+"; c.v(51, 22, 25); c.g[26][51] = "+"; c.h(26, 49, 50); c.put(48, 26, "<")

print(c.render())
